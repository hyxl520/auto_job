package com.jingge.autojob.logging.model.factory;

import com.jingge.autojob.logging.domain.AutoJobRunLog;
import com.jingge.autojob.skeleton.framework.task.AutoJobRunResult;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lang.IAutoJobFactory;
import com.jingge.autojob.skeleton.lifecycle.event.TaskEvent;
import com.jingge.autojob.skeleton.lifecycle.event.imp.*;
import com.jingge.autojob.skeleton.lifecycle.event.imp.*;
import com.jingge.autojob.util.convert.DateUtils;
import com.jingge.autojob.util.convert.DefaultValueUtil;
import com.jingge.autojob.util.id.IdGenerator;
import com.jingge.autojob.util.json.JsonUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 任务运行日志工厂
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/11 15:02
 */
@Slf4j
public class AutoJobRunLogFactory implements IAutoJobFactory {
    public static AutoJobRunLog getAutoJobRunLog(TaskEvent event) {
        if (event instanceof TaskBeforeRunEvent) {
            return getStartUpRunLog((TaskBeforeRunEvent) event);
        } else if (event instanceof TaskFinishedEvent) {
            return getFinishedRunLog((TaskFinishedEvent) event);
        } else if (event instanceof TaskRunErrorEvent) {
            return getErrorRunLog((TaskRunErrorEvent) event);
        } else if (event instanceof TaskRunSuccessEvent) {
            return getSuccessRunLog((TaskRunSuccessEvent) event);
        } else if (event instanceof TaskForbiddenEvent) {
            return getForbiddenRunLog((TaskForbiddenEvent) event);
        } else if (event instanceof TaskRetryEvent) {
            return getRetryRunLog((TaskRetryEvent) event);
        }
        return null;
    }

    public static AutoJobRunLog getStartUpRunLog(TaskBeforeRunEvent event) {
        return new AutoJobRunLog()
                .setId(IdGenerator.getNextIdAsLong())
                .setWriteTime(DateUtils.getTime())
                .setTaskType(event
                        .getTask()
                        .getType()
                        .toString())
                .setTaskId(getRunLogTaskId(event.getTask()))
                .setRunStatus(1)
                .setMessage(event.getMessage());
    }

    public static AutoJobRunLog getFinishedRunLog(TaskFinishedEvent event) {
        return new AutoJobRunLog()
                .setId(IdGenerator.getNextIdAsLong())
                .setWriteTime(DateUtils.getTime())
                .setTaskType(event
                        .getTask()
                        .getType()
                        .toString())
                .setTaskId(getRunLogTaskId(event.getTask()))
                .setRunStatus(1)
                .setMessage(event.getMessage());
    }

    public static AutoJobRunLog getSuccessRunLog(TaskRunSuccessEvent event) {
        AutoJobRunLog runLog = new AutoJobRunLog()
                .setId(IdGenerator.getNextIdAsLong())
                .setWriteTime(DateUtils.getTime())
                .setTaskType(event
                        .getTask()
                        .getType()
                        .toString())
                .setTaskId(getRunLogTaskId(event.getTask()))
                .setRunStatus(1)
                .setMessage(event.getMessage());
        AutoJobRunResult runResult = event
                .getTask()
                .getRunResult();
        if (runResult != null) {
            runLog.setRunResult(JsonUtil.pojoToJsonString(DefaultValueUtil.defaultObjectWhenNull(runResult.getResult(), "")));
        }
        return runLog;
    }

    public static AutoJobRunLog getErrorRunLog(TaskRunErrorEvent errorEvent) {
        return new AutoJobRunLog()
                .setId(IdGenerator.getNextIdAsLong())
                .setWriteTime(DateUtils.getTime())
                .setTaskType(errorEvent
                        .getTask()
                        .getType()
                        .toString())
                .setTaskId(getRunLogTaskId(errorEvent.getTask()))
                .setRunStatus(0)
                .setErrorStack(errorEvent.getErrorStack())
                .setMessage(errorEvent.getMessage());
    }

    public static AutoJobRunLog getForbiddenRunLog(TaskForbiddenEvent event) {
        return new AutoJobRunLog()
                .setId(IdGenerator.getNextIdAsLong())
                .setWriteTime(DateUtils.getTime())
                .setTaskType(event
                        .getTask()
                        .getType()
                        .toString())
                .setTaskId(getRunLogTaskId(event.getTask()))
                .setRunStatus(0)
                .setMessage(event.getMessage());
    }

    public static AutoJobRunLog getRetryRunLog(TaskRetryEvent event) {
        return new AutoJobRunLog()
                .setId(IdGenerator.getNextIdAsLong())
                .setId(IdGenerator.getNextIdAsLong())
                .setWriteTime(DateUtils.getTime())
                .setTaskType(event
                        .getTask()
                        .getType()
                        .toString())
                .setTaskId(getRunLogTaskId(event.getTask()))
                .setRunStatus(0)
                .setMessage(event.getMessage());
    }

    private static long getRunLogTaskId(AutoJobTask task) {
        return task.getId();
    }
}
