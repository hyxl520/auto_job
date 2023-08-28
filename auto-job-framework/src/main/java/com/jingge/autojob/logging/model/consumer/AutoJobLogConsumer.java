package com.jingge.autojob.logging.model.consumer;

import com.jingge.autojob.logging.domain.AutoJobLog;
import com.jingge.autojob.logging.domain.AutoJobRunLog;
import com.jingge.autojob.logging.model.AutoJobLogContainer;
import com.jingge.autojob.logging.model.factory.AutoJobRunLogFactory;
import com.jingge.autojob.logging.model.handler.AutoJobLogHandler;
import com.jingge.autojob.logging.model.handler.AutoJobLogLoop;
import com.jingge.autojob.skeleton.framework.config.AutoJobLogConfig;
import com.jingge.autojob.skeleton.framework.config.TimeConstant;
import com.jingge.autojob.skeleton.framework.mq.MessageEntry;
import com.jingge.autojob.skeleton.framework.mq.MessagePublishedListener;
import com.jingge.autojob.skeleton.framework.mq.MessageQueue;
import com.jingge.autojob.skeleton.framework.mq.MessageQueueContext;
import com.jingge.autojob.skeleton.framework.task.AutoJobContext;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lifecycle.ITaskEventHandler;
import com.jingge.autojob.skeleton.lifecycle.event.TaskEvent;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskAfterRunEvent;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskBeforeRunEvent;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskFinishedEvent;
import com.jingge.autojob.skeleton.model.task.functional.FunctionTask;
import com.jingge.autojob.util.json.JsonUtil;
import com.jingge.autojob.util.thread.ScheduleTaskUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 任务日志消费者
 *
 * @Author Huang Yongxiang
 * @Date 2022/10/21 14:43
 */
@Slf4j
public class AutoJobLogConsumer implements ITaskEventHandler<TaskEvent> {
    private final Map<Long, AutoJobLogHandler> logHandlerMap = new ConcurrentHashMap<>();
    private final ILogSaveStrategyDelegate<AutoJobLog> logSaveStrategyDelegate;
    private final ILogSaveStrategyDelegate<AutoJobRunLog> runLogSaveStrategyDelegate;
    private final MessageQueueContext<AutoJobLog> logMessageQueueContext;
    private final ScheduleTaskUtil saveLogScheduler = ScheduleTaskUtil.build(true, "saveLogScheduler");
    private static final String LISTENER_PREFIX = "LOG_HANDLE_LISTENER_";
    private final AutoJobLogConfig logConfig;

    public AutoJobLogConsumer(ILogSaveStrategyDelegate<AutoJobLog> logSaveStrategyDelegate, ILogSaveStrategyDelegate<AutoJobRunLog> runLogSaveStrategyDelegate, AutoJobLogConfig logConfig) {
        this.logSaveStrategyDelegate = logSaveStrategyDelegate;
        this.runLogSaveStrategyDelegate = runLogSaveStrategyDelegate;
        logMessageQueueContext = AutoJobLogContainer
                .getInstance()
                .getMessageQueueContext(AutoJobLog.class);
        AutoJobLogLoop
                .getInstance()
                .bound(logHandlerMap);
        this.logConfig = logConfig;
    }

