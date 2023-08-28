package com.jingge.autojob.skeleton.model.alert.event;

import com.jingge.autojob.skeleton.framework.event.AutoJobEvent;
import com.jingge.autojob.skeleton.cluster.model.ClusterNode;
import com.jingge.autojob.skeleton.enumerate.AlertEventLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 报警事件
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/28 13:47
 */
@Getter
@Setter
@Accessors(chain = true)
public class AlertEvent extends AutoJobEvent {
    /**
     * 事件标题
     */
    protected String title;
    /**
     * 事件级别
     */
    protected AlertEventLevel type;
    /**
     * 事件内容
     */
    protected String content;
    /**
     * 报警机器
     */
    protected ClusterNode node;

    public AlertEvent(String title, AlertEventLevel type, String content) {
        super(new Date());
        this.title = title;
        this.type = type;
        this.content = content;
    }
}
