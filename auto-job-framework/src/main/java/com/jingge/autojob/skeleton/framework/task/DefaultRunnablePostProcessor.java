package com.jingge.autojob.skeleton.framework.task;

import com.jingge.autojob.logging.domain.AutoJobLog;
import com.jingge.autojob.logging.model.AutoJobLogContainer;
import com.jingge.autojob.logging.model.producer.AutoJobLogHelper;
import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.config.AutoJobConfig;
import com.jingge.autojob.skeleton.framework.pool.AutoJobPoolExecutor;
import com.jingge.autojob.skeleton.framework.pool.Executable;
import com.jingge.autojob.skeleton.framework.pool.RunnablePostProcessor;
import com.jingge.autojob.skeleton.lifecycle.ITaskEventHandler;
import com.jingge.autojob.skeleton.lifecycle.TaskEventFactory;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskAfterRunEvent;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskBeforeRunEvent;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskRunErrorEvent;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskRunSuccessEvent;
import com.jingge.autojob.skeleton.lifecycle.manager.TaskEventManager;
import com.jingge.autojob.skeleton.model.task.TaskExecutable;
import com.jingge.autojob.util.convert.DefaultValueUtil;
import com.jingge.autojob.util.servlet.InetUtil;
import com.jingge.autojob.util.thread.ScheduleTaskUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 任务执行后置处理器的默认实现，原则上无需自己实现，如果确实需要在任务开始、完成等完成一些操作，请优先使用任务事件处理器{@link ITaskEventHandler}
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/03 16:43
 */
@Slf4j
public class DefaultRunnablePostProcessor implements RunnablePostProcessor {

    @Override
    public void beforeRun(final Executable executable, AutoJobPoolExecutor executor, Object... params) {
        AutoJobConfig config = AutoJobApplication
                .getInstance()
                .getConfigHolder()
                .getAutoJobConfig();
        if (executable instanceof TaskExecutable) {
            TaskExecutable taskExecutable = (TaskExecutable) executable;
            AutoJobTask autoJobTask = taskExecutable.getAutoJobTask();
            if (autoJobTask != null) {
                /*=================状态刷新=================>*/
                autoJobTask
                        .getTrigger()
                        .start();
                autoJobTask
                        .getTrigger()
                        .setIsRunning(true);
                autoJobTask.setLogHelper(new AutoJobLogHelper(null, autoJobTask));
                autoJobTask
                        .getTrigger()
                        .setLastTriggeringTime(autoJobTask
                                .getTrigger()
                                .getTriggeringTime());
                if (autoJobTask.getRunResult() == null) {
                    autoJobTask.setRunResult(new AutoJobRunResult());
                } else {
                    autoJobTask
                            .getRunResult()
                            .reset();
                }
                autoJobTask.updateRunningStatus(AutoJobRunningStatus.RUNNING);
                if (autoJobTask.getType() == AutoJobTask.TaskType.DB_TASK) {
                    ScheduleTaskUtil.oneTimeTask(() -> {
                        AutoJobMapperHolder.TRIGGER_ENTITY_MAPPER.updateOperatingStatus(true, autoJobTask.getId());
                        return null;
                    }, 0, TimeUnit.SECONDS);

                }
                /*=======================Finished======================<*/

                /*=================绑定到任务上下文=================>*/
                AutoJobContext
                        .getCurrentScheduleID()
                        .set(autoJobTask
                                .getTrigger()
                                .getSchedulingRecordID());
                AutoJobContext.registerRunningTask(autoJobTask);
                AutoJobContext
                        .getConcurrentThreadTask()
                        .set(autoJobTask);
                /*=======================Finished======================<*/

                /*=================设置运行堆栈=================>*/
                if (config.getEnableStackTrace()) {
                    AutoJobRunningStackContainer
                            .getInstance()
                            .addNew(autoJobTask, config.getStackDepth());
                    autoJobTask.setStack(AutoJobRunningStackContainer
                            .getInstance()
                            .get(autoJobTask.id));
                    AutoJobRunningStackContainer
                            .getInstance()
                            .addEntry(autoJobTask.id, new RunningStackEntry().recordStart(autoJobTask));
                    autoJobTask.stack
                            .currentStackEntry()
                            .recordStart(autoJobTask);
                }

                AutoJobRunningContextHolder.contextHolder.set(new AutoJobRunningContext(autoJobTask));
                /*=======================Finished======================<*/

                //AutoJobLogContainer
                //        .getInstance()
                //        .getMessageQueueContext(AutoJobLog.class)
                //        .registerMessageQueue(autoJobTask.getId() + "");
                TaskEventManager
                        .getInstance()
                        .publishTaskEventSync(TaskEventFactory.newBeforeRunEvent(autoJobTask), TaskBeforeRunEvent.class, true);

                //输出启动日志
                AutoJobLogHelper logHelper = DefaultValueUtil.defaultValue(autoJobTask.logHelper, AutoJobLogHelper.getInstance());
                if (autoJobTask
                        .getTrigger()
                        .getIsRetrying() == null || !autoJobTask
                        .getTrigger()
                        .getIsRetrying()) {
                    if (!autoJobTask.isEnableSharding()) {
                        logHelper.info("Auto-Job-Start=========================>任务：{}即将在机器{}开始执行", autoJobTask.getId(), InetUtil.getTCPAddress());
                    } else {
                        logHelper.info("Auto-Job-Start=========================>任务：{}的分片{}即将在机器{}开始执行", autoJobTask.getId(), autoJobTask.shardingId, InetUtil.getTCPAddress());
                    }
                } else {
                    if (!autoJobTask.getIsShardingTask()) {
                        logHelper.info("Auto-Job-Start=========================>任务：{}即将在机器{}进行重试", autoJobTask.getId(), InetUtil.getTCPAddress());
                    } else {
                        logHelper.info("Auto-Job-Start=========================>任务分片：{}即将在机器{}进行重试", autoJobTask.shardingId, InetUtil.getTCPAddress());
                    }
                }
            }
        }
    }

