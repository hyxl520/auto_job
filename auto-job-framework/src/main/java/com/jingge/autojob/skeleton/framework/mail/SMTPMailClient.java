package com.jingge.autojob.skeleton.framework.mail;

import com.jingge.autojob.skeleton.enumerate.MailType;
import com.jingge.autojob.util.convert.StringUtils;
import com.jingge.autojob.util.thread.SyncHelper;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * 邮件客户端类，仅支持smtp协议
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/28 11:22
 */
@Slf4j
public class SMTPMailClient implements IMailClient {
    /**
     * 本人邮箱地址
     */
    private final String username;
    /**
     * 邮箱密码
     */
    private final String password;
    /**
     * 要发送到的邮箱地址
     */
    private String receiveMail;
    /**
     * 要发送的邮件地址，支持多个
     */
    private String[] receiveMailAddress;
    /**
     * 邮箱类型
     */
    private final MailType mailType;
    /**
     * SMTP服务器地址
     */
    private String smtpAddress;

    private int smtpPort;
    /**
     * 允许的发送间隔
     */
    private long interval = 5000;
    private long lastSendTime = System.currentTimeMillis() - interval;

    public static Builder builder(String senderUsername) {
        return new Builder(senderUsername);
    }

    private SMTPMailClient(String username, String password, String[] receiveMailAddress, MailType mailType) {
        this.username = username;
        this.password = password;
        if (receiveMailAddress != null) {
            if (receiveMailAddress.length > 1) {
                this.receiveMailAddress = receiveMailAddress;
            } else if (receiveMailAddress.length == 1) {
                this.receiveMail = receiveMailAddress[0];
            }
        }
        this.mailType = mailType;
    }

    private SMTPMailClient(String username, String password, String[] receiveMailAddress, String smtpAddress, int smtpPort) {
        if (smtpPort < 0 || StringUtils.isEmpty(smtpAddress)) {
            throw new IllegalArgumentException("错误的SMTP服务器地址");
        }
        this.username = username;
        this.password = password;
        if (receiveMailAddress != null) {
            if (receiveMailAddress.length > 1) {
                this.receiveMailAddress = receiveMailAddress;
            } else if (receiveMailAddress.length == 1) {
                this.receiveMail = receiveMailAddress[0];
            }
        }
        this.smtpAddress = smtpAddress;
        this.smtpPort = smtpPort;
        this.mailType = MailType.CUSTOMIZE;
    }


    public boolean sendMail(String title, String body) {
        SyncHelper.aWaitQuietly(() -> System.currentTimeMillis() >= lastSendTime + interval);
        lastSendTime = System.currentTimeMillis();
        switch (mailType) {
            case GMAIL: {
                return sendMailGmail(title, body);
            }
            case QQ_MAIL: {
                return sendMailQQ(title, body);
            }
            case _163MAIL: {
                return sendMail163(title, body);
            }
            case OUT_LOOK_MAIL: {
                return sendMailOutLook(title, body);
            }
            case CUSTOMIZE: {
                return sendMailCustomize(title, body);
            }
        }
        return false;
    }

    private boolean sendMailQQ(String title, String body) {
        //设置参数
        Properties props = new Properties();
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", "smtp.qq.com");
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.port", 465);
        //自定义信息
        props.put("username", username);//你的邮箱
        props.put("password", password);//你的密码
        if (!StringUtils.isEmpty(receiveMail)) {
            props.put("to", receiveMail);//接收的邮箱
        }
        if (receiveMailAddress != null && receiveMailAddress.length > 0) {
            return SMTPMailClient.sendAll(props, title, body, receiveMailAddress);
        }
        return SMTPMailClient.send(props, title, body);
    }


    /**
     * 发送邮件到自己的163邮箱
     *
     * @param title 需要传输的标题
     * @param body  需要传输的内容
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/7/28 11:30
     */
    private boolean sendMail163(String title, String body) {
        //设置参数
        Properties props = new Properties();
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", "smtp.163.com");
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.port", 465);
        //自定义信息
        props.put("username", username);//你的邮箱
        props.put("password", password);//你的密码
        if (!StringUtils.isEmpty(receiveMail)) {
            props.put("to", receiveMail);//接收的邮箱
        }
        if (receiveMailAddress != null && receiveMailAddress.length > 0) {
            return SMTPMailClient.sendAll(props, title, body, receiveMailAddress);
        }
        return SMTPMailClient.send(props, title, body);
    }

    /**
     * 发送邮件到gmail
     * 国内网络无法访问
     *
     * @param title 标题
     * @param body  内容
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/7/28 11:30
     */
    private boolean sendMailGmail(String title, String body) {

        //设置参数
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        //自定义信息
        props.put("username", username);//你的邮箱
        props.put("password", password);//你的密码
        if (!StringUtils.isEmpty(receiveMail)) {
            props.put("to", receiveMail);//接收的邮箱
        }
        if (receiveMailAddress != null && receiveMailAddress.length > 0) {
            return SMTPMailClient.sendAll(props, title, body, receiveMailAddress);
        }
        return SMTPMailClient.send(props, title, body);

    }

