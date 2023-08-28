package com.jingge.autojob.skeleton.model.scheduler;

import com.jingge.autojob.skeleton.db.entity.AutoJobMethodTaskEntity;
import com.jingge.autojob.skeleton.db.entity.AutoJobScriptTaskEntity;
import com.jingge.autojob.skeleton.db.entity.EntityConvertor;
import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.config.AutoJobConfigHolder;
import com.jingge.autojob.skeleton.framework.config.AutoJobConstant;
import com.jingge.autojob.skeleton.framework.container.MemoryTaskContainer;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lifecycle.ITaskEventHandler;
import com.jingge.autojob.skeleton.lifecycle.TaskEventFactory;
import com.jingge.autojob.skeleton.lifecycle.TaskEventHandlerDelegate;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskFinishedEvent;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskRunSuccessEvent;
import com.jingge.autojob.skeleton.lifecycle.manager.TaskEventManager;
import com.jingge.autojob.skeleton.model.executor.AutoJobTaskExecutorPool;
import com.jingge.autojob.skeleton.model.register.IAutoJobRegister;
import com.jingge.autojob.skeleton.model.task.functional.FunctionTask;
import com.jingge.autojob.util.convert.DateUtils;
import com.jingge.autojob.util.thread.ScheduleTaskUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 运行成功调度器
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/19 14:41
 */
@Slf4j
public class AutoJobRunSuccessScheduler extends AbstractScheduler implements ITaskEventHandler<TaskRunSuccessEvent> {
    private final ScheduleTaskUtil childTaskScheduleThread;
    private final MemoryTaskContainer container = AutoJobApplication
            .getInstance()
            .getMemoryTaskContainer();

    public AutoJobRunSuccessScheduler(AutoJobTaskExecutorPool executorPool, IAutoJobRegister register, AutoJobConfigHolder configHolder) {
        super(executorPool, register, configHolder);
        this.childTaskScheduleThread = ScheduleTaskUtil.build(true, "childTaskScheduleThread");
    }