    @Override
    public void doHandle(TaskEvent event) {

        if (event instanceof TaskBeforeRunEvent) {
            AutoJobLogHandler handler = logHandlerMap.get(event
                    .getTask()
                    .getTrigger()
                    .getSchedulingRecordID());
            if (handler == null) {
                handler = new AutoJobLogHandler(event.getTask(), logSaveStrategyDelegate, runLogSaveStrategyDelegate)
                        .setMaxBufferLength(logConfig.getSaveWhenBufferReachSize())
                        .setSaveCycle(logConfig.getSaveWhenOverTime(), TimeUnit.SECONDS);
                logHandlerMap.put(event
                        .getTask()
                        .getTrigger()
                        .getSchedulingRecordID(), handler);
            } else {
                handler.refresh(event.getTask());
            }
            AutoJobLogHandler finalHandler = handler;
            saveLogScheduler.EOneTimeTask(() -> {
                finalHandler.startScheduling();
                return null;
            }, 0, TimeUnit.MILLISECONDS);
            logMessageQueueContext.registerMessageQueue(event
                    .getTask()
                    .getTrigger()
                    .getSchedulingRecordID() + "");
            logMessageQueueContext.addMessagePublishedListener(event
                    .getTask()
                    .getTrigger()
                    .getSchedulingRecordID() + "", new HandleMessageListener(handler, event.getTask()));

        }

        if (event
                .getTask()
                .getTrigger()
                .getSchedulingRecordID() != null && logHandlerMap.containsKey(event
                .getTask()
                .getTrigger()
                .getSchedulingRecordID())) {
            AutoJobLogHandler handler = logHandlerMap.get(event
                    .getTask()
                    .getTrigger()
                    .getSchedulingRecordID());
            handler.addRunLog(AutoJobRunLogFactory.getAutoJobRunLog(event));
        }

        //任务运行完更新状态，清空日志队列
        if (event instanceof TaskAfterRunEvent) {
            AutoJobLogHandler handler = logHandlerMap.get(event
                    .getTask()
                    .getTrigger()
                    .getSchedulingRecordID());
            AutoJobTask task = event.getTask();
            logMessageQueueContext.takeAllMessageNoBlock(event
                    .getTask()
                    .getTrigger()
                    .getSchedulingRecordID() + "", true);

            String result = JsonUtil.pojoToJsonString(task
                    .getRunResult()
                    .getResult());
            saveLogScheduler.EOneTimeTask(() -> {
                handler.finishScheduling(task
                        .getRunResult()
                        .isRunSuccess(), result, ((TaskAfterRunEvent) event).getEndTime() - task
                        .getTrigger()
                        .getStartRunTime());
                return null;
            }, 0, TimeUnit.MILLISECONDS);
            if (task
                    .getTrigger()
                    .getTriggeringTime() - System.currentTimeMillis() > TimeConstant.A_MINUTE * 30) {
                logHandlerMap
                        .remove(task
                                .getTrigger()
                                .getSchedulingRecordID())
                        .stop();
            }

            logMessageQueueContext.removeMessagePublishedListener(event
                    .getTask()
                    .getTrigger()
                    .getSchedulingRecordID() + "", LISTENER_PREFIX + task.getId());
            logMessageQueueContext.unsubscribe(event
                    .getTask()
                    .getTrigger()
                    .getSchedulingRecordID() + "", 0, TimeUnit.SECONDS);
        }

        //任务完成，卸载日志处理器
        if (event instanceof TaskFinishedEvent) {
            AutoJobTask finished = event.getTask();
            if (logHandlerMap.containsKey(finished
                    .getTrigger()
                    .getSchedulingRecordID())) {
                logHandlerMap
                        .remove(finished
                                .getTrigger()
                                .getSchedulingRecordID())
                        .stop();
            }
            if (finished instanceof FunctionTask) {
                ((FunctionTask) finished).allFinished();
            }
        }

    }

    private static class HandleMessageListener implements MessagePublishedListener<AutoJobLog> {
        AutoJobLogHandler handler;
        AutoJobTask task;

        public HandleMessageListener(AutoJobLogHandler handler, AutoJobTask task) {
            this.handler = handler;
            this.task = task;
        }


        @Override
        public void onMessagePublished(AutoJobLog message, MessageQueue<MessageEntry<AutoJobLog>> queue) {
            handler.addLog(message);
            queue.clear();
        }

        @Override
        public String listenerName() {
            return LISTENER_PREFIX + task
                    .getTrigger()
                    .getSchedulingRecordID();
        }
    }

    @Override
    public int getHandlerLevel() {
        return Integer.MIN_VALUE;
    }
}
