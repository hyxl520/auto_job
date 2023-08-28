package com.jingge.autojob.skeleton.framework.pool;

import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lifecycle.TaskEventFactory;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskMissFireEvent;
import com.jingge.autojob.skeleton.lifecycle.manager.TaskEventManager;
import com.jingge.autojob.skeleton.model.alert.AlertEventHandlerDelegate;
import com.jingge.autojob.skeleton.model.alert.event.AlertEventFactory;
import com.jingge.autojob.skeleton.model.alert.event.TaskRefuseHandleEvent;
import com.jingge.autojob.skeleton.model.task.TaskExecutable;

/**
 * 默认拒绝处理器
 *
 * @Author Huang Yongxiang
 * @Date 2022/10/17 11:20
 */
public class DefaultRefuseHandler implements IRefuseHandler {

    @Override
    public void doHandle(Executable executable, RunnablePostProcessor runnablePostProcessor, AbstractAutoJobPool pool) {
        if (executable instanceof TaskExecutable) {
            AutoJobTask task = ((TaskExecutable) executable).getAutoJobTask();
            TaskEventManager
                    .getInstance()
                    .publishTaskEventSync(TaskEventFactory.newTaskMissFireEvent(task), TaskMissFireEvent.class, true);
            TaskRefuseHandleEvent event = AlertEventFactory.newTaskRefuseHandleEvent(task);
            AlertEventHandlerDelegate
                    .getInstance()
                    .doHandle(event, true);
        }
    }
}