    @Override
    public void doHandle(TaskRunSuccessEvent event) {
        AutoJobTask task = event.getTask();
        if (task.getType() == AutoJobTask.TaskType.DB_TASK) {
            unlock(task.getId());
        }
        //异步处理子任务，保证调度的高效性
        childTaskScheduleThread.EOneTimeTask(() -> {
            //处理子任务
            if (task
                    .getTrigger()
                    .hasChildTask()) {
                findChildTask(task).forEach(c -> {
                    if (c.getType() == AutoJobTask.TaskType.MEMORY_TASk || (c.getType() == AutoJobTask.TaskType.DB_TASK && isLatest(task) && lock(c.getId()))) {
                        submitTask(c);
                    }
                });
            }
            return null;
        }, 0, TimeUnit.MILLISECONDS);
        try {
            if (!task.getIsChildTask()) {
                if (task
                        .getTrigger()
                        .refresh()) {
                    if (AutoJobConfigHolder
                            .getInstance()
                            .isDebugEnable()) {
                        log.warn("任务{}刷新成功，下次执行时间{}", task.getId(), DateUtils.formatDateTime(task
                                .getTrigger()
                                .getTriggeringTime()));
                    }
                    if (task
                            .getTrigger()
                            .isNearTriggeringTime(AutoJobConstant.dbSchedulerRate * 2)) {
                        register.registerTask(task);
                    }
                    if (task.getType() == AutoJobTask.TaskType.DB_TASK) {
                        //异步更新DB任务的状态
                        childTaskScheduleThread.EOneTimeTask(() -> {
                            AutoJobMapperHolder.TRIGGER_ENTITY_MAPPER.updateStatus(task
                                    .getTrigger()
                                    .getFinishedTimes(), task
                                    .getTrigger()
                                    .getCurrentRepeatTimes()
                                    .get(), event.getTriggeringTime(), task
                                    .getTrigger()
                                    .getTriggeringTime(), task
                                    .getTrigger()
                                    .getLastRunTime(), true, task.getId());
                            return null;
                        }, 0, TimeUnit.MILLISECONDS);
                    } else {
                        container.updateStatus(task
                                .getTrigger()
                                .getFinishedTimes(), task
                                .getTrigger()
                                .getCurrentRepeatTimes()
                                .get(), event.getTriggeringTime(), task
                                .getTrigger()
                                .getTriggeringTime(), task
                                .getTrigger()
                                .getLastRunTime(), true, task.getId());
                    }
                }
                //任务无法刷新下次执行时间说明任务已完成
                else {
                    //异步更新任务状态
                    if (task.getType() == AutoJobTask.TaskType.DB_TASK) {
                        childTaskScheduleThread.EOneTimeTask(() -> AutoJobMapperHolder.TRIGGER_ENTITY_MAPPER.updateStatus(task
                                .getTrigger()
                                .getFinishedTimes(), task
                                .getTrigger()
                                .getCurrentRepeatTimes()
                                .get(), event.getTriggeringTime(), Long.MAX_VALUE, task
                                .getTrigger()
                                .getLastRunTime(), true, task.getId()), 0, TimeUnit.MILLISECONDS);
                    } else {
                        container.updateStatus(task
                                .getTrigger()
                                .getFinishedTimes(), task
                                .getTrigger()
                                .getCurrentRepeatTimes()
                                .get(), event.getTriggeringTime(), Long.MAX_VALUE, task
                                .getTrigger()
                                .getLastRunTime(), true, task.getId());
                    }
                    //如果是因为任务暂停而无法刷新的就直接退出，不更新任务已完成状态
                    if (task
                            .getTrigger()
                            .getIsPause()) {
                        return;
                    }
                    task
                            .getRunResult()
                            .finish();
                    task.setIsFinished(true);
                    TaskEventManager
                            .getInstance()
                            .publishTaskEvent(TaskEventFactory.newFinishedEvent(event.getTask()), TaskFinishedEvent.class, true);
                }
            } else {
                if (task.getType() == AutoJobTask.TaskType.DB_TASK) {
                    //异步更新DB任务的状态
                    childTaskScheduleThread.EOneTimeTask(() -> {
                        AutoJobMapperHolder.TRIGGER_ENTITY_MAPPER.updateStatus(task
                                .getTrigger()
                                .getFinishedTimes(), task
                                .getTrigger()
                                .getCurrentRepeatTimes()
                                .get(), event.getTriggeringTime(), Long.MAX_VALUE, task
                                .getTrigger()
                                .getLastRunTime(), true, task.getId());
                        return null;
                    }, 0, TimeUnit.MILLISECONDS);
                } else {
                    container.updateStatus(task
                            .getTrigger()
                            .getFinishedTimes(), task
                            .getTrigger()
                            .getCurrentRepeatTimes()
                            .get(), event.getTriggeringTime(), Long.MAX_VALUE, task
                            .getTrigger()
                            .getLastRunTime(), true, task.getId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private List<AutoJobTask> findChildTask(AutoJobTask parent) {
        if (parent
                .getTrigger()
                .hasChildTask()) {
            List<Long> childTaskIds = parent
                    .getTrigger()
                    .getChildTask();
            List<AutoJobTask> tasks = childTaskIds
                    .stream()
                    .map(id -> {
                        AutoJobTask task = container.getById(id);
                        if (task == null) {
                            task = container.getByVersionId(id);
                        }
                        return task;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            List<AutoJobScriptTaskEntity> scriptTaskEntities = AutoJobMapperHolder.SCRIPT_TASK_ENTITY_MAPPER.selectChildTasks(childTaskIds);
            List<AutoJobMethodTaskEntity> methodTaskEntities = AutoJobMapperHolder.METHOD_TASK_ENTITY_MAPPER.selectChildTasks(childTaskIds);
            tasks.addAll(scriptTaskEntities
                    .stream()
                    .map(EntityConvertor::entity2Task)
                    .collect(Collectors.toList()));
            tasks.addAll(methodTaskEntities
                    .stream()
                    .map(EntityConvertor::entity2Task)
                    .collect(Collectors.toList()));
            return tasks;
        }
        return Collections.emptyList();
    }


    @Override
    public int getHandlerLevel() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void execute() {
        TaskEventHandlerDelegate
                .getInstance()
                .addHandler(TaskRunSuccessEvent.class, this);
    }

    @Override
    public void destroy() {
        childTaskScheduleThread.shutdown();
    }
}
