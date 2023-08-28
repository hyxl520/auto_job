package com.jingge.autojob.skeleton.model.tq;

import com.jingge.autojob.skeleton.framework.config.AutoJobConfigHolder;
import com.jingge.autojob.skeleton.framework.task.AutoJobRunningStatus;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lifecycle.TaskEventFactory;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskForbiddenEvent;
import com.jingge.autojob.skeleton.lifecycle.manager.TaskEventManager;
import com.jingge.autojob.skeleton.model.alert.AlertEventHandlerDelegate;
import com.jingge.autojob.skeleton.model.alert.event.AlertEventFactory;
import com.jingge.autojob.skeleton.model.register.AutoJobRegisterRefusedException;
import com.jingge.autojob.util.bean.ObjectUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 任务调度队列
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/02 17:25
 */
@Slf4j
public class AutoJobTaskQueue {
    private final BlockingQueue<AutoJobTask> blockingQueue;
    private final int maxLength;
    private final boolean isCheckExist;
    private final Comparator<AutoJobTask> comparator;


    public AutoJobTaskQueue(int maxLength, boolean isCheckExist) {
        this.maxLength = maxLength;
        this.isCheckExist = isCheckExist;
        this.comparator = new DefaultAutoJobTaskComparator();
        blockingQueue = new PriorityBlockingQueue<>(maxLength, comparator);
    }

    public AutoJobTaskQueue(int maxLength, boolean isCheckExist, Comparator<AutoJobTask> comparator) {
        this.maxLength = maxLength;
        this.isCheckExist = isCheckExist;
        blockingQueue = new PriorityBlockingQueue<>(maxLength, comparator);
        this.comparator = comparator;
    }

    public boolean joinTask(AutoJobTask task) {
        return joinTask(task, 0);
    }

    public boolean joinTask(AutoJobTask task, long wait, TimeUnit unit) {
        return joinTask(task, unit.toMillis(wait));
    }


