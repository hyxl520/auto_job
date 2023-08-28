package com.jingge.autojob.skeleton.framework.task;

/**
 * 任务运行结果
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/19 14:52
 */
public class AutoJobRunResult {
    /**
     * 完成时间
     */
    protected Long finishedTime;
    /**
     * 是否运行成功
     */
    protected Boolean isSuccess;
    /**
     * 是否运行失败
     */
    protected Boolean isError;
    /**
     * 任务结果
     */
    protected Object result;
    /**
     * 任务抛出的异常
     */
    protected Throwable throwable;

    public boolean hasResult() {
        return isSuccess != null && isError != null && finishedTime != null;
    }

    void reset() {
        finishedTime = null;
        isSuccess = null;
        isError = null;
        result = null;
        throwable = null;
    }

    public void success(Object result) {
        isSuccess = true;
        isError = false;
        this.result = result;
    }

    public void error(Throwable throwable, Object result) {
        isSuccess = false;
        isError = true;
        this.throwable = throwable;
        this.result = result;
    }

    public void finish() {
        finishedTime = System.currentTimeMillis();
    }

    public boolean isRunSuccess() {
        return isSuccess != null && isSuccess;
    }

    public Object getResult() {
        return result;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public Long getFinishedTime() {
        return finishedTime;
    }
}
