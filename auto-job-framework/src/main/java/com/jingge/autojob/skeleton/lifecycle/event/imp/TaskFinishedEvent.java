package com.jingge.autojob.skeleton.lifecycle.event.imp;


import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lifecycle.event.TaskEvent;

/**
 * 任务完成后，包括运行时出错退出和成功完成
 *
 * @Auther Huang Yongxiang
 * @Date 2021/12/15 17:19
 */
public class TaskFinishedEvent extends TaskEvent {

    public TaskFinishedEvent(AutoJobTask task) {
        super(task);
        level = "INFO";
    }
}
