package com.jingge.autojob.skeleton.lifecycle.listener;

import com.jingge.autojob.skeleton.lifecycle.event.TaskEvent;

import java.util.EventListener;

/**
 * @Description 任务的监听类
 * @Auther Huang Yongxiang
 * @Date 2021/12/15 15:06
 */
public interface TaskListener<E extends TaskEvent> extends EventListener {
    void onTaskEvent(E taskEvent);
}
