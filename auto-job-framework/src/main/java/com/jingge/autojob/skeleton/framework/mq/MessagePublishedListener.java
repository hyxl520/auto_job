package com.jingge.autojob.skeleton.framework.mq;


/**
 * 消息发布监听器，当有消息发布到消息队列时将会执行相关逻辑
 *
 * @Author Huang Yongxiang
 * @Date 2022/11/02 14:52
 * @Email 1158055613@qq.com
 */
public interface MessagePublishedListener<M> extends IMessageListener {
    void onMessagePublished(M message, MessageQueue<MessageEntry<M>> queue);

    default String listenerName() {
        return "defaultMessagePublishedListener";
    }
}
