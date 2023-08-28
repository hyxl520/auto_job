package com.jingge.autojob.skeleton.model.handler;

import com.jingge.autojob.skeleton.annotation.FactoryAutoJob;
import com.jingge.autojob.skeleton.annotation.IMethodTaskFactory;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.framework.task.AutoJobWrapper;
import com.jingge.autojob.util.bean.ObjectUtil;

import java.lang.reflect.Method;

/**
 * FactoryAutoJob注解包装器
 *
 * @author Huang Yongxiang
 * @date 2022-12-03 18:06
 * @email 1158055613@qq.com
 */
public class FactoryAutoJobAnnotationWrapper implements AutoJobWrapper {
    @Override
    public AutoJobTask wrapper(Method method, Class<?> clazz) {
        FactoryAutoJob factoryAutoJob = method.getAnnotation(FactoryAutoJob.class);
        if (factoryAutoJob == null) {
            return null;
        }
        IMethodTaskFactory methodTaskFactory = ObjectUtil.getClassInstance(factoryAutoJob.value());
        return methodTaskFactory.newTask(AutoJobApplication
                .getInstance()
                .getConfigHolder(), method);
    }

}
