package com.jingge.autojob.skeleton.framework.task;

import java.lang.reflect.Method;

/**
 * 包装器，将方法包装成任务对象
 *
 * @author Huang Yongxiang
 * @date 2022-12-03 17:23
 * @email 1158055613@qq.com
 */
public interface AutoJobWrapper {
    AutoJobTask wrapper(Method method, Class<?> clazz);
}
