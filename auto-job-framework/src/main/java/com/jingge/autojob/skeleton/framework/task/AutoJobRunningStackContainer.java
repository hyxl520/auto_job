package com.jingge.autojob.skeleton.framework.task;

import com.jingge.autojob.skeleton.lifecycle.ITaskEventHandler;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskFinishedEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 运行堆栈容器
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-03-21 15:19
 * @email 1158055613@qq.com
 */
@Slf4j
public class AutoJobRunningStackContainer implements ITaskEventHandler<TaskFinishedEvent> {
    private final Map<Long, AutoJobRunningStack> container = new ConcurrentHashMap<>();

    private AutoJobRunningStackContainer() {
    }

    public static AutoJobRunningStackContainer getInstance() {
        return InstanceHolder.CONTAINER;
    }

    protected void addNew(AutoJobTask task) {
        int deep = task
                .getTrigger()
                .getRepeatTimes() > 0 ? task
                .getTrigger()
                .getRepeatTimes() : -1;
        addNew(task, deep);
    }

    protected void addNew(AutoJobTask task, int depth) {
        if (container.containsKey(task.getTrigger().schedulingRecordID)) {
            return;
        }
        container.put(task.getId(), new AutoJobRunningStack(task.getId(), depth));
        log.debug("新增任务{}的运行堆栈", task.id);
    }

    void addEntry(long taskID, RunningStackEntry entry) {
        if (!container.containsKey(taskID)) {
            container.put(taskID, new AutoJobRunningStack(taskID));
        }
        container
                .get(taskID)
                .add(entry);
    }

    public AutoJobRunningStack remove(long taskID) {
        return container.remove(taskID);
    }

    public AutoJobRunningStack get(long taskID) {
        return container.get(taskID);
    }

    @Override
    public void doHandle(TaskFinishedEvent event) {
        log.debug("移除任务{}的运行堆栈", event.getTask().id);
        remove(event.getTask().id);
    }

    static class InstanceHolder {
        private static final AutoJobRunningStackContainer CONTAINER = new AutoJobRunningStackContainer();
    }
}
