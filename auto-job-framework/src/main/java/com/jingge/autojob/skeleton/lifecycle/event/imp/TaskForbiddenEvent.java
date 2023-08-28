package com.jingge.autojob.skeleton.lifecycle.event.imp;

import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lifecycle.event.TaskEvent;

/**
 * 禁止的任务事件
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/11 17:41
 */
public class TaskForbiddenEvent extends TaskEvent {
    public TaskForbiddenEvent(AutoJobTask task) {
        super(task);
        level = "ERROR";
    }
}
