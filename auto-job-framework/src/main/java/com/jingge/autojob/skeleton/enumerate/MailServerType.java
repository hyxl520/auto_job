package com.jingge.autojob.skeleton.enumerate;

import com.jingge.autojob.util.convert.StringUtils;

/**
 * 邮件服务器类型
 *
 * @author Huang Yongxiang
 * @date 2023-01-10 15:41
 * @email 1158055613@qq.com
 */
public enum MailServerType {
    SMTP("smtp");
    private final String name;

    MailServerType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static MailServerType findByName(String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        for (MailServerType type : values()) {
            if (type.name.equalsIgnoreCase(name.trim())) {
                return type;
            }
        }
        return null;
    }
}
