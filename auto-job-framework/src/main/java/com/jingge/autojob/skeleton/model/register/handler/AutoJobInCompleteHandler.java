package com.jingge.autojob.skeleton.model.register.handler;

import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.model.register.AbstractRegisterHandler;
import com.jingge.autojob.util.id.IdGenerator;

/**
 * @Author Huang Yongxiang
 * @Date 2022/07/13 13:55
 */
public class AutoJobInCompleteHandler extends AbstractRegisterHandler {
    @Override
    public void doHandle(AutoJobTask task) {
        if (task != null) {
            if (task.getId() == null) {
                task.setId(IdGenerator.getNextIdAsLong());
            }
            if (task.getType() == null) {
                task.setType(AutoJobTask.TaskType.MEMORY_TASk);
            }
            if (task.getIsFinished() == null) {
                task.setIsFinished(false);
            }
            if (task
                    .getTrigger()
                    .getTriggeringTime() == null && (task.getIsChildTask() == null || !task.getIsChildTask())) {
                task
                        .getTrigger()
                        .refresh();
            }
        }
        if (chain != null) {
            chain.doHandle(task);
        }
    }
}