    /**
     * 加入任务到任务队列，如果队列已满，可指定是否等待一定时间：ms
     *
     * @param task     任务
     * @param waitTime 要等待的时间：ms
     * @return boolean
     * @author Huang Yongxiang
     * @date 2021/12/9 10:42
     */
    public boolean joinTask(AutoJobTask task, long waitTime) {
        if (task == null) {
            log.error("不能插入空任务");
            return false;
        }
        if (!isAllowJoin(task)) {
            log.warn("任务{}不允许被放入调度队列", task.getId());
            return false;
        }
        if (isExists(task)) {
            if (AutoJobConfigHolder
                    .getInstance()
                    .isDebugEnable()) {
                log.warn("任务{}已在调度队列存在，不能注册到调度队列", task.getId());
            }
            return false;
        }
        if (!task.getIsAllowRegister()) {
            log.error("任务：{}不允许被插入", task.getId());
            TaskEventManager
                    .getInstance()
                    .publishTaskEvent(TaskEventFactory.newForbiddenEvent(task), TaskForbiddenEvent.class, true);
            AlertEventHandlerDelegate
                    .getInstance()
                    .doHandle(AlertEventFactory.newTaskRefuseHandleEvent(task));
            throw new AutoJobRegisterRefusedException("任务：" + task.getId() + "-" + task.getAlias() + "被拒绝执行");
        }
        if (task.getId() == null) {
            log.warn("任务Id不能为空，插入任务失败");
            return false;
        }
        try {
            if (blockingQueue.offer(task, waitTime, TimeUnit.MILLISECONDS)) {
                return true;
            } else {
                log.warn("经过：{}ms等待后依然无法加入，请尝试增加调度队列的容量重启服务", waitTime);
                return false;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 强制把一个任务加入到调度队列，强制加入将会忽略任务是否已存在且忽略任务是否允许被注册
     *
     * @param task     要强制加入的任务
     * @param waitTime 等待时间
     * @return boolean
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/8/7 14:56
     */
    public boolean forceJoinTask(AutoJobTask task, long waitTime, TimeUnit unit) {
        if (task == null) {
            log.error("不能插入空任务");
            return false;
        }
        if (!isAllowJoin(task)) {
            log.warn("任务{}不允许被放入调度队列，请检查任务相关配置", task.getId());
            return false;
        }
        try {
            if (blockingQueue.offer(task, unit.toMillis(waitTime), TimeUnit.MILLISECONDS)) {
                return true;
            } else {
                log.warn("经过：{}ms等待后依然无法加入，请尝试增加调度队列的容量重启服务", waitTime);
                return false;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }


    public AutoJobTask getTask() {
        if (blockingQueue.isEmpty()) {
            //log.warn("此时没有待执行任务");
            return null;
        }
        return blockingQueue.poll();
    }

    public AutoJobTask getTaskSync() {
        if (blockingQueue.isEmpty()) {
            //log.warn("此时没有待执行任务");
            return null;
        }
        try {
            return blockingQueue.take();
        } catch (InterruptedException e) {
            log.error("阻塞获取任务时被异常阻断");
        }
        return null;
    }

    public AutoJobTask getTask(long wait, TimeUnit unit) {
        if (blockingQueue.isEmpty()) {
            //log.warn("此时没有待执行任务");
            return null;
        }
        try {
            return blockingQueue.poll(wait, unit);
        } catch (InterruptedException e) {
            log.error("阻塞获取任务时被异常阻断");
        }
        return null;
    }

    public boolean removeTasks(List<AutoJobTask> tasks) {
        if (tasks == null) {
            log.error("移除任务失败，不合法的参数");
            return false;
        }
        try {
            for (AutoJobTask task : tasks) {
                blockingQueue.remove(task);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public AutoJobTask readTask() {
        return blockingQueue.peek();
    }

    public AutoJobTask replaceTasks(long taskId, AutoJobTask newTask) {
        if (taskId < 0 || newTask == null) {
            log.error("替换任务失败，不合法的参数");
            return null;
        }
        AutoJobTask newInstance = null;
        try {
            for (AutoJobTask task : blockingQueue) {
                if (task.getId() == taskId) {
                    newInstance = ObjectUtil.mergeObject(newTask, task, "id", "versionId", "isFinished", "runResult", "isAllowRegister");
                    newInstance.setId(taskId);
                    blockingQueue.remove(task);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return newInstance;
    }

    public boolean removeTaskById(long id) {
        try {
            return blockingQueue.removeIf(task -> task.getId() == id);
        } catch (Exception e) {
            return false;
        }
    }

    public AutoJobTask removeTaskByScheduleQueueID(long scheduleQueueID) {
        try {
            AutoJobTask removed = getTaskByScheduleQueueID(scheduleQueueID);
            blockingQueue.removeIf(task -> task.getScheduleQueueId() == scheduleQueueID);
            return removed;
        } catch (Exception e) {
            return null;
        }
    }

    public List<AutoJobTask> getSortedList() {
        return blockingQueue
                .stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    public Stream<AutoJobTask> stream() {
        return blockingQueue.stream();
    }

    public List<AutoJobTask> removeAndGetTask(long id) {
        List<AutoJobTask> autoJobTask = getTaskById(id);
        if (autoJobTask != null && removeTaskById(id)) {
            return autoJobTask;
        }
        return null;
    }


    public List<AutoJobTask> getTaskById(long taskId) {
        List<AutoJobTask> tasks = new ArrayList<>();
        for (AutoJobTask task : blockingQueue) {
            if (task.getId() == taskId) {
                tasks.add(task);
            }
        }
        return tasks;
    }

    public AutoJobTask getTaskByScheduleQueueID(long scheduleQueueID) {
        for (AutoJobTask task : blockingQueue) {
            if (task.getScheduleQueueId() == scheduleQueueID) {
                return task;
            }
        }
        return null;
    }

    /**
     * 判断指定方法在调度队列中是否已存在，以下情况认为存在
     * <li>1、同一个任务对象</li>
     * <li>2、ID相同</li>
     * <li>3、方法所在的类、方法名均相同</li>
     *
     * @param task 要比较的task对象
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/1/24 16:38
     */
    public boolean isExists(AutoJobTask task) {
        //如果不检查是否存在，默认返回false
        if (!isCheckExist) {
            return false;
        }
        if (blockingQueue.contains(task)) {
            return true;
        }
        for (AutoJobTask task1 : blockingQueue) {
            if (task1.equals(task)) {
                return true;
            }
        }
        return false;
    }

    public BlockingQueue<AutoJobTask> getBlockingQueue() {
        return blockingQueue;
    }

    public int size() {
        return blockingQueue.size();
    }

    private boolean isAllowJoin(AutoJobTask task) {
        if (task.getTrigger() == null || task.getId() == null) {
            return false;
        }
        return task
                .getTrigger()
                .getTriggeringTime() > System.currentTimeMillis() && !task
                .getTrigger()
                .getIsPause() && (task.getRunningStatus() == AutoJobRunningStatus.SCHEDULING || task.getRunningStatus() == AutoJobRunningStatus.RETRYING);
    }

    /**
     * 基于开始时间进行排序
     */
    private static class DefaultAutoJobTaskComparator implements java.util.Comparator<AutoJobTask> {
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
