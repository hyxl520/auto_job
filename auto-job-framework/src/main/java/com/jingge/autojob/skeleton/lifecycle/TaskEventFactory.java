package com.jingge.autojob.skeleton.lifecycle;

import com.jingge.autojob.skeleton.cluster.model.ClusterNode;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lang.IAutoJobFactory;
import com.jingge.autojob.skeleton.lifecycle.event.TaskEvent;
import com.jingge.autojob.skeleton.lifecycle.event.imp.*;
import com.jingge.autojob.skeleton.lifecycle.event.imp.*;
import com.jingge.autojob.util.convert.DefaultValueUtil;
import com.jingge.autojob.util.servlet.InetUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * 任务事件工厂
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/12 9:58
 */
public class TaskEventFactory implements IAutoJobFactory {
    private static final String localhostIp = InetUtil.getTCPAddress();

    public static TaskEvent newTaskEvent(AutoJobTask task, String message) {
        return new TaskEvent(task).setMessage(message);
    }

    public static TaskBeforeRegisterEvent newBeforeRegisterEvent(AutoJobTask task) {
        TaskBeforeRegisterEvent taskBeforeRegisterEvent = new TaskBeforeRegisterEvent(task);
        taskBeforeRegisterEvent.setMessage(String.format("任务：%d准备注册", task.getId()));
        return taskBeforeRegisterEvent;
    }

    public static TaskRegisteredEvent newRegisteredEvent(AutoJobTask task) {
        TaskRegisteredEvent registeredEvent = new TaskRegisteredEvent(task);
        registeredEvent.setMessage(String.format("任务：%d注册完成", task.getId()));
        return registeredEvent;
    }

    public static TaskBeforeRunEvent newBeforeRunEvent(AutoJobTask task) {
        TaskBeforeRunEvent taskBeforeRunEvent = new TaskBeforeRunEvent(task);
        taskBeforeRunEvent.setMessage(String.format("任务：%d准备在机器%s启动运行", task.getId(), localhostIp));
        taskBeforeRunEvent.setStartTime(System.currentTimeMillis());
        return taskBeforeRunEvent;
    }

    public static TaskAfterRunEvent newAfterRunEvent(AutoJobTask task) {
        TaskAfterRunEvent afterRunEvent = new TaskAfterRunEvent(task);
        afterRunEvent.setMessage(String.format("任务：%d在机器%s运行完成", task.getId(), localhostIp));
        afterRunEvent.setEndTime(System.currentTimeMillis());
        return afterRunEvent;
    }

    public static TaskRunSuccessEvent newSuccessEvent(AutoJobTask task) {
        TaskRunSuccessEvent successEvent = new TaskRunSuccessEvent(task);
        successEvent.setMessage(String.format("任务：%d执行成功", task.getId()));
        successEvent.setTriggeringTime(task
                .getTrigger()
                .getTriggeringTime());
        return successEvent;
    }

    public static TaskRunErrorEvent newRunErrorEvent(AutoJobTask task, Throwable throwable) {
        TaskRunErrorEvent errorEvent = new TaskRunErrorEvent(task);
        String ex = "";
        if (throwable != null) {
            ex = DefaultValueUtil
                    .defaultValue(throwable.getCause(), throwable)
                    .toString();
        }
        errorEvent.setMessage(String.format("任务：%d在机器%s执行异常：%s", task.getId(), localhostIp, ex));
        if (throwable != null) {
            errorEvent.setErrorStack(ExceptionUtils.getStackTrace(throwable));
        }
        errorEvent.setLevel("ERROR");
        return errorEvent;
    }

    public static TaskFinishedEvent newFinishedEvent(AutoJobTask task) {
        TaskFinishedEvent finishedEvent = new TaskFinishedEvent(task);
        finishedEvent.setMessage(String.format("任务：%d执行完成", task.getId()));
        return finishedEvent;
    }

    public static TaskForbiddenEvent newForbiddenEvent(AutoJobTask task) {
        TaskForbiddenEvent forbiddenEvent = new TaskForbiddenEvent(task);
        forbiddenEvent.setMessage(String.format("任务：%d被禁止运行", task.getId()));
        forbiddenEvent.setLevel("ERROR");
        return forbiddenEvent;
    }


    public static TaskErrorEvent newErrorEvent(AutoJobTask task) {
        TaskErrorEvent errorEvent = new TaskErrorEvent(task);
        errorEvent.setMessage(String.format("任务：%d执行失败", task.getId()));
        return errorEvent;
    }

    public static TaskTransferEvent newTaskTransferEvent(AutoJobTask task, ClusterNode transferTo) {
        TaskTransferEvent event = new TaskTransferEvent();
        event.setTransferTo(transferTo);
        event.setTask(task);
        event.setMessage(String.format("任务：%d被转移到节点：%s运行", task.getId(), transferTo.toString()));
        return event;
    }

    public static TaskReceivedEvent newTaskReceivedEvent(AutoJobTask task, ClusterNode transferFrom) {
        TaskReceivedEvent event = new TaskReceivedEvent();
        event.setTask(task);
        event.setTransferFrom(transferFrom);
        event.setMessage(String.format("接收到来自节点：%s的转移任务：%d", transferFrom.toString(), task.getId()));
        return event;
    }

    public static TaskMissFireEvent newTaskMissFireEvent(AutoJobTask task) {
        TaskMissFireEvent event = new TaskMissFireEvent(task);
        event.setMessage(String.format("任务:%d miss fire", task.getId()));
        event.setTriggeringTime(task
                .getTrigger()
                .getTriggeringTime());
        return event;
    }

    public static TaskRetryEvent newTaskRetryEvent(AutoJobTask task, int retriedTimes, long retryTime, int maximumRetryCount) {
        TaskRetryEvent event = new TaskRetryEvent(task);
        event.setMessage(String.format("任务%d将在%dms后进行第%d/%d次重试", task.getId(), retryTime - System.currentTimeMillis(), retriedTimes, maximumRetryCount));
        event.setMaximumRetryCount(maximumRetryCount);
        event.setRetriedTimes(retriedTimes);
        event.setRetryTime(retryTime);
        return event;
    }


}
