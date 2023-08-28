package com.jingge.autojob.skeleton.lifecycle.event.imp;

import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lifecycle.event.TaskEvent;

/**
 * @Description 任务注册后的事件
 * @Auther Huang Yongxiang
 * @Date 2021/12/15 16:07
 */
public class TaskRegisteredEvent extends TaskEvent {
    public TaskRegisteredEvent(AutoJobTask task) {
        super(task);
        level="INFO";
    }
}
