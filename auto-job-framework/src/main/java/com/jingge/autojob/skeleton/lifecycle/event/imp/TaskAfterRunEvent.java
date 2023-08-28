package com.jingge.autojob.skeleton.lifecycle.event.imp;

import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lifecycle.event.TaskEvent;
import lombok.Getter;
import lombok.Setter;

/**
 * 任务运行完成事件
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/08 9:57
 */
@Getter
@Setter
public class TaskAfterRunEvent extends TaskEvent {
    /**
     * 结束时间
     */
    private long endTime;

    public TaskAfterRunEvent(AutoJobTask task) {
        super(task);
    }
}
