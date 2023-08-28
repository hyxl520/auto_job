package com.jingge.autojob.skeleton.model.alert.handler;

import com.jingge.autojob.skeleton.framework.config.AutoJobConfig;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.model.alert.AlertMail;
import com.jingge.autojob.skeleton.model.alert.AlertMailFactory;
import com.jingge.autojob.skeleton.model.alert.IAlertEventHandler;
import com.jingge.autojob.skeleton.model.alert.event.TaskRefuseHandleEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description
 * @Author Huang Yongxiang
 * @Date 2022/07/29 17:46
 */
@Slf4j
public class TaskRefuseHandleEventHandler implements IAlertEventHandler<TaskRefuseHandleEvent> {
    @Override
    public void doHandle(TaskRefuseHandleEvent event) {
        AutoJobConfig config = AutoJobApplication.getInstance().getConfigHolder().getAutoJobConfig();
        if (!config.getTaskRefuseHandleAlert()) {
            return;
        }
        AlertMail alertMail = AlertMailFactory.newTaskRefuseHandleAlertMail(event);
        if (alertMail != null) {
            if (alertMail.send()) {
                log.info("发送报警邮件成功");
            } else {
                log.error("发送报警邮件失败");
            }
        }
    }
}
