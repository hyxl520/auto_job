package com.jingge.autojob.skeleton.lifecycle.event.imp;

import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lifecycle.event.TaskEvent;
import lombok.Getter;
import lombok.Setter;

/**
 * 任务重试事件
 *
 * @author Huang Yongxiang
 * @date 2022-12-30 14:14
 * @email 1158055613@qq.com
 */
@Getter
@Setter
public class TaskRetryEvent extends TaskEvent {
    /**
     * 重试时间
     */
    private long retryTime;
    /**
     * 已重试次数
     */
    private int retriedTimes;
    /**
     * 最大重试次数
     */
    private int maximumRetryCount;

    public TaskRetryEvent(AutoJobTask task) {
        super(task);
        level = "WARN";
    }
}
