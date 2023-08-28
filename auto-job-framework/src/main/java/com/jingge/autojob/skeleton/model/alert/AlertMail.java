package com.jingge.autojob.skeleton.model.alert;

import com.jingge.autojob.skeleton.framework.mail.IMailClient;
import com.jingge.autojob.util.convert.StringUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 报警邮件对象，请通过{@link AlertMailBuilder}对象构建
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/28 15:50
 */
@Getter
@Setter
@Accessors(chain = true)
public class AlertMail {
    /**
     * 标题
     */
    private String title;
    /**
     * 内容
     */
    private String content;
    /**
     * 报警级别
     */
    private String level;
    /**
     * 邮箱客户端
     */
    private IMailClient mailClient;

    protected AlertMail() {
    }

    public boolean send() {
        if (mailClient != null) {
            if (!StringUtils.isEmpty(level)) {
                return mailClient.sendMail(level + "：" + title, String.format("<h2 style='color: %s'>报警级别：%s\n%s</h2>", getLevelColor(), level, content));
            } else {
                return mailClient.sendMail(title, content);
            }
        }
        return false;
    }

    private String getLevelColor() {
        switch (level) {
            case "提醒": {
                return "MediumSpringGreen";
            }
            case "警告": {
                return "Coral";
            }
            case "严重警告": {
                return "HotPink";
            }
            case "系统错误": {
                return "red";
            }
        }
        return "black";
    }
}
