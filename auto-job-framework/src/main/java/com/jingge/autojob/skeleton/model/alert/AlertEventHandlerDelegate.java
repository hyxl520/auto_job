package com.jingge.autojob.skeleton.model.alert;

import com.jingge.autojob.skeleton.framework.event.AbstractEventHandlerDelegate;
import com.jingge.autojob.skeleton.framework.event.AutoJobEvent;
import com.jingge.autojob.skeleton.model.alert.event.AlertEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 警报事件委派者
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/28 15:17
 */
public class AlertEventHandlerDelegate extends AbstractEventHandlerDelegate<AlertEvent> {
    private AlertEventHandlerDelegate(Map<Class<? extends AutoJobEvent>, List<Object>> handlerContainer) {
        super(handlerContainer);
    }

    public static AlertEventHandlerDelegate getInstance() {
        return InstanceHolder.handlerDelegate;
    }

    @Override
    public boolean isParentEvent(Class<? extends AutoJobEvent> eventClass) {
        return eventClass == AlertEvent.class;
    }

    private static class InstanceHolder {
        private static final AlertEventHandlerDelegate handlerDelegate = new AlertEventHandlerDelegate(new ConcurrentHashMap<>());
    }
}
