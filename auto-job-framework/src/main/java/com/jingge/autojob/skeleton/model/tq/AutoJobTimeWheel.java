package com.jingge.autojob.skeleton.model.tq;

import com.jingge.autojob.skeleton.framework.task.AutoJobRunningStatus;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lifecycle.TaskEventFactory;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskForbiddenEvent;
import com.jingge.autojob.skeleton.lifecycle.manager.TaskEventManager;
import com.jingge.autojob.skeleton.model.alert.AlertEventHandlerDelegate;
import com.jingge.autojob.skeleton.model.alert.event.AlertEventFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 时间轮
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/07 10:56
 */
@Slf4j
public class AutoJobTimeWheel {
    private final Map<Integer, List<AutoJobTask>> timeWheel;
    private final AtomicInteger size;
    private final Comparator<AutoJobTask> comparator = new DefaultComparator();

    public AutoJobTimeWheel() {
        this.timeWheel = new ConcurrentHashMap<>(60);
        for (int i = 0; i <= 59; i++) {
            timeWheel.put(i, new LinkedList<>());
        }
        size = new AtomicInteger(0);
    }


    public boolean joinTask(AutoJobTask task) {
        try {
            if (!task.getIsAllowRegister()) {
                log.error("任务：{}不允许被插入", task.getId());
                TaskEventManager
                        .getInstance()
                        .publishTaskEvent(TaskEventFactory.newForbiddenEvent(task), TaskForbiddenEvent.class, true);
                AlertEventHandlerDelegate
                        .getInstance()
                        .doHandle(AlertEventFactory.newTaskRefuseHandleEvent(task));
                return false;
            }
            int second = (int) ((task
                    .getTrigger()
                    .getTriggeringTime() / 1000) % 60);
            //log.warn("任务：{}注册到：{}", task.getId(), second);
            if (second >= 0) {
                timeWheel
                        .get(second)
                        .add(task);
                size.incrementAndGet();
                task.updateRunningStatus(AutoJobRunningStatus.LOCKED);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<AutoJobTask> getSecondTasks(int second) {
        if (second < 0 || second > 59) {
            throw new IllegalArgumentException("秒数只能位于0-59");
        }
        try {
            List<AutoJobTask> sorted = timeWheel
                    .get(second)
                    .stream()
                    .sorted(comparator)
                    .collect(Collectors.toList());
            timeWheel
                    .get(second)
                    .clear();
            size.addAndGet(-1 * sorted.size());
            return sorted;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }


    public AutoJobTask getTaskById(long taskId) {
        for (Map.Entry<Integer, List<AutoJobTask>> entry : timeWheel.entrySet()) {
            for (AutoJobTask task : entry.getValue()) {
                if (task.getId() == taskId) {
                    return task;
                }
            }
        }
        return null;
    }

    public boolean removeTaskById(long taskId) {
        for (Map.Entry<Integer, List<AutoJobTask>> entry : timeWheel.entrySet()) {
            boolean flag = entry
                    .getValue()
                    .removeIf(task -> task.getId() == taskId);
            if (flag) {
                size.decrementAndGet();
            }
            return flag;
        }
        return false;
    }

    public AutoJobTask removeAndGetTask(long taskId) {
        AutoJobTask task = getTaskById(taskId);
        if (task != null && removeTaskById(taskId)) {
            return task;
        }
        return null;
    }

    public boolean isExists(AutoJobTask task) {
        if (task == null) {
            return false;
        }
        for (Map.Entry<Integer, List<AutoJobTask>> entry : timeWheel.entrySet()) {
            for (AutoJobTask task1 : entry.getValue()) {
                if (task.equals(task1)) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<AutoJobTask> getAllTasks() {
        List<AutoJobTask> all = new ArrayList<>();
        for (Map.Entry<Integer, List<AutoJobTask>> entry : timeWheel.entrySet()) {
            all.addAll(entry.getValue());
        }
        return all;
    }

    public void clear() {
        for (Map.Entry<Integer, List<AutoJobTask>> entry : timeWheel.entrySet()) {
            entry
                    .getValue()
                    .clear();
        }
        size.set(0);
    }

    /**
     * 获取所有任务的迭代器，迭代器是弱一致的且非有序的
     *
     * @return java.util.Iterator<com.example.autojob.skeleton.framework.task.AutoJobTask>
     * @author Huang Yongxiang
     * @date 2022/8/7 11:53
     */
    public Iterator<AutoJobTask> iterator() {
        List<AutoJobTask> all = new ArrayList<>();
        for (Map.Entry<Integer, List<AutoJobTask>> entry : timeWheel.entrySet()) {
            all.addAll(entry.getValue());
        }
        return all.iterator();
    }

    public int size() {
        return size.get();
    }


    private static class DefaultComparator implements Comparator<AutoJobTask> {
        @Override
        public int compare(AutoJobTask o1, AutoJobTask o2) {
            if (o1.getTaskLevel() == -1 && o2.getTaskLevel() == -1) {
                return Long.compare(o1
                        .getTrigger()
                        .getTriggeringTime(), o2
                        .getTrigger()
                        .getTriggeringTime());
            }
            return Integer.compare(o1.getTaskLevel(), o2.getTaskLevel());
        }
    }
}
