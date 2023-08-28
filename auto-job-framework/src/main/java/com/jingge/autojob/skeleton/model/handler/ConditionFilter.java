package com.jingge.autojob.skeleton.model.handler;

import com.jingge.autojob.skeleton.annotation.Conditional;
import com.jingge.autojob.skeleton.annotation.IAutoJobCondition;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.config.AutoJobConfigHolder;
import com.jingge.autojob.util.bean.ObjectUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Huang Yongxiang
 * @date 2022-12-03 17:48
 * @email 1158055613@qq.com
 */
@Slf4j
public class ConditionFilter extends AbstractAnnotationFilter {
    @Override
    public void doFilter(Set<Method> scannedMethods) {
        for (Iterator<Method> iterator = scannedMethods.iterator(); iterator.hasNext(); ) {
            Method method = iterator.next();
            Conditional conditional = method.getAnnotation(Conditional.class);
            if (conditional == null) {
                continue;
            }
            Class<? extends IAutoJobCondition>[] classes = conditional.value();
            for (Class<? extends IAutoJobCondition> condition : classes) {
                if (!ObjectUtil
                        .getClassInstance(condition)
                        .matches(AutoJobApplication
                                .getInstance()
                                .getConfigHolder()
                                .getPropertiesHolder(), AutoJobApplication.getInstance())) {
                    if (AutoJobConfigHolder
                            .getInstance()
                            .isDebugEnable()) {
                        log.warn("任务：{}.{}将不会被注入", method
                                .getDeclaringClass()
                                .getName(), method.getName());
                    }
                    iterator.remove();
                }
            }
        }
        if (nextFilter != null) {
            nextFilter.doFilter(scannedMethods);
        }
    }
}
