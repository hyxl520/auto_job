package com.jingge.autojob.skeleton.lifecycle.event;

import com.jingge.autojob.skeleton.framework.event.AutoJobEvent;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lifecycle.TaskEventHandlerDelegate;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 任务事件基类，任务事件的处理器添加请参照TaskEventHandlerDelegate
 *
 * @Auther Huang Yongxiang
 * @Date 2021/12/15 14:48
 * @see TaskEventHandlerDelegate
 */
@Setter
@Getter
@Accessors(chain = true)
public class TaskEvent extends AutoJobEvent {
    protected AutoJobTask task;
    /**
     * 发布的消息
     */
    protected String message;
    /**
     * 级别，建议使用日志级别
     */
    protected String level;

    public TaskEvent(AutoJobTask task) {
        super(new Date());
        this.task = task;
        this.level = "INFO";
    }

    public TaskEvent() {
        super(new Date());
    }
}