    /**
     * 发送邮件到outlook
     *
     * @param title 标题
     * @param body  内容
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/7/28 11:29
     */
    private boolean sendMailOutLook(String title, String body) {
        //设置参数
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.outlook.com");
        props.put("mail.smtp.port", "587");
        //自定义信息
        props.put("username", username);//你的邮箱
        props.put("password", password);//你的密码
        if (!StringUtils.isEmpty(receiveMail)) {
            props.put("to", receiveMail);//接收的邮箱
        }
        if (receiveMailAddress != null && receiveMailAddress.length > 0) {
            return SMTPMailClient.sendAll(props, title, body, receiveMailAddress);
        }
        return SMTPMailClient.send(props, title, body);

    }

    private boolean sendMailCustomize(String title, String body) {
//设置参数
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtpAddress);
        props.put("mail.smtp.port", smtpPort);
        //自定义信息
        props.put("username", username);//你的邮箱
        props.put("password", password);//你的密码
        if (!StringUtils.isEmpty(receiveMail)) {
            props.put("to", receiveMail);//接收的邮箱
        }
        if (receiveMailAddress != null && receiveMailAddress.length > 0) {
            return SMTPMailClient.sendAll(props, title, body, receiveMailAddress);
        }
        return SMTPMailClient.send(props, title, body);
    }

    /**
     * 获取系统当前的时间
     * 以传入时间格式返回，传空返回默认格式
     *
     * @param format 时间格式
     * @return java.lang.String
     * @author Huang Yongxiang
     * @date 2022/7/28 11:29
     */
    private static String getTitleTimeFormat(String format) {
        if (format == null) {
            format = "yyyy-MM-dd HH:mm:ss/SSS";
        }
        SimpleDateFormat df = new SimpleDateFormat(format);//设置日期格式
        return df.format(new Date());// new Date()为获取当前系统时间
    }

    /**
     * 发送邮件，获取参数，和标题还有内容
     *
     * @param props 参数
     * @param title 标题
     * @param body  内容
     * @return java.lang.Boolean
     * @author Huang Yongxiang
     * @date 2022/7/28 11:29
     */
    private static Boolean send(Properties props, String title, String body) {
        //发送邮件地址
        final String username = props.getProperty("username");
        //发送邮件名称
        final String password = props.getProperty("password");
        //接收邮件地址
        String to = props.getProperty("to");

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(title + "(" + SMTPMailClient.getTitleTimeFormat(null) + ")");
            message.setContent(body, "text/html;" + "charset=utf-8");
            Transport.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        log.info("发送完毕！");

        return true;
    }


    private static boolean sendAll(Properties props, String title, String body, String[] receiveMailAddress) {
        //发送邮件地址
        final String username = props.getProperty("username");
        //发送邮件名称
        final String password = props.getProperty("password");
        if (receiveMailAddress == null || receiveMailAddress.length == 0) {
            return false;
        }
        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            Arrays
                    .stream(receiveMailAddress)
                    .forEach(item -> {
                        try {
                            message.addRecipient(Message.RecipientType.TO, new InternetAddress(item));
                        } catch (MessagingException e) {
                            e.printStackTrace();
                        }
                    });
            message.setSubject(title + "(" + SMTPMailClient.getTitleTimeFormat(null) + ")");
            message.setContent(body, "text/html;" + "charset=utf-8");
            Transport.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        log.info("发送邮件到{}个收件人完毕！", receiveMailAddress.length);
        return true;

    }


    @Setter
    @Accessors(chain = true)
    public static class Builder {
        /**
         * 发送方邮箱地址
         */
        private final String senderUsername;
        /**
         * 发送方smtp授权码，无密码时留空
         */
        private String senderPassword;
        /**
         * 发送方邮箱类型
         */
        private MailType senderMailType;
        /**
         * 接收方邮箱地址
         */
        private String[] receiverAddress;
        /**
         * 发送间隔，密集发送时两次发送的间隔
         */
        private Long interval = 5000L;
        /**
         * 自定义的smtp服务器地址
         */
        private String smtpAddress;
        /**
         * 自定义的smtp服务器端口
         */
        private Integer smtpPort;

        public Builder(String senderUsername) {
            this.senderUsername = senderUsername;
        }

        public Builder setInterval(long interval, TimeUnit unit) {
            this.interval = unit.toMillis(interval);
            return this;
        }

        public Builder setReceiverAddress(String... receiverAddress) {
            this.receiverAddress = receiverAddress;
            return this;
        }

        public IMailClient build() {
            SMTPMailClient mailClient;
            if (senderMailType == null) {
                throw new NullPointerException();
            }
            if (senderMailType != MailType.CUSTOMIZE) {
                mailClient = new SMTPMailClient(senderUsername, senderPassword, receiverAddress, senderMailType);
            } else {
                mailClient = new SMTPMailClient(senderUsername, senderPassword, receiverAddress, smtpAddress, smtpPort);
            }
            mailClient.interval = interval;
            mailClient.lastSendTime = System.currentTimeMillis() - interval;
            return mailClient;
        }
    }

}
