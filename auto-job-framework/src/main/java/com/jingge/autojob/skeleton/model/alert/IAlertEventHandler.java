package com.jingge.autojob.skeleton.model.alert;

import com.jingge.autojob.skeleton.framework.event.IEventHandler;
import com.jingge.autojob.skeleton.model.alert.event.AlertEvent;

/**
 * @Description 警报事件处理器
 * @Author Huang Yongxiang
 * @Date 2022/07/28 14:07
 */
public interface IAlertEventHandler<E extends AlertEvent> extends IEventHandler<E> {
}
