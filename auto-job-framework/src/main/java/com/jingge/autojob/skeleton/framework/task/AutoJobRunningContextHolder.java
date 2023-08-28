package com.jingge.autojob.skeleton.framework.task;

/**
 * 任务上下文持有者
 *
 * @author Huang Yongxiang
 * @date 2023-01-11 9:47
 * @email 1158055613@qq.com
 */
public final class AutoJobRunningContextHolder {
    protected static final InheritableThreadLocal<AutoJobRunningContext> contextHolder = new InheritableThreadLocal<>();

    /**
     * 获取与当前线程绑定的任务执行上下文
     *
     * @return com.example.autojob.skeleton.framework.task.AutoJobTaskContext
     * @author Huang Yongxiang
     * @date 2023/1/11 11:43
     */
    public static AutoJobRunningContext currentTaskContext() {
        return contextHolder.get();
    }
}
