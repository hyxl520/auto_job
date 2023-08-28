package com.jingge.autojob.skeleton.framework.mq;

import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Predicate;

/**
 * 消息队列
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-05-26 9:44
 * @email 1158055613@qq.com
 */
@Slf4j
public class MessageQueue<M> {
    final BlockingQueue<M> messageQueue;

    public MessageQueue() {
        messageQueue = new LinkedBlockingQueue<>();
    }


    public MessageQueue(int maxLength) {
        if (maxLength <= 0) {
            throw new IllegalArgumentException("最大容量应该为非负数");
        }
        messageQueue = new LinkedBlockingDeque<>(maxLength);
    }

    public MessageQueue(BlockingQueue<M> messageQueue) {
        this.messageQueue = messageQueue;
    }

    public M takeMessageSync() throws InterruptedException {
        return messageQueue.take();
    }

    public void clear() {
        messageQueue.clear();
    }

    public M takeMessage() {
        return messageQueue.poll();
    }

    public M readMessage() {
        return messageQueue.peek();
    }

    public M tryReadMessage() {
        return messageQueue.element();
    }

    public boolean removeIf(Predicate<? super M> predicate) {
        if (predicate == null) {
            return false;
        }
        return messageQueue.removeIf(predicate);
    }

    public int length() {
        return messageQueue.size();
    }

    public boolean remove(M message) {
        if (message == null) {
            return false;
        }
        return messageQueue.remove(message);
    }

    public Iterator<M> iterator() {
        return messageQueue.iterator();
    }

    public boolean publishMessageSync(M message) throws InterruptedException {
        if (message == null) {
            return false;
        }
        try {
            messageQueue.put(message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public boolean publishMessage(M message) {
        if (message == null) {
            return false;
        }
        try {
            return messageQueue.offer(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
