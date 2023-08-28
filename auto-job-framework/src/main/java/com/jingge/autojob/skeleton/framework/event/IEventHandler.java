package com.jingge.autojob.skeleton.framework.event;

/**
 * 事件处理器的公共接口
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/28 14:13
 */
public interface IEventHandler<E extends AutoJobEvent> {
    void doHandle(E event);
}
