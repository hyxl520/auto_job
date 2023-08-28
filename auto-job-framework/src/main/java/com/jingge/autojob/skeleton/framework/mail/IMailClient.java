package com.jingge.autojob.skeleton.framework.mail;

/**
 * 邮件客户端接口
 *
 * @author Huang Yongxiang
 * @date 2023-01-03 15:30
 * @email 1158055613@qq.com
 */
public interface IMailClient {
    boolean sendMail(String title, String body);
}
