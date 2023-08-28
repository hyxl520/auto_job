package com.jingge.autojob.skeleton.framework.pool;

/**
 * 拒绝处理器
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/02 14:13
 */
public interface IRefuseHandler {
    void doHandle(Executable executable, RunnablePostProcessor runnablePostProcessor, AbstractAutoJobPool pool);
}
