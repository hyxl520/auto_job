package com.jingge.autojob.skeleton.model.scheduler;

import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.config.AutoJobConfigHolder;
import com.jingge.autojob.skeleton.framework.config.AutoJobExecutorPoolConfig;
import com.jingge.autojob.skeleton.framework.task.AutoJobRunningStatus;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.framework.task.AutoJobContext;
import com.jingge.autojob.skeleton.model.executor.AutoJobTaskExecutorPool;
import com.jingge.autojob.skeleton.model.register.IAutoJobRegister;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 调度器，调度器是个抽象概念，因其拥有执行器池和注册器的实现
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/04 10:35
 */
@Slf4j
public abstract class AbstractScheduler {
    protected AutoJobTaskExecutorPool executorPool;
    protected IAutoJobRegister register;
    protected AutoJobConfigHolder configHolder;
    protected AutoJobRunningLock runningLock;


    /**
     * 调度器的通用构造方法，框架自动注册调度器时会执行该构造方法
     *
     * @param executorPool 执行器池
     * @param register     注册器
     * @param configHolder 配置源
     * @author Huang Yongxiang
     * @date 2022/8/19 15:18
     */
    public AbstractScheduler(AutoJobTaskExecutorPool executorPool, IAutoJobRegister register, AutoJobConfigHolder configHolder) {
        this(executorPool, register, configHolder, new DBOptimisticLock());
    }

    /**
     * 调度器的构造方法，支持配置执行器池、注册器、配置、以及分布式锁配置，通过这个方法可以让你的调度器支持其他分布式锁
     *
     * @param executorPool 执行器池
     * @param register     注册器
     * @param configHolder 配置源
     * @param runningLock  分布式锁
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/7/26 16:00
     */
    public AbstractScheduler(AutoJobTaskExecutorPool executorPool, IAutoJobRegister register, AutoJobConfigHolder configHolder, AutoJobRunningLock runningLock) {
        this.executorPool = executorPool;
        this.register = register;
        this.configHolder = configHolder;
        this.runningLock = runningLock;
    }

    public boolean lock(long taskId) {
        boolean flag = runningLock.lock(taskId);
        if (!flag) {
            if (AutoJobConfigHolder
                    .getInstance()
                    .isDebugEnable()) {
                log.warn("获取DB任务：{}的锁失败，任务将不会执行", taskId);
            }
            //register.removeTask(taskId);
        } else {
            if (AutoJobConfigHolder
                    .getInstance()
                    .isDebugEnable()) {
                log.warn("获取DB任务：{}的锁成功", taskId);
            }
            AutoJobContext
                    .getOnLockMap()
                    .put(taskId, true);
        }
        return flag;
    }

    public boolean isLatest(AutoJobTask task) {
        if (task.getType() == AutoJobTask.TaskType.MEMORY_TASk || task.getVersionId() == null) {
            return true;
        }
        boolean flag = AutoJobMapperHolder
                .getMatchTaskMapper(task.getId())
                .isLatestVersion(task.getVersionId(), task.getId());
        if (!flag && AutoJobConfigHolder
                .getInstance()
                .isDebugEnable()) {
            log.warn("任务：{}不是最新版本，不会执行", task.getId());
        }
        return flag;
    }

    public boolean unlock(long taskId) {
        if (runningLock.unlock(taskId)) {
            AutoJobContext
                    .getOnLockMap()
                    .put(taskId, false);
            return true;
        }
        return false;
    }

    /**
     * 提交一个任务到执行器池，该方法将会根据给定的任务触发器信息以及阈值决定将任务提交到fast-pool还是slow-pool
     *
     * @param task      要提交的任务
     * @param threshold 上次执行时长大于该阈值的将会提交到slow-pool，反之提交到fast-pool
     * @param unit      单位
     * @return void
     * @author Huang Yongxiang
     * @date 2022/9/9 9:38
     */
    protected void submitTask(AutoJobTask task, long threshold, TimeUnit unit) {
        if (task == null || task.getTrigger() == null || task
                .getTrigger()
                .getIsPause() || !task.getIsAllowRegister()) {
            return;
        }
        //if (task.getRunningStatus() == AutoJobRunningStatus.RUNNING) {
        //    log.warn("任务{} miss fire", task.getId());
        //    TaskEventManager
        //            .getInstance()
        //            .publishTaskEventSync(TaskEventFactory.newTaskMissFireEvent(task), TaskMissFireEvent.class, true);
        //    return;
        //}
        task.updateRunningStatus(AutoJobRunningStatus.WAITING);
        if (task
                .getTrigger()
                .getLastRunTime() > unit.toMillis(threshold)) {
            executorPool.submit2SlowPool(task.getExecutable(), task.getRunnablePostProcessor());
        } else {
            executorPool.submit2FastPool(task.getExecutable(), task.getRunnablePostProcessor());
        }

    }

    /**
     * 提交一个任务到执行器池，如果该任务上次执行时长大于配置的relegationThreshold将会提交到slow-pool
     *
     * @param task 要提交的任务
     * @return void
     * @author Huang Yongxiang
     * @date 2022/9/9 9:40
     */
    protected void submitTask(AutoJobTask task) {
        AutoJobExecutorPoolConfig config = AutoJobApplication
                .getInstance()
                .getConfigHolder()
                .getAutoJobConfig()
                .getExecutorPoolConfig();
        submitTask(task, (long) (config.getRelegationThreshold() * 60 * 1000), TimeUnit.MILLISECONDS);
    }

    /**
     * 定义调度器的级别，高级别的调度器将会被优先执行
     *
     * @return int
     * @author Huang Yongxiang
     * @date 2022/9/9 9:37
     */
    public int getSchedulerLevel() {
        return 0;
    }

    public void beforeExecute() {

    }

    /**
     * 调度器的启动方法，启动逻辑写在该方法里
     */
    public void execute() {

    }

    public void afterExecute() {

    }

    public void executeError(Throwable throwable) {

    }

    public void beforeDestroy() {

    }

    /**
     * 调度器的摧毁方法，关闭逻辑写在这里
     */
    public void destroy() {

    }

    public void afterDestroy() {

    }

    public void destroyError(Throwable throwable) {

    }

}
