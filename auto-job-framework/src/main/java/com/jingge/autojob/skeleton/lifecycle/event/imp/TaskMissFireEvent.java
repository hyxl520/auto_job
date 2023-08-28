package com.jingge.autojob.skeleton.lifecycle.event.imp;

import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lifecycle.event.TaskEvent;
import lombok.Getter;
import lombok.Setter;

/**
 * 任务MissFire事件
 *
 * @author Huang Yongxiang
 * @date 2022-12-27 11:27
 * @email 1158055613@qq.com
 */
@Getter
@Setter
public class TaskMissFireEvent extends TaskEvent {
    /**
     * 理论触发时间
     */
    private long triggeringTime;

    public TaskMissFireEvent(AutoJobTask task) {
        super(AutoJobTask.deepCopyFrom(task));
        this.level = "WARN";
    }
}
