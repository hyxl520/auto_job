package com.jingge.autojob.skeleton.model.executor;

import com.jingge.autojob.skeleton.model.task.method.MethodTask;

/**
 * 任务运行环境类工厂，任务对应一个方法，方法存在于类中，该接口用于定义该类如何被创建，特别是类存在全局变量时，或者要使用Spring容器环境时，请考虑实现该接口
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/14 14:34
 */
public interface IMethodObjectFactory {
    Object createMethodObject(MethodTask task, Class<?> methodClass);
}