    @Override
    public void afterRun(final Executable executable, AutoJobPoolExecutor executor, Object result) {
        AutoJobConfig config = AutoJobApplication
                .getInstance()
                .getConfigHolder()
                .getAutoJobConfig();
        if (executable instanceof TaskExecutable) {
            TaskExecutable taskExecutable = (TaskExecutable) executable;
            AutoJobTask autoJobTask = taskExecutable.getAutoJobTask();
            if (autoJobTask != null) {
                AutoJobContext.removeRunningTask(autoJobTask);
                //AutoJobContext.outSchedulingProgress(autoJobTask.getId());
                autoJobTask
                        .getTrigger()
                        .finished();
                autoJobTask
                        .getTrigger()
                        .setIsLastSuccess(true);
                autoJobTask.updateRunningStatus(AutoJobRunningStatus.SCHEDULING);
                autoJobTask
                        .getTrigger()
                        .setIsRetrying(false);
                autoJobTask.setIsAlreadyBroadcastSharding(false);
                autoJobTask
                        .getRunResult()
                        .success(result);
                autoJobTask
                        .getTrigger()
                        .update();
                if (config.getEnableStackTrace()) {
                    autoJobTask.stack
                            .currentStackEntry()
                            .recordResult(autoJobTask);
                }
                /*=================更新状态=================>*/
                if (autoJobTask.getType() == AutoJobTask.TaskType.DB_TASK) {
                    ScheduleTaskUtil.oneTimeTask(() -> {
                        AutoJobMapperHolder.TRIGGER_ENTITY_MAPPER.updateOperatingStatus(false, autoJobTask.getId());
                        return null;
                    }, 0, TimeUnit.SECONDS);
                }
                /*=======================Finished======================<*/
                DefaultValueUtil
                        .defaultValue(autoJobTask.logHelper, AutoJobLogHelper.getInstance())
                        .setSlf4jProxy(null)
                        .info("Auto-Job-End=========================>任务：{}在机器{}执行完成", autoJobTask.getId(), InetUtil.getTCPAddress());
                TaskEventManager
                        .getInstance()
                        .publishTaskEventSync(TaskEventFactory.newAfterRunEvent(autoJobTask), TaskAfterRunEvent.class, true);
                TaskEventManager
                        .getInstance()
                        .publishTaskEventSync(TaskEventFactory.newSuccessEvent(autoJobTask), TaskRunSuccessEvent.class, true);
                autoJobTask
                        .getTrigger()
                        .setIsRunning(false);
                autoJobTask.setLogHelper(null);
                AutoJobRunningContextHolder.contextHolder.remove();
            }
        }
    }

    @Override
    public void runError(final Executable executable, AutoJobPoolExecutor executor, Throwable throwable, Object result) {
        AutoJobConfig config = AutoJobApplication
                .getInstance()
                .getConfigHolder()
                .getAutoJobConfig();
        if (executable instanceof TaskExecutable) {
            TaskExecutable taskExecutable = (TaskExecutable) executable;
            AutoJobTask autoJobTask = taskExecutable.getAutoJobTask();
            if (autoJobTask != null) {
                AutoJobContext.removeRunningTask(autoJobTask);
                //AutoJobContext.outSchedulingProgress(autoJobTask.getId());
                autoJobTask
                        .getTrigger()
                        .finished();
                autoJobTask
                        .getTrigger()
                        .setIsLastSuccess(false);
                autoJobTask.updateRunningStatus(AutoJobRunningStatus.SCHEDULING);
                autoJobTask
                        .getTrigger()
                        .setIsRetrying(false);
                autoJobTask
                        .getRunResult()
                        .error(throwable, result);
                autoJobTask.setIsAlreadyBroadcastSharding(false);
                if (config.getEnableStackTrace()) {
                    autoJobTask.stack
                            .currentStackEntry()
                            .recordResult(autoJobTask);
                }
                /*=================更新状态=================>*/
                if (autoJobTask.getType() == AutoJobTask.TaskType.DB_TASK) {
                    ScheduleTaskUtil.oneTimeTask(() -> {
                        AutoJobMapperHolder.TRIGGER_ENTITY_MAPPER.updateOperatingStatus(false, autoJobTask.getId());
                        return null;
                    }, 0, TimeUnit.SECONDS);
                }
                /*=======================Finished======================<*/
                DefaultValueUtil
                        .defaultValue(autoJobTask.logHelper, AutoJobLogHelper.getInstance())
                        .setSlf4jProxy(null)
                        .error("Auto-Job-Error=========================>任务：{}在机器{}执行异常：{}", autoJobTask.getId(), InetUtil.getTCPAddress(), throwable.getCause() == null ? throwable.toString() : throwable
                                .getCause()
                                .toString());
                TaskEventManager
                        .getInstance()
                        .publishTaskEventSync(TaskEventFactory.newAfterRunEvent(autoJobTask), TaskAfterRunEvent.class, true);
                TaskEventManager
                        .getInstance()
                        .publishTaskEventSync(TaskEventFactory.newRunErrorEvent(autoJobTask, throwable), TaskRunErrorEvent.class, true);
                autoJobTask
                        .getTrigger()
                        .setIsRunning(false);
                autoJobTask.setLogHelper(null);
                AutoJobRunningContextHolder.contextHolder.remove();
            }
        }
    }
}
