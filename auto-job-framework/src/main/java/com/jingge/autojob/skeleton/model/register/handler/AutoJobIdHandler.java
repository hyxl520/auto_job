package com.jingge.autojob.skeleton.model.register.handler;

import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.model.register.AbstractRegisterHandler;
import com.jingge.autojob.util.id.IdGenerator;

/**
 * ID 处理器
 * @Author Huang Yongxiang
 * @Date 2022/07/06 17:55
 */
public class AutoJobIdHandler extends AbstractRegisterHandler {

    @Override
    public void doHandle(AutoJobTask task) {
        if (task != null && task.getId() == null) {
            task.setId(IdGenerator.getNextIdAsLong());
        }
        if (chain != null) {
            chain.doHandle(task);
        }
    }
}
