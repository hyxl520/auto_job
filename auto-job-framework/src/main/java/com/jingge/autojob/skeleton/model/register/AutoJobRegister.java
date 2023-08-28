package com.jingge.autojob.skeleton.model.register;

import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.config.AutoJobConfig;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lang.LifeCycleHook;
import com.jingge.autojob.skeleton.lifecycle.TaskEventFactory;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskBeforeRegisterEvent;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskRegisteredEvent;
import com.jingge.autojob.skeleton.lifecycle.manager.TaskEventManager;
import com.jingge.autojob.skeleton.model.tq.AutoJobTaskQueue;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 注册器
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/04 12:55
 */
@Slf4j
public class AutoJobRegister implements IAutoJobRegister, LifeCycleHook {
    private final AutoJobTaskQueue autoJobTaskQueue;
    private AbstractRegisterHandler handler;
    private AbstractRegisterFilter filter;
    private final AutoJobConfig config;

    public AutoJobRegister setFilter(AbstractRegisterFilter filter) {
        if (filter == null) {
            throw new NullPointerException();
        }
        if (this.filter == null) {
            this.filter = filter;
        } else {
            this.filter.add(filter);
        }
        return this;
    }

    public AutoJobRegister setHandler(AbstractRegisterHandler handler) {
        if (handler == null) {
            throw new NullPointerException();
        }
        if (this.handler == null) {
            this.handler = handler;
        } else {
            this.handler.add(handler);
        }
        return this;
    }

    public AutoJobRegister(AutoJobTaskQueue autoJobTaskQueue, AbstractRegisterHandler handler, AbstractRegisterFilter filter, AutoJobConfig config) {
        this.autoJobTaskQueue = autoJobTaskQueue;
        this.handler = handler;
        this.filter = filter;
        this.config = config;
    }

    public AutoJobRegister(AutoJobTaskQueue autoJobTaskQueue) {
        this.autoJobTaskQueue = autoJobTaskQueue;
        this.config = AutoJobApplication
                .getInstance()
                .getConfigHolder()
                .getAutoJobConfig();
    }

    @Override
    public boolean registerTask(AutoJobTask task) {
        beforeInitialize(task);
        boolean flag;
        flag = autoJobTaskQueue.joinTask(task);
        //if (flag) {
        //    AutoJobContext.inSchedulingProgress(task.getId());
        //}
        afterInitialize(task);
        return flag;
    }

    @Override
    public boolean registerTask(AutoJobTask task, boolean isForced, long waitTime, TimeUnit unit) {
        beforeInitialize(task);
        boolean flag;
        if (!isForced) {
            flag = autoJobTaskQueue.joinTask(task, waitTime, unit);
        } else {
            flag = autoJobTaskQueue.forceJoinTask(task, waitTime, unit);
        }
        afterInitialize(task);
        return flag;
    }

    @Override
    public AutoJobTask takeTask() {
        AutoJobTask task = autoJobTaskQueue.getTask();
        afterClose(task);
        return task;
    }

    @Override
    public AutoJobTask takeTask(long waitTime, TimeUnit unit) {
        AutoJobTask task = autoJobTaskQueue.getTask(waitTime, unit);
        afterClose(task);
        return task;
    }

    @Override
    public AutoJobTask readTask() {
        return autoJobTaskQueue.readTask();
    }

    public boolean removeTask(long taskId) {
        autoJobTaskQueue
                .getTaskById(taskId)
                .forEach(this::afterClose);
        return autoJobTaskQueue.removeTaskById(taskId);
    }

    @Override
    public AutoJobTask mergeAndReplaceTaskAndGet(long taskId, AutoJobTask newInstance) {
        AutoJobTask newTask = autoJobTaskQueue.replaceTasks(taskId, newInstance);
        if (newTask != null) {
            registerTask(newTask);
        }
        return newTask;
    }

    @Override
    public List<AutoJobTask> removeAndGetTask(long taskId) {
        List<AutoJobTask> tasks = autoJobTaskQueue.removeAndGetTask(taskId);
        tasks.forEach(this::afterClose);
        return tasks;
    }

    @Override
    public AutoJobTask removeAndGetTaskByScheduleQueueID(long scheduleQueueID) {
        AutoJobTask task = autoJobTaskQueue.removeTaskByScheduleQueueID(scheduleQueueID);
        afterClose(task);
        return task;
    }

    @Override
    public List<AutoJobTask> getTaskById(long taskId) {
        return autoJobTaskQueue.getTaskById(taskId);
    }

    @Override
    public AutoJobTask getTaskByScheduleQueueID(long scheduleQueueID) {
        return autoJobTaskQueue.getTaskByScheduleQueueID(scheduleQueueID);
    }

    @Override
    public Iterator<AutoJobTask> iterator() {
        return autoJobTaskQueue
                .getBlockingQueue()
                .iterator();
    }

    @Override
    public int size() {
        return autoJobTaskQueue.size();
    }

    @Override
    public List<AutoJobTask> filter(Predicate<AutoJobTask> predicate) {
        return autoJobTaskQueue
                .stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    @Override
    public void beforeInitialize(Object... params) {
        if (params != null && params.length == 1 && params[0] instanceof AutoJobTask) {
            AutoJobTask task = ((AutoJobTask) params[0]);
            if (filter != null && config.getEnableRegisterFilter()) {
                filter.doHandle(task);
            }
            if (handler != null) {
                handler.doHandle(task);
            }
            TaskEventManager
                    .getInstance()
                    .publishTaskEventSync(TaskEventFactory.newBeforeRegisterEvent(task), TaskBeforeRegisterEvent.class, true);
        }
    }

    @Override
    public void afterInitialize(Object... params) {
        if (params != null && params.length == 1 && params[0] instanceof AutoJobTask) {
            AutoJobTask task = ((AutoJobTask) params[0]);
            task.inScheduleQueue();
            //log.warn("任务{}-{}注册进调度队列", task.getId(), task.getScheduleQueueId());
            TaskEventManager
                    .getInstance()
                    .publishTaskEvent(TaskEventFactory.newRegisteredEvent(task), TaskRegisteredEvent.class, true);
        }
    }

    @Override
    public void afterClose(Object... params) {
        if (params != null && params.length == 1 && params[0] != null && params[0] instanceof AutoJobTask) {
            AutoJobTask task = ((AutoJobTask) params[0]);
            //log.warn("任务{}-{}移除出调度队列", task.getId(), task.getScheduleQueueId());
            task.outScheduleQueue();
        }
    }
}
