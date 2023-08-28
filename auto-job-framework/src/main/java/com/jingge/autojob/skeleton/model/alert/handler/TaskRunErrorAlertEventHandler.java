package com.jingge.autojob.skeleton.model.alert.handler;

import com.jingge.autojob.skeleton.framework.config.AutoJobConfig;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.model.alert.AlertMail;
import com.jingge.autojob.skeleton.model.alert.AlertMailFactory;
import com.jingge.autojob.skeleton.model.alert.IAlertEventHandler;
import com.jingge.autojob.skeleton.model.alert.event.TaskRunErrorAlertEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * 任务运行出错报警
 * @Author Huang Yongxiang
 * @Date 2022/07/28 15:48
 */
@Slf4j
public class TaskRunErrorAlertEventHandler implements IAlertEventHandler<TaskRunErrorAlertEvent> {
    @Override
    public void doHandle(TaskRunErrorAlertEvent event) {
        AutoJobConfig config = AutoJobApplication.getInstance().getConfigHolder().getAutoJobConfig();
        if (!config.getTaskRunErrorAlert()) {
            return;
        }
        AlertMail alertMail = AlertMailFactory.newRunErrorAlertMail(event);
        if (alertMail != null) {
            if (alertMail.send()) {
                log.info("发送报警邮件成功");
            } else {
                log.error("发送报警邮件失败");
            }
        }
    }
}
