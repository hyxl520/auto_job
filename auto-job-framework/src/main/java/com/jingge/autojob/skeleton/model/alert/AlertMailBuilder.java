package com.jingge.autojob.skeleton.model.alert;

import com.jingge.autojob.skeleton.enumerate.AlertEventLevel;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.mail.IMailClient;
import com.jingge.autojob.util.convert.DefaultValueUtil;

/**
 * 报警邮件构建者对象
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/28 16:11
 */
public class AlertMailBuilder {
    private AlertMail alertMail;

    private StringBuilder builder;

    private IMailClient mailClient;

    private AlertMailBuilder(AlertMail alertMail) {
        this.alertMail = alertMail;
        this.builder = new StringBuilder();
    }

    /**
     * 清空已经构建的邮件内容，单例模式下在使用前应该调用此方法清空上次使用构建的内容
     *
     * @return void
     * @author Huang Yongxiang
     * @date 2022/7/29 17:58
     */
    public void clear() {
        this.builder = new StringBuilder();
        this.alertMail = new AlertMail();
    }

    /**
     * 获取构建者单例，单例不是线程安全的
     *
     * @return com.example.autojob.skeleton.model.alert.AlertMailBuilder
     * @author Huang Yongxiang
     * @date 2022/7/29 17:56
     */
    public static AlertMailBuilder getInstance() {
        return InstanceHolder.BUILDER;
    }

    /**
     * 新创建一个构建者实例
     *
     * @return com.example.autojob.skeleton.model.alert.AlertMailBuilder
     * @author Huang Yongxiang
     * @date 2022/7/29 17:56
     */
    public static AlertMailBuilder newInstance() {
        return new AlertMailBuilder(new AlertMail());
    }

    public AlertMailBuilder setTitle(String title) {
        alertMail.setTitle(title);
        return this;
    }

    public AlertMailBuilder setLevel(AlertEventLevel level) {
        alertMail.setLevel(AlertEventLevel.valueOf(level));
        return this;
    }

    public AlertMailBuilder addContentTitle(String contentTitle, int level) {
        builder
                .append(String.format("<h%d>", level))
                .append(contentTitle)
                .append(String.format("</h%d>", level));
        return this;
    }

    public AlertMailBuilder addHyperlinks(String content, String linkTo) {
        builder
                .append("<a ")
                .append("src=")
                .append("\"")
                .append(linkTo)
                .append("\"")
                .append(">")
                .append(content)
                .append("</a>");
        return this;
    }

    public AlertMailBuilder addImage(String url, String alt) {
        builder.append(String.format("<img src='%s' alt='%s'></img>", url, alt));
        return this;
    }

    public AlertMailBuilder addItalics(String content) {
        builder.append(String.format("<i>%s</i>", content));
        return this;
    }

    public AlertMailBuilder addParagraph(String content) {
        builder.append(String.format("<p>%s</p>", content));
        return this;
    }


    public AlertMailBuilder addWarn(String content) {
        builder.append(String.format("<h3 style='color: Coral;'>%s</h3>", content));
        return this;
    }

    public AlertMailBuilder addInfo(String content) {
        builder.append(String.format("<h3 style='color: MediumSpringGreen;'>%s</h3>", content));
        return this;
    }

    public AlertMailBuilder addSeriousWarn(String content) {
        builder.append(String.format("<h3 style='color: HotPink;'>%s</h3>", content));
        return this;
    }

    public AlertMailBuilder addError(String content) {
        builder.append(String.format("<h3 style='color: red;'>%s</h3>", content));
        return this;
    }

    /**
     * 添加一个加粗文本
     *
     * @param content 文本内容
     * @return com.example.autojob.skeleton.model.alert.AlertMailBuilder
     * @author Huang Yongxiang
     * @date 2022/7/29 17:55
     */
    public AlertMailBuilder addBold(String content) {
        builder.append(String.format("<b>%s</b>", content));
        return this;
    }

    /**
     * 添加一个换行
     *
     * @return com.example.autojob.skeleton.model.alert.AlertMailBuilder
     * @author Huang Yongxiang
     * @date 2022/7/29 17:55
     */
    public AlertMailBuilder addBr() {
        builder.append("</br>");
        return this;
    }

    public AlertMailBuilder addColor(String content, String color) {
        builder.append(String.format("<span style='color: %s;'>%s</span>", color, content));
        return this;
    }

    public AlertMailBuilder addBlankSpace() {
        builder.append("&nbsp;");
        return this;
    }

    public AlertMailBuilder setMailClient(IMailClient mailClient) {
        this.mailClient = mailClient;
        return this;
    }

    public AlertMail getAlertMail() {
        alertMail.setContent(builder.toString());
        alertMail.setMailClient(DefaultValueUtil.defaultValue(mailClient, AutoJobApplication
                .getInstance()
                .getMailClient()));
        return alertMail;
    }

    private static class InstanceHolder {
        private static final AlertMailBuilder BUILDER = new AlertMailBuilder(new AlertMail());
    }
}
