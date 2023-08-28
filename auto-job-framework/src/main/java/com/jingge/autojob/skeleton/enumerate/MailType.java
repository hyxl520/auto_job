package com.jingge.autojob.skeleton.enumerate;

/**
 * 邮件类型
 *
 * @author Huang Yongxiang
 * @date 2023-01-10 15:33
 * @email 1158055613@qq.com
 */
public enum MailType {
    QQ_MAIL("QQMail"), GMAIL("gMail"), _163MAIL("163Mail"), OUT_LOOK_MAIL("outLookMail"), CUSTOMIZE("customize");

    private final String name;

    MailType(String name) {
        this.name = name;
    }

    public static MailType convert(String name) {
        for (MailType type : MailType.values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }
}
