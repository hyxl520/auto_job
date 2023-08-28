package com.jingge.autojob.skeleton.lifecycle.event.imp;

import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lifecycle.event.TaskEvent;
import lombok.Getter;
import lombok.Setter;

/**
 * 任务运行完成且成功完成，反映的是单次调度的执行情况
 *
 * @Auther Huang Yongxiang
 * @Date 2021/12/15 17:20
 */
@Getter
@Setter
public class TaskRunSuccessEvent extends TaskEvent {
    /**
     * 本次调度的触发时间
     */
    private long triggeringTime;

    public TaskRunSuccessEvent(AutoJobTask task) {
        super(task);
        level = "INFO";
    }
}
