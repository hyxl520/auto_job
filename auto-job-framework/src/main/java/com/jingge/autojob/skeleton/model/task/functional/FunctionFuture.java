package com.jingge.autojob.skeleton.model.task.functional;

import com.jingge.autojob.skeleton.framework.task.AutoJobContext;
import com.jingge.autojob.skeleton.framework.task.AutoJobRunResult;
import com.jingge.autojob.util.thread.SyncHelper;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 反馈一个FunctionTask是否执行完
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-08-24 16:27
 * @email 1158055613@qq.com
 */
public class FunctionFuture implements Future<AutoJobRunResult> {
    private final FunctionTask functionTask;

    public FunctionFuture(FunctionTask functionTask) {
        if (functionTask == null || !functionTask.isExecutable()) {
            throw new IllegalArgumentException();
        }
        this.functionTask = functionTask;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (!mayInterruptIfRunning) {
            return true;
        }
        Long scheduleRecordID = functionTask
                .getTrigger()
                .getSchedulingRecordID();
        if (scheduleRecordID == null) {
            return false;
        }
        return AutoJobContext.stopRunningTask(scheduleRecordID);
    }

    @Override
    public boolean isCancelled() {
        return functionTask.isAllFinished();
    }

    @Override
    public boolean isDone() {
        return functionTask.isAllFinished();
    }

    @Override
    public AutoJobRunResult get() {
        SyncHelper.aWaitQuietly(this::isDone);
        return functionTask.getRunResult();
    }

    @Override
    public AutoJobRunResult get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        SyncHelper.aWaitQuietly(this::isDone, timeout, unit);
        return functionTask.getRunResult();
    }
}
