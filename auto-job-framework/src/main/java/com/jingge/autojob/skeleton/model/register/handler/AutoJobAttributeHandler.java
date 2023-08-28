package com.jingge.autojob.skeleton.model.register.handler;

import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.model.interpreter.AutoJobAttributeContext;
import com.jingge.autojob.skeleton.model.register.AbstractRegisterHandler;
import com.jingge.autojob.util.convert.StringUtils;

/**
 * 参数处理器
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/06 17:40
 */
public class AutoJobAttributeHandler extends AbstractRegisterHandler {
    @Override
    public void doHandle(AutoJobTask task) {
        if (task != null && task.getParams() == null && !StringUtils.isEmpty(task.getParamsString())) {
            AutoJobAttributeContext context = new AutoJobAttributeContext(task);
            task.setParams(context.getAttributeEntity());
        }
        if (chain != null) {
            chain.doHandle(task);
        }
    }
}
