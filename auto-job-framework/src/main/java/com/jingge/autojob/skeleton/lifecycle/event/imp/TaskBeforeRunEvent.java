package com.jingge.autojob.skeleton.lifecycle.event.imp;


import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lifecycle.event.TaskEvent;
import lombok.Getter;
import lombok.Setter;

/**
 * 任务执行前
 *
 * @Auther Huang Yongxiang
 * @Date 2021/12/15 17:18
 */
@Getter
@Setter
public class TaskBeforeRunEvent extends TaskEvent {
    private long startTime;

    public TaskBeforeRunEvent(AutoJobTask task) {
        super(task);
        level = "INFO";
    }
}
