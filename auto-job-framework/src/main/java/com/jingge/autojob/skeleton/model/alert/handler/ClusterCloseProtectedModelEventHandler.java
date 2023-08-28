package com.jingge.autojob.skeleton.model.alert.handler;

import com.jingge.autojob.skeleton.framework.config.AutoJobConfig;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.model.alert.AlertMail;
import com.jingge.autojob.skeleton.model.alert.AlertMailFactory;
import com.jingge.autojob.skeleton.model.alert.IAlertEventHandler;
import com.jingge.autojob.skeleton.model.alert.event.ClusterCloseProtectedModelEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description
 * @Author Huang Yongxiang
 * @Date 2022/07/29 10:34
 */
@Slf4j
public class ClusterCloseProtectedModelEventHandler implements IAlertEventHandler<ClusterCloseProtectedModelEvent> {
    @Override
    public void doHandle(ClusterCloseProtectedModelEvent event) {
        AutoJobConfig config = AutoJobApplication.getInstance().getConfigHolder().getAutoJobConfig();
        if (!config.getClusterCloseProtectedModeAlert()) {
            return;
        }
        AlertMail alertMail = AlertMailFactory.newClusterCloseProtectedModeAlertMail(event);
        if (alertMail != null) {
            if (alertMail.send()) {
                log.info("发送报警邮件成功");
            } else {
                log.error("发送报警邮件失败");
            }
        }
    }
}
