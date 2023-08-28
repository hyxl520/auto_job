package com.jingge.autojob.skeleton.model.alert.event;

import com.jingge.autojob.skeleton.enumerate.AlertEventLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description
 * @Author Huang Yongxiang
 * @Date 2022/07/28 14:06
 */
@Getter
@Setter
public class ClusterOpenProtectedModelAlertEvent extends AlertEvent {
    public ClusterOpenProtectedModelAlertEvent(String title, String content) {
        super(title, AlertEventLevel.SERIOUS_WARN, content);
    }
}
