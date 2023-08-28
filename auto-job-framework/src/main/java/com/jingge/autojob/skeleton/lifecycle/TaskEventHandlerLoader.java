package com.jingge.autojob.skeleton.lifecycle;

import com.jingge.autojob.logging.model.AutoJobLogContext;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.processor.IAutoJobLoader;
import com.jingge.autojob.skeleton.framework.task.AutoJobRunningStackContainer;
import com.jingge.autojob.skeleton.lifecycle.event.TaskEvent;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskFinishedEvent;

/**
 * 任务事件处理器加载器
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/13 11:08
 */
public class TaskEventHandlerLoader implements IAutoJobLoader {
    @Override
    public void load() {
        TaskEventHandlerDelegate
                .getInstance()
                .addHandler(TaskEvent.class, AutoJobLogContext
                        .getInstance()
                        .getLogManager())
                .addHandler(TaskFinishedEvent.class, AutoJobRunningStackContainer.getInstance())
                .addHandler(TaskFinishedEvent.class, AutoJobApplication
                        .getInstance()
                        .getMethodObjectCache());
    }

}
