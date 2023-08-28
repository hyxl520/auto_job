package com.jingge.autojob.skeleton.framework.mq;

/**
 * 消息条目
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-05-26 9:45
 * @email 1158055613@qq.com
 */
public class MessageEntry<M> {
    long messageId;
    M message;
    long expiringTime;

    public MessageEntry(long messageId, M message, long expiringTime) {
        this.messageId = messageId;
        this.message = message;
        this.expiringTime = expiringTime;
    }

    public MessageEntry() {
    }

    MessageEntry<M> setMessageId(long messageId) {
        this.messageId = messageId;
        return this;
    }

    MessageEntry<M> setMessage(M message) {
        this.message = message;
        return this;
    }

    MessageEntry<M> setExpiringTime(long expiringTime) {
        this.expiringTime = expiringTime;
        return this;
    }

    public long getMessageId() {
        return messageId;
    }

    public M getMessage() {
        return message;
    }

    public long getExpiringTime() {
        return expiringTime;
    }
}
