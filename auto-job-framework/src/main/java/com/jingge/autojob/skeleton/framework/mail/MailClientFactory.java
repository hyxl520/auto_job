package com.jingge.autojob.skeleton.framework.mail;

import com.jingge.autojob.skeleton.enumerate.MailServerType;
import com.jingge.autojob.skeleton.framework.config.AutoJobMailConfig;
import com.jingge.autojob.skeleton.lang.IAutoJobFactory;

/**
 * 邮箱客户端工厂
 *
 * @author Huang Yongxiang
 * @date 2023-01-10 16:34
 * @email 1158055613@qq.com
 */
public class MailClientFactory implements IAutoJobFactory {
    private static SMTPMailClient createSMTPMailClient(AutoJobMailConfig config) {
        if (config == null || config.getEnable() == null || !config.getEnable() || config.getMailType() == null || config.getMailServerType() != MailServerType.SMTP) {
            return null;
        }
        return (SMTPMailClient) SMTPMailClient
                .builder(config.getSenderUsername())
                .setSenderPassword(config.getSenderPassword())
                .setInterval(config.getInterval())
                .setReceiverAddress(config.getReceiverAddress())
                .setSmtpAddress(config.getCustomMailServerAddress())
                .setSmtpPort(config.getCustomMailServerPort())
                .setSenderMailType(config.getMailType())
                .build();
    }

    public static IMailClient createMailClient(AutoJobMailConfig config) {
        if (config == null || config.getEnable() == null || !config.getEnable() || config.getMailServerType() == null) {
            return null;
        }
        switch (config.getMailServerType()) {
            case SMTP:
                return createSMTPMailClient(config);
            default:
                return null;
        }
    }
}
