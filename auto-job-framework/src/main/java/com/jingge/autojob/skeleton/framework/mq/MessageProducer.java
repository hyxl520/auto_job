package com.jingge.autojob.skeleton.framework.mq;

import lombok.Data;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 生产者
 *
 * @Auther Huang Yongxiang
 * @Date 2022/03/21 12:54
 */
@Data
public class MessageProducer<M> implements IProducer<M> {
    private MessageQueueContext<M> queueContext;

    public MessageProducer(final MessageQueueContext<M> messageQueueContext) {
        this.queueContext = messageQueueContext;
    }


    public MessageQueueContext<M> getMessageQueueContext() {
        return queueContext;
    }


    @Override
    public boolean registerMessageQueue(String topic) {
        return queueContext.registerMessageQueue(topic);
    }

    @Override
    public boolean registerMessageQueue(String topic, BlockingQueue<MessageEntry<M>> queue) {
        return queueContext.registerMessageQueue(topic, queue);
    }

    @Override
    public boolean hasTopic(String topic) {
        return queueContext.hasTopic(topic);
    }

    @Override
    public boolean hasRegexTopic(String regexTopic) {
        return queueContext.hasRegexTopic(regexTopic);
    }

    @Override
    public boolean removeMessageQueue(String topic) {
        return queueContext.removeMessageQueue(topic);
    }

    @Override
    public boolean publishMessageNoBlock(M message, String topic) {
        return queueContext.publishMessageNoBlock(message, topic);
    }

    @Override
    public boolean publishMessageNoBlock(M message, String topic, long expiringTime, TimeUnit unit) {
        return queueContext.publishMessageNoBlock(message, topic, expiringTime, unit);
    }

    @Override
    public boolean publishMessageBlock(M message, String topic) {
        return queueContext.publishMessageBlock(message, topic);
    }

    @Override
    public boolean publishMessageBlock(M message, String topic, long expiringTime, TimeUnit unit) {
        return queueContext.publishMessageBlock(message, topic, expiringTime, unit);
    }

    public MessageProducer<M> setMessageQueueContext(MessageQueueContext<M> queueContext) {
        this.queueContext = queueContext;
        return this;
    }
}
