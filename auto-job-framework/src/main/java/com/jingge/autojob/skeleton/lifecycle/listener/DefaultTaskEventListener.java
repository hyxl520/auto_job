package com.jingge.autojob.skeleton.lifecycle.listener;

import com.jingge.autojob.skeleton.lifecycle.TaskEventHandlerDelegate;
import com.jingge.autojob.skeleton.lifecycle.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * 任务事件的默认监听器
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/05 14:01
 */
@Slf4j
public class DefaultTaskEventListener implements TaskListener<TaskEvent> {
    @Override
    public void onTaskEvent(TaskEvent taskEvent) {
        //log.debug("任务：{}.{}，事件：{}", taskEvent.getTask().getMethodClassName(), taskEvent.getTask().getMethodName(), taskEvent.getMessage());
        TaskEventHandlerDelegate
                .getInstance()
                .doHandle(taskEvent, true);
    }
}
