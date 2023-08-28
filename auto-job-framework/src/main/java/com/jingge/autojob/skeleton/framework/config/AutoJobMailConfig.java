package com.jingge.autojob.skeleton.framework.config;

import com.jingge.autojob.skeleton.enumerate.MailServerType;
import com.jingge.autojob.skeleton.enumerate.MailType;
import com.jingge.autojob.util.io.PropertiesHolder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 邮件报警配置
 *
 * @author Huang Yongxiang
 * @date 2023-01-10 15:26
 * @email 1158055613@qq.com
 */
@Getter
@Setter
@Accessors(chain = true)
public class AutoJobMailConfig extends AbstractAutoJobConfig implements StorableConfig {
    /**
     * 邮件服务器类型
     */
    private MailServerType mailServerType;
    /**
     * 是否启用邮件报警
     */
    private Boolean enable;
    /**
     * 发送方用户名
     */
    private String senderUsername;
    /**
     * 发送方密码
     */
    private String senderPassword;
    /**
     * 接收方地址
     */
    private String[] receiverAddress;
    /**
     * 邮箱类型
     */
    private MailType mailType;
    /**
     * 发送间隔
     */
    private Long interval;
    /**
     * 自定义邮件服务器地址
     */
    private String customMailServerAddress;
    /**
     * 自定义邮件服务器端口号
     */
    private Integer customMailServerPort;
    /**
     * 关联到的任务ID
     */
    private Long taskId;

    @Override
    public ConfigSerializer getSerializer() {
        return new ConfigJsonSerializerAndDeserializer();
    }

    @Override
    public Long getTaskId() {
        return taskId;
    }



    public AutoJobMailConfig(PropertiesHolder propertiesHolder) {
        super(propertiesHolder);
        enable = propertiesHolder.getProperty("autoJob.emailAlert.enable", Boolean.class, "false");
        interval = propertiesHolder.getProperty("autoJob.emailAlert.interval", Long.class, "5000");
        String mailServerTypStr = propertiesHolder.getProperty("autoJob.emailAlert.serverType", String.class, "SMTP");
        mailServerType = MailServerType.findByName(mailServerTypStr);
        senderUsername = propertiesHolder.getProperty("autoJob.emailAlert.auth.sender");
        senderPassword = propertiesHolder.getProperty("autoJob.emailAlert.auth.token");
        receiverAddress = propertiesHolder
                .getProperty("autoJob.emailAlert.auth.receiver", "")
                .split(",");
        String maiTypeStr = propertiesHolder.getProperty("autoJob.emailAlert.auth.type");
        mailType = MailType.convert(maiTypeStr);
        customMailServerAddress = propertiesHolder.getProperty("autoJob.emailAlert.auth.customize.customMailServerAddress", "");
        customMailServerPort = propertiesHolder.getProperty("autoJob.emailAlert.auth.customize.customMailServerPort", Integer.class, "0");
    }

    public AutoJobMailConfig() {
        super(null);
    }
}
