package com.jingge.autojob.skeleton.model.alert.handler;

import com.jingge.autojob.skeleton.framework.config.AutoJobConfig;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.model.alert.AlertMail;
import com.jingge.autojob.skeleton.model.alert.AlertMailFactory;
import com.jingge.autojob.skeleton.model.alert.IAlertEventHandler;
import com.jingge.autojob.skeleton.model.alert.event.ClusterOpenProtectedModelAlertEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author Huang Yongxiang
 * @Date 2022/07/29 9:05
 */
@Slf4j
public class ClusterOpenProtectedModelEventHandler implements IAlertEventHandler<ClusterOpenProtectedModelAlertEvent> {

    @Override
    public void doHandle(ClusterOpenProtectedModelAlertEvent event) {
        AutoJobConfig config = AutoJobApplication
                .getInstance()
                .getConfigHolder()
                .getAutoJobConfig();
        if (!config.getClusterOpenProtectedModeAlert()) {
            return;
        }
        AlertMail alertMail = AlertMailFactory.newClusterOpenProtectedModelAlertMail(event);
        if (alertMail != null) {
            if (alertMail.send()) {
                log.info("发送报警邮件成功");
            } else {
                log.error("发送报警邮件失败");
            }
        }
    }
}
