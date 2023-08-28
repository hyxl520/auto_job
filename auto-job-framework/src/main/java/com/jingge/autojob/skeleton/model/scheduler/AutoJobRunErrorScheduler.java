package com.jingge.autojob.skeleton.model.scheduler;

import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.config.AutoJobConfigHolder;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lifecycle.ITaskEventHandler;
import com.jingge.autojob.skeleton.lifecycle.TaskEventFactory;
import com.jingge.autojob.skeleton.lifecycle.TaskEventHandlerDelegate;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskErrorEvent;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskFinishedEvent;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskRunErrorEvent;
import com.jingge.autojob.skeleton.lifecycle.manager.TaskEventManager;
import com.jingge.autojob.skeleton.model.executor.AutoJobTaskExecutorPool;
import com.jingge.autojob.skeleton.model.handler.AutoJobRetryHandler;
import com.jingge.autojob.skeleton.model.register.IAutoJobRegister;
import com.jingge.autojob.skeleton.model.task.functional.FunctionTask;
import com.jingge.autojob.util.convert.DateUtils;
import com.jingge.autojob.util.thread.ScheduleTaskUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 运行异常调度器
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/19 15:16
 */
@Slf4j
public class AutoJobRunErrorScheduler extends AbstractScheduler implements ITaskEventHandler<TaskRunErrorEvent> {
    public AutoJobRunErrorScheduler(AutoJobTaskExecutorPool executorPool, IAutoJobRegister register, AutoJobConfigHolder configHolder) {
        super(executorPool, register, configHolder);
    }

    @Override
    public void doHandle(TaskRunErrorEvent event) {
        AutoJobTask task = event.getTask();
        if (task.getType() == AutoJobTask.TaskType.DB_TASK) {
            unlock(task.getId());
        }
        try {
            if (AutoJobRetryHandler
                    .getInstance()
                    .retry(task)) {
                log.info("任务{}重试成功，将在{}进行重试", task.isSharding() ? "分片" + task.getShardingId() : task.getId(), DateUtils.formatDateTime(task
                        .getRetryConfig()
                        .getNextRetryTime()));

            } else {
                log.error("任务{}经过重试后依然执行异常，任务执行失败", task.getId());
                task.setIsFinished(true);
                task
                        .getRunResult()
                        .finish();
                //AutoJobRetryHandler
                //        .getInstance()
                //        .remove(task.getId());
                TaskEventManager
                        .getInstance()
                        .publishTaskEvent(TaskEventFactory.newErrorEvent(task), TaskErrorEvent.class, true);
                TaskEventManager
                        .getInstance()
                        .publishTaskEvent(TaskEventFactory.newFinishedEvent(task), TaskFinishedEvent.class, true);
            }
        } finally {
            if (task.getType() == AutoJobTask.TaskType.DB_TASK) {
                ScheduleTaskUtil.oneTimeTask(() -> {
                    AutoJobMapperHolder.TRIGGER_ENTITY_MAPPER.updateStatus(task
                            .getTrigger()
                            .getFinishedTimes(), task
                            .getTrigger()
                            .getCurrentRepeatTimes()
                            .get(), Long.MAX_VALUE, task
                            .getTrigger()
                            .getTriggeringTime(), task
                            .getTrigger()
                            .getLastRunTime(), false, task.getId());
                }, 1, TimeUnit.MILLISECONDS);

            } else {
                AutoJobApplication
                        .getInstance()
                        .getMemoryTaskContainer()
                        .updateStatus(task
                                .getTrigger()
                                .getFinishedTimes(), task
                                .getTrigger()
                                .getCurrentRepeatTimes()
                                .get(), Long.MAX_VALUE, task
                                .getTrigger()
                                .getTriggeringTime(), task
                                .getTrigger()
                                .getLastRunTime(), false, task.getId());
            }
        }
    }


    @Override
    public void execute() {
        TaskEventHandlerDelegate
                .getInstance()
                .addHandler(TaskRunErrorEvent.class, this);
    }
}
