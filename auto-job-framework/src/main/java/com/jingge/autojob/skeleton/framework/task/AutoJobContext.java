package com.jingge.autojob.skeleton.framework.task;

import com.jingge.autojob.logging.model.producer.AutoJobLogHelper;
import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.config.AutoJobConfigHolder;
import com.jingge.autojob.skeleton.lang.WithDaemonThread;
import com.jingge.autojob.skeleton.model.executor.AutoJobTaskExecutorPool;
import com.jingge.autojob.skeleton.model.register.IAutoJobRegister;
import com.jingge.autojob.skeleton.model.scheduler.AbstractScheduler;
import com.jingge.autojob.skeleton.model.scheduler.AutoJobRunningLock;
import com.jingge.autojob.util.convert.DefaultValueUtil;
import com.jingge.autojob.util.thread.SyncHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 任务运行上下文，提供运行时的任务操作
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/04 15:12
 */
@Slf4j
public class AutoJobContext extends AbstractScheduler implements WithDaemonThread {
    /**
     * 执行任务的线程将绑定当前线程执行的调度Id到这里，子线程也将继承该值
     */
    private static final InheritableThreadLocal<Long> currentScheduleID = new InheritableThreadLocal<>();
    /**
     * 当前线程执行的任务
     */
    private static final InheritableThreadLocal<AutoJobTask> concurrentThreadTask = new InheritableThreadLocal<>();
    /**
     * 已加锁的DB任务
     */
    private static final Map<Long, Boolean> lockedMap = new ConcurrentHashMap<>();
    /**
     * 正在运行的任务
     */
    private static final Map<Long, AutoJobTask> runningTask = new ConcurrentHashMap<>();
    /**
     * 正在运行的任务对应的线程
     */
    private static final Map<Long, Thread> runningThread = new ConcurrentHashMap<>();

    private boolean isStop = false;


    public AutoJobContext(AutoJobTaskExecutorPool executorPool, IAutoJobRegister register, AutoJobConfigHolder configHolder) {
        super(executorPool, register, configHolder);
    }

    public AutoJobContext(AutoJobTaskExecutorPool executorPool, IAutoJobRegister register, AutoJobConfigHolder configHolder, AutoJobRunningLock runningLock) {
        super(executorPool, register, configHolder, runningLock);
    }

    /**
     * 添加一个运行中的任务
     *
     * @param autoJobTask 正在运行的任务
     * @return void
     * @author Huang Yongxiang
     * @date 2022/8/22 17:07
     */
    static void registerRunningTask(AutoJobTask autoJobTask) {
        runningTask.put(autoJobTask
                .getTrigger()
                .getSchedulingRecordID(), autoJobTask);
        runningThread.put(autoJobTask
                .getTrigger()
                .getSchedulingRecordID(), Thread.currentThread());
    }

    /**
     * 移除一个正在运行的任务
     *
     * @param autoJobTask 要被移除的任务
     * @return void
     * @author Huang Yongxiang
     * @date 2022/8/22 17:07
     */
    static void removeRunningTask(AutoJobTask autoJobTask) {
        runningTask.remove(autoJobTask
                .getTrigger()
                .getSchedulingRecordID());
        runningThread.remove(autoJobTask
                .getTrigger()
                .getSchedulingRecordID());
    }

    /**
     * 尝试对已经加锁的DB任务解锁
     *
     * @return int 成功解锁的任务书
     * @author Huang Yongxiang
     * @date 2022/8/22 17:06
     */
    public int unlock() {
        int count = 0;
        for (Map.Entry<Long, Boolean> entry : lockedMap.entrySet()) {
            if (entry.getValue()) {
                if (this.unlock(entry.getKey())) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 尝试停止一个正在运行中的任务
     *
     * @param scheduleID 尝试停止的任务的调度ID
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/8/22 17:11
     */
    public static boolean stopRunningTask(long scheduleID) {
        if (runningThread.containsKey(scheduleID)) {
            try {
                Thread runThread = runningThread.get(scheduleID);
                runThread.interrupt();
                if (runThread.isAlive() && runThread.isInterrupted()) {
                    runningTask.remove(scheduleID);
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static InheritableThreadLocal<Long> getCurrentScheduleID() {
        return currentScheduleID;
    }

    public static Map<Long, Boolean> getOnLockMap() {
        return lockedMap;
    }

    public static Map<Long, AutoJobTask> getRunningTask() {
        return runningTask;
    }

    public static InheritableThreadLocal<AutoJobTask> getConcurrentThreadTask() {
        return concurrentThreadTask;
    }

    @Override
    public void execute() {
        startWork();
    }

    @Override
    public void destroy() {
        isStop = true;
    }

    /**
     * 提供任务过长限制，超过给定执行时长的任务将会尝试中断
     *
     * @return void
     * @author Huang Yongxiang
     * @date 2022/8/26 14:24
     */
    @Override
    public void startWork() {
        Thread stopLongTaskThread = new Thread(() -> {
            do {
                try {
                    SyncHelper.sleepQuietly(1, TimeUnit.MILLISECONDS);
                    for (Map.Entry<Long, AutoJobTask> entry : runningTask.entrySet()) {
                        if (entry
                                .getValue()
                                .getTrigger()
                                .getStartRunTime() == 0) {
                            continue;
                        }
                        Long maximumExecutionTime = entry
                                .getValue()
                                .getTrigger()
                                .getMaximumExecutionTime();
                        if (maximumExecutionTime != null && maximumExecutionTime > 0) {
                            long runTime = System.currentTimeMillis() - entry
                                    .getValue()
                                    .getTrigger()
                                    .getStartRunTime();
                            if (runTime > maximumExecutionTime) {
                                DefaultValueUtil
                                        .defaultValue(entry
                                                .getValue()
                                                .getLogHelper(), new AutoJobLogHelper(log, entry.getValue()))
                                        .info("任务：{}已执行{}ms，最长运行时间：{}，尝试进行停止", entry.getKey(), runTime, maximumExecutionTime);
                                if (stopRunningTask(entry.getKey())) {
                                    DefaultValueUtil
                                            .defaultValue(entry
                                                    .getValue()
                                                    .getLogHelper(), new AutoJobLogHelper(log, entry.getValue()))
                                            .info("任务{}停止成功", entry.getKey());
                                } else {
                                    DefaultValueUtil
                                            .defaultValue(entry
                                                    .getValue()
                                                    .getLogHelper(), new AutoJobLogHelper(log, entry.getValue()))
                                            .info("任务{}停止失败", entry.getKey());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } while (!isStop);
        });
        stopLongTaskThread.setDaemon(true);
        stopLongTaskThread.setName("stopLongTaskThread");
        stopLongTaskThread.start();
    }
}
