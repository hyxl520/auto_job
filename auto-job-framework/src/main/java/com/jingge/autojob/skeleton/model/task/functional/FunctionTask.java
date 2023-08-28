package com.jingge.autojob.skeleton.model.task.functional;

import com.jingge.autojob.api.log.AutoJobLogAPI;
import com.jingge.autojob.logging.domain.AutoJobLog;
import com.jingge.autojob.logging.domain.AutoJobRunLog;
import com.jingge.autojob.skeleton.enumerate.SchedulingStrategy;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.config.AutoJobConstant;
import com.jingge.autojob.skeleton.framework.config.AutoJobRetryConfig;
import com.jingge.autojob.skeleton.framework.config.RetryStrategy;
import com.jingge.autojob.skeleton.framework.task.AutoJobRunResult;
import com.jingge.autojob.skeleton.framework.task.AutoJobRunningStatus;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.framework.task.AutoJobTrigger;
import com.jingge.autojob.skeleton.model.builder.AutoJobTriggerFactory;
import com.jingge.autojob.skeleton.model.task.TaskExecutable;
import com.jingge.autojob.util.convert.DateUtils;
import com.jingge.autojob.util.convert.DefaultValueUtil;
import com.jingge.autojob.util.id.IdGenerator;
import com.jingge.autojob.util.thread.SyncHelper;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 基于函数的任务
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-08-24 15:35
 * @email 1158055613@qq.com
 */
public class FunctionTask extends AutoJobTask {
    private final Function function;
    private boolean isAllFinished;
    private FunctionExecutable executable;

    /**
     * 创建一个FunctionTask
     *
     * @param function             函数接口
     * @param trigger              触发器，默认是一个延迟触发器，你可以使用{@link AutoJobTriggerFactory}来创建
     * @param retryConfig          重试配置
     * @param maximumExecutionTime 最长运行时间
     * @param unit                 单位
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/8/24 17:09
     */
    public FunctionTask(Function function, AutoJobTrigger trigger, AutoJobRetryConfig retryConfig, long maximumExecutionTime, TimeUnit unit) {
        id = IdGenerator.getNextIdAsLong();
        versionId = IdGenerator.getNextIdAsLong();
        alias = DateUtils
                .getTime()
                .replace(" ", "_") + "Function";
        this.function = function;
        this.trigger = DefaultValueUtil
                .defaultValue(trigger, AutoJobTriggerFactory.newDelayTrigger(AutoJobConstant.beforeSchedulingInTimeWheel, TimeUnit.SECONDS))
                .setTaskId(id);
        schedulingStrategy = SchedulingStrategy.JOIN_SCHEDULING;
        this.retryConfig = retryConfig.setTaskId(id);
        type = TaskType.MEMORY_TASk;
        taskLevel = 0;
        runningStatus.set(AutoJobRunningStatus.SCHEDULING);
        this.trigger.setMaximumExecutionTime(unit.toMillis(maximumExecutionTime));
    }

    /**
     * 创建一个默认的FunctionTask，不支持重试，使用延迟触发器，一定时间后只触发一次，最长允许执行24小时
     *
     * @param function 函数接口
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/8/24 17:11
     */
    public FunctionTask(Function function) {
        this(function, null, new AutoJobRetryConfig(false, RetryStrategy.LOCAL_RETRY, -1, -1L), 1, TimeUnit.DAYS);
    }

    /**
     * AutoJob自动调用，客户端不能手动调用
     *
     * @return void
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/8/24 17:12
     */
    public void allFinished() {
        isAllFinished = true;
    }

    public boolean isAllFinished() {
        return isAllFinished;
    }

    /**
     * 提交执行，该方法是异步的，任务将会在{@link AutoJobConstant#beforeSchedulingInTimeWheel}延迟后执行一次
     *
     * @return FunctionFuture
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/8/24 16:06
     */
    public FunctionFuture submit() {
        trigger.setTriggeringTime(System.currentTimeMillis() + AutoJobConstant.beforeSchedulingInTimeWheel);
        AutoJobApplication
                .getInstance()
                .getRegister()
                .registerTask(this, true, 0, TimeUnit.SECONDS);
        return new FunctionFuture(this);
    }

    /**
     * 提交执行，该方法是同步的，任务将会在{@link AutoJobConstant#beforeSchedulingInTimeWheel}延迟后执行一次
     *
     * @return AutoJobRunResult 任务结果
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/8/24 16:12
     */
    public AutoJobRunResult runSync() {
        FunctionFuture functionFuture = submit();
        return functionFuture.get();
    }

    /**
     * 获取任务的运行日志，日志可能会过一段时间后才能获取到
     *
     * @param wait 最长阻塞等待日志的时间
     * @param unit 时间单位
     * @return java.util.List<com.jingge.autojob.logging.domain.AutoJobLog>
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/8/24 16:19
     */
    public List<AutoJobLog> getLogs(long wait, TimeUnit unit) {
        if (trigger.getSchedulingRecordID() == null) {
            return Collections.emptyList();
        }
        SyncHelper.aWaitQuietly(() -> isAllFinished, wait, unit);
        return AutoJobApplication
                .getInstance()
                .getLogDbAPI()
                .findLogsBySchedulingId(trigger.getSchedulingRecordID());
    }

    /**
     * 获取任务的执行日志，日志可能会过一段时间后才能获取到
     *
     * @param wait 最长阻塞等待日志的时间
     * @param unit 时间单位
     * @return java.util.List<com.jingge.autojob.logging.domain.AutoJobRunLog>
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/8/24 16:20
     */
    public List<AutoJobRunLog> getRunLogs(long wait, TimeUnit unit) {
        if (trigger.getSchedulingRecordID() == null) {
            return Collections.emptyList();
        }
        SyncHelper.aWaitQuietly(() -> isAllFinished, wait, unit);
        return AutoJobApplication
                .getInstance()
                .getLogDbAPI()
                .findRunLogsBySchedulingId(trigger.getSchedulingRecordID());
    }


    @Override
    public TaskExecutable getExecutable() {
        if (executable != null) {
            return executable;
        }
        executable = new FunctionExecutable(this);
        return executable;
    }

    public Function getFunction() {
        return function;
    }
}
