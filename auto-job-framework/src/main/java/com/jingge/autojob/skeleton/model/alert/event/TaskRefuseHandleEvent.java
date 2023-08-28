package com.jingge.autojob.skeleton.model.alert.event;

import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.enumerate.AlertEventLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * 任务拒绝执行报警事件
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/29 17:43
 */
@Getter
@Setter
public class TaskRefuseHandleEvent extends AlertEvent {
    public TaskRefuseHandleEvent(String title, String content) {
        super(title, AlertEventLevel.WARN, content);
    }
    private AutoJobTask refusedTask;
}
