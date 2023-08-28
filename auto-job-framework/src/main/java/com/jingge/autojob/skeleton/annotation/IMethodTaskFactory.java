package com.jingge.autojob.skeleton.annotation;

import com.jingge.autojob.skeleton.framework.config.AutoJobConfigHolder;
import com.jingge.autojob.skeleton.model.task.method.MethodTask;

import java.lang.reflect.Method;

/**
 * 任务工厂
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/18 12:53
 */
public interface IMethodTaskFactory {
    MethodTask newTask(AutoJobConfigHolder configHolder, Method method);
}
