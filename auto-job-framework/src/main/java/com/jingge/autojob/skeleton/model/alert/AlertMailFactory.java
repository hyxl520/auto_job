package com.jingge.autojob.skeleton.model.alert;

import com.jingge.autojob.skeleton.db.entity.AutoJobConfigEntity;
import com.jingge.autojob.skeleton.db.entity.EntityConvertor;
import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.config.AutoJobMailConfig;
import com.jingge.autojob.skeleton.framework.config.ConfigJsonSerializerAndDeserializer;
import com.jingge.autojob.skeleton.framework.mail.IMailClient;
import com.jingge.autojob.skeleton.framework.mail.MailClientFactory;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lang.IAutoJobFactory;
import com.jingge.autojob.skeleton.enumerate.AlertEventLevel;
import com.jingge.autojob.skeleton.model.alert.event.ClusterCloseProtectedModelEvent;
import com.jingge.autojob.skeleton.model.alert.event.ClusterOpenProtectedModelAlertEvent;
import com.jingge.autojob.skeleton.model.alert.event.TaskRefuseHandleEvent;
import com.jingge.autojob.skeleton.model.alert.event.TaskRunErrorAlertEvent;
import com.jingge.autojob.util.convert.DateUtils;

/**
 * 警告邮件工厂类
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/28 16:44
 */
public class AlertMailFactory implements IAutoJobFactory {
    private static IMailClient getMailClient(AutoJobTask task) {
        if (task == null) {
            return AutoJobApplication
                    .getInstance()
                    .getMailClient();
        }
        if (task.getMailClient() != null) {
            return task.getMailClient();
        }
        if (task.getType() == AutoJobTask.TaskType.DB_TASK) {
            AutoJobMailConfig mailConfig = null;
            if (task.getMailConfig() == null) {
                AutoJobConfigEntity configEntity = AutoJobMapperHolder.CONFIG_ENTITY_MAPPER.selectByTaskIdAndType(AutoJobMailConfig.class.getName(), task.getId());
                mailConfig = (AutoJobMailConfig) EntityConvertor.entity2StorableConfig(configEntity, new ConfigJsonSerializerAndDeserializer());
            } else {
                mailConfig = task.getMailConfig();
            }
            return MailClientFactory.createMailClient(mailConfig);
        }
        return AutoJobApplication
                .getInstance()
                .getMailClient();
    }

    public static AlertMail newRunErrorAlertMail(TaskRunErrorAlertEvent event) {
        AlertMailBuilder builder = AlertMailBuilder.newInstance();
        AutoJobTask errorTask = event.getErrorTask();
        return builder
                .setMailClient(getMailClient(errorTask))
                .setTitle(event.getTitle())
                .setLevel(AlertEventLevel.WARN)
                .addContentTitle(String.format("任务：\"%d:%s\"执行失败", errorTask.getId(), errorTask.getAlias()), 1)
                .addBr()
                .addBold("报警时间：" + DateUtils.formatDateTime(event.getPublishTime()))
                .addBr()
                .addBold(String.format("报警机器：%s:%s", event
                        .getNode()
                        .getHost(), event
                        .getNode()
                        .getPort()))
                .addBr()
                .addBold("任务路径：" + errorTask.getReference())
                .addBr()
                .addParagraph("堆栈信息如下：")
                .addParagraph(event
                        .getStackTrace()
                        .replace("\n", "</br>"))
                .addError("请及时处理")
                .addBr()
                .addItalics("[该邮件为系统自动发送，请不要回复此邮件]")
                .getAlertMail();
    }

    public static AlertMail newClusterOpenProtectedModelAlertMail(ClusterOpenProtectedModelAlertEvent event) {
        AlertMailBuilder builder = AlertMailBuilder.newInstance();
        return builder
                .setMailClient(getMailClient(null))
                .setTitle(event.getTitle())
                .setLevel(AlertEventLevel.SERIOUS_WARN)
                .addContentTitle(String.format("节点：%s:%s启动保护模式", event
                        .getNode()
                        .getHost(), event
                        .getNode()
                        .getPort()), 1)
                .addBr()
                .addBold("报警时间：" + DateUtils.formatDateTime(event.getPublishTime()))
                .addBr()
                .addBold(String.format("报警机器：%s:%s", event
                        .getNode()
                        .getHost(), event
                        .getNode()
                        .getPort()))
                .addError("请检查集群节点情况")
                .addBr()
                .addItalics("[该邮件为系统自动发送，请不要回复此邮件]")
                .getAlertMail();
    }

    public static AlertMail newClusterCloseProtectedModeAlertMail(ClusterCloseProtectedModelEvent event) {
        AlertMailBuilder builder = AlertMailBuilder.newInstance();
        return builder
                .setMailClient(getMailClient(null))
                .setTitle(event.getTitle())
                .setLevel(AlertEventLevel.INFO)
                .addContentTitle(String.format("节点：%s:%s关闭保护模式", event
                        .getNode()
                        .getHost(), event
                        .getNode()
                        .getPort()), 1)
                .addBr()
                .addBold("报警时间：" + DateUtils.formatDateTime(event.getPublishTime()))
                .addBr()
                .addBold(String.format("报警机器：%s:%s", event
                        .getNode()
                        .getHost(), event
                        .getNode()
                        .getPort()))
                .addError("请检查集群节点情况")
                .addBr()
                .addItalics("[该邮件为系统自动发送，请不要回复此邮件]")
                .getAlertMail();
    }

    public static AlertMail newTaskRefuseHandleAlertMail(TaskRefuseHandleEvent event) {
        AlertMailBuilder builder = AlertMailBuilder.newInstance();
        AutoJobTask refusedTask = event.getRefusedTask();
        String refusedContent = !refusedTask.getIsAllowRegister() ? "过滤器拒绝执行" : "资源过载";
        return builder
                .setMailClient(getMailClient(refusedTask))
                .setTitle(event.getTitle())
                .setLevel(AlertEventLevel.WARN)
                .addContentTitle(String.format("任务：\"%d:%s\"被拒绝执行", refusedTask.getId(), refusedTask.getAlias()), 1)
                .addBr()
                .addBold("报警时间：" + DateUtils.formatDateTime(event.getPublishTime()))
                .addBr()
                .addBold(String.format("报警机器：%s:%s", event
                        .getNode()
                        .getHost(), event
                        .getNode()
                        .getPort()))
                .addBr()
                .addBold("任务路径：" + refusedTask.getReference())
                .addBr()
                .addBold("拒绝原因：" + refusedContent)
                .addBr()
                .addError("请及时处理")
                .addBr()
                .addItalics("[该邮件为系统自动发送，请不要回复此邮件]")
                .getAlertMail();

    }
}
