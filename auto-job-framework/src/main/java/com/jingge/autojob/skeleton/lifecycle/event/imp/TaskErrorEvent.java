package com.jingge.autojob.skeleton.lifecycle.event.imp;

import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lifecycle.event.TaskEvent;

/**
 * 任务最终执行失败时发布该事件，最终执行失败表示经过重试、转移等操作后依然失败
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/25 14:47
 */
public class TaskErrorEvent extends TaskEvent {
    public TaskErrorEvent(AutoJobTask task) {
        super(task);
        level = "ERROR";
    }
}
