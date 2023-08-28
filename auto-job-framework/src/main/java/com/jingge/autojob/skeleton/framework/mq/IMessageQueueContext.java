package com.jingge.autojob.skeleton.framework.mq;


import java.util.List;

/**
 * 消息上下文抽象接口
 *
 * @Auther Huang Yongxiang
 * @Date 2022/03/20 9:57
 */
public interface IMessageQueueContext<M> {

    int length(String topic);

    /**
     * 使得消息立即过期
     *
     * @param topic 主题
     * @return void
     * @throws ErrorExpiredException 过期时发生异常抛出
     * @author Huang Yongxiang
     * @date 2022/3/20 11:31
     */
    void expire(String topic, MessageEntry<M> messageEntry) throws ErrorExpiredException;

    /**
     * 摧毁消息容器并启动垃圾清理
     *
     * @return void
     * @author Huang Yongxiang
     * @date 2022/3/22 14:33
     */
    void destroy();

    /**
     * 添加一个消息发布监听器，监听器的操作不会影响消息的正常发布
     *
     * @param topic    要添加监听器的主题
     * @param listener 监听器
     * @return void
     * @author Huang Yongxiang
     * @date 2022/11/7 9:43
     */
    void addMessagePublishedListener(String topic, MessagePublishedListener<M> listener);

    /**
     * 移除主题下的所有消息发布监听器
     *
     * @param topic 要移除的主题
     * @return java.util.List<com.example.autojob.skeleton.framework.mq.MessagePublishedListener < M>>
     * 移除的监听器列表，如果移除失败或不存在，返回空列表
     * @author Huang Yongxiang
     * @date 2022/11/7 9:53
     */
    List<MessagePublishedListener<M>> removeAllMessagePublishedListener(String topic);

    boolean removeMessagePublishedListener(String topic, String listenerName);

    /**
     * 添加一个消息过期监听器
     *
     * @param topic    主题
     * @param listener 监听器
     * @return void
     * @author Huang Yongxiang
     * @date 2022/11/7 9:54
     */
    void addMessageExpiredListener(String topic, MessageExpiredListener<M> listener);

    /**
     * 移除所有的消息过期监听器
     *
     * @param topic 主题
     * @return java.util.List<com.example.autojob.skeleton.framework.mq.MessageExpiredListener < M>>
     * @author Huang Yongxiang
     * @date 2022/11/7 9:55
     */
    List<MessageExpiredListener<M>> removeAllMessageExpiredListener(String topic);

    boolean removeMessageExpiredListener(String topic, String listenerName);
}
