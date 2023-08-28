package com.jingge.autojob.skeleton.framework.event;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 事件超类
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/28 14:22
 */
@Getter
@Setter
public class AutoJobEvent {
    protected Date publishTime;

    public AutoJobEvent(Date publishTime) {
        this.publishTime = publishTime;
    }

    public AutoJobEvent() {
        this.publishTime = new Date();
    }
}
