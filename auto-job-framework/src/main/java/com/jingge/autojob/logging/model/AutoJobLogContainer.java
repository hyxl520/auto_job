package com.jingge.autojob.logging.model;

import com.jingge.autojob.skeleton.framework.mq.MessageQueueContext;
import com.jingge.autojob.util.convert.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 日志容器
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/15 12:37
 */
public class AutoJobLogContainer {
    private final Map<String, MessageQueueContext<Object>> messageQueueContextMap;

    public AutoJobLogContainer() {
        this.messageQueueContextMap = new ConcurrentHashMap<>();
    }

    @SuppressWarnings("unchecked")
    public boolean addMessageQueueContext(String name, MessageQueueContext<?> messageQueueContext) {
        if (StringUtils.isEmpty(name) || messageQueueContext == null || messageQueueContextMap.containsKey(name)) {
            return false;
        }
        messageQueueContextMap.put(name, (MessageQueueContext<Object>) messageQueueContext);
        return true;
    }

    public boolean addMessageQueueContext(Class<?> logType, MessageQueueContext<?> messageQueueContext) {
        return addMessageQueueContext(logType.getName(), messageQueueContext);
    }

    @SuppressWarnings("unchecked")
    public <T> MessageQueueContext<T> getMessageQueueContext(String name, Class<T> type) {
        if (!messageQueueContextMap.containsKey(name)) {
            return null;
        }
        return (MessageQueueContext<T>) messageQueueContextMap.get(name);
    }

    public <T> MessageQueueContext<T> getMessageQueueContext(Class<T> type) {
        return getMessageQueueContext(type.getName(), type);
    }


    public static AutoJobLogContainer getInstance() {
        return InstanceHolder.CONTAINER;
    }

    private static class InstanceHolder {
        private static final AutoJobLogContainer CONTAINER = new AutoJobLogContainer();
    }


}
