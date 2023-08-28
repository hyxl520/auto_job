package com.jingge.autojob.skeleton.model.handler;

import com.jingge.autojob.skeleton.framework.config.AutoJobConfig;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.util.convert.RegexUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Huang Yongxiang
 * @date 2022-12-03 17:54
 * @email 1158055613@qq.com
 */
@Slf4j
public class ClasspathFilter extends AbstractAnnotationFilter {
    @Override
    public void doFilter(Set<Method> scannedMethods) {
        AutoJobConfig config = AutoJobApplication
                .getInstance()
                .getConfigHolder()
                .getAutoJobConfig();
        for (Iterator<Method> iterator = scannedMethods.iterator(); iterator.hasNext(); ) {
            Method method = iterator.next();
            String className = method
                    .getDeclaringClass()
                    .getName();
            if (config
                    .getAnnotationClassPathPattern()
                    .stream()
                    .noneMatch(item -> RegexUtil.isMatch(className, RegexUtil.wildcardToRegexString(item)))) {
                log.warn("方法：{}.{}不在白名单内", method
                        .getDeclaringClass()
                        .getName(), method.getName());
                iterator.remove();
            }
        }
        if (nextFilter != null) {
            nextFilter.doFilter(scannedMethods);
        }
    }
}

