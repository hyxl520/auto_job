package com.jingge.autojob.skeleton.framework.mq;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 消费者
 *
 * @Auther Huang Yongxiang
 * @Date 2022/03/21 12:53
 */
public class MessageConsumer<M> implements IConsumer<M> {
    private MessageQueueContext<M> messageQueueContext;

    public MessageConsumer(MessageQueueContext<M> messageQueueContext) {
        this.messageQueueContext = messageQueueContext;
    }


    @Override
    public M takeMessageBlock(String topic, boolean isTakeout) {
        if (messageQueueContext == null) {
            throw new NullPointerException("消息容器为空");
        }
        return messageQueueContext.takeMessageBlock(topic, isTakeout);
    }

    @Override
    public M takeMessageNoBlock(String topic, boolean isTakeout) {
        if (messageQueueContext == null) {
            throw new NullPointerException("消息容器为空");
        }
        return messageQueueContext.takeMessageNoBlock(topic, isTakeout);
    }

    @Override
    public List<M> takeAllMessageNoBlock(String topic, boolean isTakeout) {
        return messageQueueContext.takeAllMessageNoBlock(topic, isTakeout);
    }

    @Override
    public List<M> takeMessageByRegexTopicBlock(String regexTopic, boolean isTakeout) {
        return messageQueueContext.takeMessageByRegexTopicBlock(regexTopic, isTakeout);
    }

    @Override
    public List<M> takeMessageByRegexTopicNoBlock(String regexTopic, boolean isTakeout) {
        return messageQueueContext.takeMessageByRegexTopicNoBlock(regexTopic, isTakeout);
    }

    @Override
    public MessageQueue<MessageEntry<M>> subscriptionMessage(String topic) {
        return messageQueueContext.subscriptionMessage(topic);
    }

    @Override
    public MessageQueue<MessageEntry<M>> subscriptionMessage(String topic, long wait, TimeUnit unit) {
        return messageQueueContext.subscriptionMessage(topic, wait, unit);
    }

    @Override
    public void unsubscribe(String topic) {
        messageQueueContext.unsubscribe(topic);
    }

    @Override
    public void unsubscribe(String topic, long wait, TimeUnit unit) {
        messageQueueContext.unsubscribe(topic, wait, unit);
    }

    public MessageQueueContext<M> getMessageQueueContext() {
        return messageQueueContext;
    }

    public MessageConsumer<M> setMessageQueueContext(MessageQueueContext<M> messageQueueContext) {
        this.messageQueueContext = messageQueueContext;
        return this;
    }
}
