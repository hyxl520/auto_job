package com.jingge.autojob.logging.model.producer;

import com.jingge.autojob.skeleton.framework.mq.MessageProducer;

/**
 * 任务日志生产者的接口
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/15 12:23
 */
public interface IAutoJobLogProducer<E> {
    void produce(MessageProducer<E> producer, String topic,E autoJobLog);

}
