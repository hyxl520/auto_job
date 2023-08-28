package com.jingge.autojob.skeleton.framework.event;

import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 抽象事件处理器委派者
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/28 14:08
 */
@Slf4j
public abstract class AbstractEventHandlerDelegate<E extends AutoJobEvent> {
    protected final Map<Class<? extends AutoJobEvent>, List<Object>> handlerContainer;
    protected List<Object> parentHandler;

    public AbstractEventHandlerDelegate(Map<Class<? extends AutoJobEvent>, List<Object>> handlerContainer) {
        this.handlerContainer = handlerContainer;
        this.parentHandler = new LinkedList<>();
    }

    /**
     * 以构造者的方式添加一个事件处理器
     *
     * @param eventClass 要处理的事件类型
     * @param handler    对应的处理器，处理器不应该具有状态，如果需要有状态请使用Spring管理
     * @return com.example.autojob.skeleton.framework.event.AbstractEventHandlerDelegate<E>
     * @author Huang Yongxiang
     * @date 2022/7/29 17:51
     */
    public AbstractEventHandlerDelegate<E> addHandler(Class<? extends AutoJobEvent> eventClass, IEventHandler<? extends AutoJobEvent> handler) {
        if (eventClass == null || handler == null) {
            return this;
        }
        if (isParentEvent(eventClass)) {
            parentHandler.add(handler);
            log.debug("添加父事件：{}的处理器完成", eventClass.getName());
            return this;
        }
        if (!handlerContainer.containsKey(eventClass)) {
            handlerContainer.put(eventClass, new LinkedList<>());
        }
        handlerContainer.get(eventClass).add(handler);
        log.debug("添加事件'{}'的处理器完成", eventClass.getSimpleName());
        return this;
    }


    /**
     * 查找指定的处理器处理该事件，该方法默认会执行父处理器
     *
     * @param event 被处理的事件
     * @return void
     * @author Huang Yongxiang
     * @date 2022/7/29 17:49
     */
    public void doHandle(E event) {
        doHandle(event, true);
    }


    /**
     * 查找指定的处理器处理该事件
     *
     * @param event           要处理的事件
     * @param isAllowBubbling 是否允许执行父处理器
     * @return void
     * @author Huang Yongxiang
     * @date 2022/7/29 17:49
     */
    @SuppressWarnings("unchecked")
    public void doHandle(E event, boolean isAllowBubbling) {
        if (isAllowBubbling) {
            parentHandler.forEach(item -> {
                IEventHandler<E> handler = (IEventHandler<E>) item;
                handler.doHandle(event);
            });
        }
        if (handlerContainer.containsKey(event.getClass())) {
            handlerContainer.get(event.getClass()).forEach(item -> {
                IEventHandler<E> handler = (IEventHandler<E>) item;
                handler.doHandle(event);
            });
        }
    }


    /**
     * 判断一个事件是否是父事件，交由子类实现
     *
     * @param eventClass 事件类型
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/7/29 17:50
     */
    public abstract boolean isParentEvent(Class<? extends AutoJobEvent> eventClass);
}
