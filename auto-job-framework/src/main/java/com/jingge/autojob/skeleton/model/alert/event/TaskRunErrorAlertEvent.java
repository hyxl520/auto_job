package com.jingge.autojob.skeleton.model.alert.event;

import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.enumerate.AlertEventLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description 任务运行出错
 * @Author Huang Yongxiang
 * @Date 2022/07/28 13:59
 */
@Getter
@Setter
public class TaskRunErrorAlertEvent extends AlertEvent {
    public TaskRunErrorAlertEvent(String title, String content, AutoJobTask errorTask) {
        super(title, AlertEventLevel.WARN, content);
        this.errorTask = errorTask;
    }
    private AutoJobTask errorTask;
    private String stackTrace;
}
