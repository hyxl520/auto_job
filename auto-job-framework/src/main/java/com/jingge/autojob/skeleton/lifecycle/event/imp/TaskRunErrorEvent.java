package com.jingge.autojob.skeleton.lifecycle.event.imp;

import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lifecycle.event.TaskEvent;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description 任务运行失败
 * @Auther Huang Yongxiang
 * @Date 2021/12/15 17:31
 */
@Setter
@Getter
public class TaskRunErrorEvent extends TaskEvent {
    private String errorStack;
    public TaskRunErrorEvent(AutoJobTask task) {
        super(task);
        level="ERROR";
    }
}
