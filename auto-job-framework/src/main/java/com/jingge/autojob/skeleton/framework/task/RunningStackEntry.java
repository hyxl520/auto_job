package com.jingge.autojob.skeleton.framework.task;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 运行栈帧，注意不是线程安全的
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-03-21 14:30
 * @email 1158055613@qq.com
 */
public class RunningStackEntry implements Serializable {
    /**
     * 任务运行的某些自定义参数
     */
    private final Map<String, Object> params = new HashMap<>();

    /**
     * 调度记录ID
     */
    long schedulingRecordID;

    /**
     * 运行参数
     */
    Object[] args;

    /**
     * 是否执行成功
     */
    boolean isSuccess = false;

    /**
     * 触发时间
     */
    long triggeringTime;

    /**
     * 执行时长
     */
    long executionTime;

    /**
     * 异常
     */
    private Throwable exception;

    /**
     * 总分片
     */
    private Object totalSharding;

    /**
     * 当前分片
     */
    private Object currentSharding;

    RunningStackEntry recordStart(AutoJobTask task) {
        schedulingRecordID = task
                .getTrigger()
                .getSchedulingRecordID();
        args = task.getParams();
        triggeringTime = task
                .getTrigger()
                .getTriggeringTime();
        if (task.isEnableSharding()) {
            totalSharding = task.shardingConfig.getTotal();
            currentSharding = task.shardingConfig.getCurrent();
        }
        return this;
    }

    RunningStackEntry recordResult(AutoJobTask task) {
        if (task.getRunResult() != null) {
            isSuccess = task
                    .getRunResult()
                    .isRunSuccess();
            exception = task.getRunResult().throwable;
        }
        executionTime = task
                .getTrigger()
                .getLastRunTime();
        return this;
    }

    public RunningStackEntry addParam(String name, Object value) {
        params.put(name, value);
        return this;
    }

    public RunningStackEntry addAll(Map<String, Object> params) {
        this.params.putAll(params);
        return this;
    }

    public Object getParam(String name) {
        return params.get(name);
    }

    public long getSchedulingRecordID() {
        return schedulingRecordID;
    }

    public Object[] getArgs() {
        return args;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public long getTriggeringTime() {
        return triggeringTime;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public Throwable getException() {
        return exception;
    }

    public Object getTotalSharding() {
        return totalSharding;
    }

    public Object getCurrentSharding() {
        return currentSharding;
    }
}
