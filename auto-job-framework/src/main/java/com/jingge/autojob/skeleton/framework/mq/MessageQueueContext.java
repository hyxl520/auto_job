package com.jingge.autojob.skeleton.framework.mq;

import com.jingge.autojob.skeleton.framework.pool.AbstractAutoJobPool;
import com.jingge.autojob.util.convert.RegexUtil;
import com.jingge.autojob.util.convert.StringUtils;
import com.jingge.autojob.util.id.IdGenerator;
import com.jingge.autojob.util.thread.ScheduleTaskUtil;
import com.jingge.autojob.util.thread.SyncHelper;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 内存消息队列的context
 *
 * @Auther Huang Yongxiang
 * @Date 2022/03/18 17:14
 */
@Slf4j
public class MessageQueueContext<M> implements IMessageQueueContext<M>, IProducer<M>, IConsumer<M> {

    /**
     * 默认过期时间：ms，对所有消息设置，-1则表示消息均为永久性消息，除非消费者取出，否则将会一直存在。谨慎使用！
     */
    private long defaultExpiringTime = -1;

    /**
     * 是否允许设置单个消息的过期时间
     */
    private boolean isAllowSetEntryExpired = false;

    /**
     * 允许的最大主题数
     */
    private int allowMaxTopicCount = 255;

    /**
     * 允许每个队列的最大消息数
     */
    private int allowMaxMessageCountPerQueue;

    /**
     * 过期监听器的策略
     */
    private ExpirationListenerPolicy listenerPolicy;

    //存储消息的数据结构
    private Map<String, MessageQueue<MessageEntry<M>>> messageQueues;

    private final Map<String, List<MessagePublishedListener<M>>> publishedListenersMap = new ConcurrentHashMap<>();

    private final Map<String, List<MessageExpiredListener<M>>> expiredListenersMap = new ConcurrentHashMap<>();

    private boolean isOpenListener = false;

    /**
     * 守护线程
     */
    private ScheduledExecutorService executorService;

    /**
     * 各个主题的订阅数
     */
    private Map<String, AtomicLong> subscriptionCount;

    private ThreadPoolExecutor concurrencyThreads;


    public static Builder<Object> builder() {
        return new Builder<>();
    }

    private MessageQueueContext() {

    }

    @PostConstruct
    public void init() {

    }

    @Override
    public boolean registerMessageQueue(String topic) {
        return registerMessageQueue(topic, new LinkedBlockingQueue<>(allowMaxMessageCountPerQueue));
    }

    @Override
    public boolean registerMessageQueue(String topic, BlockingQueue<MessageEntry<M>> queue) {
        if (StringUtils.isEmpty(topic)) {
            log.error("创建队列失败，主题为空");
            return false;
        }
        if (messageQueues.containsKey(topic)) {
            return false;
        }
        if (messageQueues.size() >= allowMaxTopicCount) {
            log.error("当前消息容器最大支持{}个主题", allowMaxTopicCount);
            return false;
        }
        try {
            MessageQueue<MessageEntry<M>> messageQueue = new MessageQueue<>(queue);
            messageQueues.put(topic, messageQueue);
            if (!isOpenListener) {
                synchronized (MessageQueueContext.class) {
                    if (!isOpenListener) {
                        if (listenerPolicy == ExpirationListenerPolicy.SINGLE_THREAD) {
                            scheduleExpiringCheckSingleThread();
                        } else {
                            scheduleExpiringCheckTopicConcurrency();
                        }
                        isOpenListener = true;
                    }
                }
            }
            if (subscriptionCount == null) {
                subscriptionCount = new ConcurrentHashMap<>();
            }
            subscriptionCount.put(topic, new AtomicLong(0));
            return true;
        } catch (Exception e) {
            log.error("创建队列发生异常：{}", e.getMessage());
        }
        return false;
    }

    @Override
    public boolean hasTopic(String topic) {
        return messageQueues.containsKey(topic);
    }

    @Override
    public boolean hasRegexTopic(String regexTopic) {
        return messageQueues
                .keySet()
                .stream()
                .anyMatch(topic -> RegexUtil.isMatch(topic, regexTopic));
    }

    @Override
    public boolean removeMessageQueue(String topic) {
        try {
            messageQueues.remove(topic);
            return true;
        } catch (Exception e) {
            log.error("移除消息队列时发生异常：{}", e.getMessage());
        }
        return false;
    }

    @Override
    public boolean publishMessageNoBlock(M message, String topic, long expiringTime, TimeUnit unit) {
        if (!isAllowSetEntryExpired) {
            log.error("不允许设置单个消息的过期时间");
            return false;
        }
        if (!messageQueues.containsKey(topic)) {
            log.error("发布非阻塞消息失败，所要发布到的队列：{}不存在", topic);
            return false;
        }
        if (expiringTime <= 0 || unit == null) {
            log.error("非法过期时间");
            return false;
        }
        if (message == null) {
            log.error("禁止发布空消息");
            return false;
        }
        MessageEntry<M> messageEntry = new MessageEntry<>();
        messageEntry.setMessageId(IdGenerator.getNextIdAsLong());
        messageEntry.setMessage(message);
        messageEntry.setExpiringTime(unit.toMillis(expiringTime) + System.currentTimeMillis());
        if (messageQueues
                .get(topic)
                .publishMessage(messageEntry)) {
            executeMessagePublishedListeners(topic, message);
            return true;
        }
        return false;
    }

    @Override
    public boolean publishMessageBlock(M message, String topic, long expiringTime, TimeUnit unit) {
        if (!isAllowSetEntryExpired) {
            log.error("不允许设置单个消息的过期时间");
            return false;
        }
        if (!messageQueues.containsKey(topic)) {
            log.error("发布非阻塞消息失败，所要发布到的队列：{}不存在", topic);
            return false;
        }
        if (expiringTime <= 0 || unit == null) {
            log.error("非法过期时间");
            return false;
        }
        if (message == null) {
            log.error("禁止发布空消息");
            return false;
        }
        MessageEntry<M> messageEntry = new MessageEntry<>();
        messageEntry.setMessageId(IdGenerator.getNextIdAsLong());
        messageEntry.setMessage(message);
        messageEntry.setExpiringTime(unit.toMillis(expiringTime) + System.currentTimeMillis());
        try {
            if (messageQueues
                    .get(topic)
                    .publishMessageSync(messageEntry)) {
                executeMessagePublishedListeners(topic, message);
                return true;
            }
        } catch (InterruptedException e) {
            log.warn("发布可阻塞消息发生异常，等待时被异常占用：{}", e.getMessage());
        }
        return false;
    }


    /**
     * 阻塞的获取一条消息，可以决定是否将该消息取出，即移出队列，当多播时最好不要移出队列
     *
     * @param topic     主题
     * @param isTakeout 是否移出队列，当为false时该方法将退化成非阻塞的
     * @return M
     * @author Huang Yongxiang
     * @date 2022/3/20 10:55
     */
    @Override
    public M takeMessageBlock(String topic, boolean isTakeout) {
        if (!messageQueues.containsKey(topic)) {
            return null;
        }
        if (isTakeout) {
            try {
                return messageQueues
                        .get(topic)
                        .takeMessageSync().message;
            } catch (InterruptedException e) {
                log.warn("可阻塞获取消息发生异常，等待时被异常占用：{}", e.getMessage());
            }
            return null;
        }
        return messageQueues
                .get(topic)
                .readMessage().message;
    }

    @Override
    public M takeMessageNoBlock(String topic, boolean isTakeout) {
        if (!hasTopic(topic)) {
            return null;
        }
        if (messageQueues
                .get(topic)
                .length() == 0) {
            return null;
        }
        if (isTakeout) {
            return messageQueues
                    .get(topic)
                    .takeMessage().message;
        }
        return messageQueues
                .get(topic)
                .readMessage().message;
    }

    @Override
    public List<M> takeAllMessageNoBlock(String topic, boolean isTakeout) {
        if (!hasTopic(topic)) {
            return Collections.emptyList();
        }
        if (messageQueues
                .get(topic)
                .length() == 0) {
            return Collections.emptyList();
        }
        List<M> messages = new LinkedList<>();
        try {
            if (!isTakeout) {
                return messageQueues.get(topic).messageQueue
                        .stream()
                        .map(item -> item.message)
                        .collect(Collectors.toList());
            } else {
                while (true) {
                    M message = takeMessageNoBlock(topic, true);
                    if (message != null) {
                        messages.add(message);
                    } else {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取全部消息时发生异常：{}", e.getMessage());
            return Collections.emptyList();
        }
        return messages;
    }

    @Override
    public List<M> takeMessageByRegexTopicBlock(String regexTopic, boolean isTakeout) {
        if (!hasRegexTopic(regexTopic)) {
            return null;
        }
        List<String> topics = messageQueues
                .keySet()
                .stream()
                .filter(topic -> RegexUtil.isMatch(topic, regexTopic))
                .collect(Collectors.toList());
        log.debug("获取到正则主题：{}的匹配主题{}个", regexTopic, topics.size());
        if (topics.size() == 0) {
            return Collections.emptyList();
        }
        List<M> messages = new LinkedList<>();
        for (String topic : topics) {
            messages.add(takeMessageBlock(topic, isTakeout));
        }
        return messages;
    }

    @Override
    public List<M> takeMessageByRegexTopicNoBlock(String regexTopic, boolean isTakeout) {
        if (!hasRegexTopic(regexTopic)) {
            return null;
        }
        List<String> topics = messageQueues
                .keySet()
                .stream()
                .filter(topic -> RegexUtil.isMatch(topic, regexTopic))
                .collect(Collectors.toList());
        log.debug("获取到正则主题：{}的匹配主题{}个", regexTopic, topics.size());
        if (topics.size() == 0) {
            return null;
        }
        List<M> messages = new LinkedList<>();
        for (String topic : topics) {
            M m = takeMessageNoBlock(topic, isTakeout);
            if (m == null) {
                continue;
            }
            messages.add(m);
        }
        return messages;
    }

    @Override
    public MessageQueue<MessageEntry<M>> subscriptionMessage(String topic) {
        MessageQueue<MessageEntry<M>> messageQueue = messageQueues.get(topic);
        if (messageQueue != null && subscriptionCount != null) {
            AtomicLong atomicLong = subscriptionCount.get(topic);
            atomicLong.incrementAndGet();
        }
        return messageQueue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public MessageQueue<MessageEntry<M>> subscriptionMessage(String topic, long wait, TimeUnit unit) {
        MessageQueue<MessageEntry<M>> messageQueue = subscriptionMessage(topic);
        if (messageQueue != null) {
            return messageQueue;
        }
        //进行阻塞获取
        ScheduledFuture<Object> future = ScheduleTaskUtil
                .build(false, "subscriptionBlock")
                .EOneTimeTask(() -> {
                    long blockTime = unit.toMillis(wait);
                    int i = 0;
                    try {
                        do {
                            if (hasTopic(topic)) {
                                return messageQueues.get(topic);
                            }
                            Thread.sleep(1);
                        } while (i++ <= blockTime);
                        return null;
                    } catch (Exception e) {
                        log.error("阻塞订阅时发生异常：{}", e.getMessage());
                    }
                    return null;
                }, 1, TimeUnit.MILLISECONDS);
        try {
            return (MessageQueue<MessageEntry<M>>) future.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("阻塞订阅时发生异常：{}", e.getMessage());
        }
        return null;
    }

    @Override
    public void unsubscribe(String topic) {
        unsubscribe(topic, 5, TimeUnit.SECONDS);
    }

    @Override
    public void unsubscribe(String topic, long wait, TimeUnit unit) {
        if (subscriptionCount != null) {
            AtomicLong atomicLong = subscriptionCount.get(topic);
            atomicLong.decrementAndGet();
            if (atomicLong.get() < 0) {
                atomicLong.set(0);
            }
            //当有队列取消订阅，且目前消息数为0，则对指定队列监视5秒，5秒后依然没有生产者发布消息则直接移除主题
            if (atomicLong.get() == 0 && length(topic) == 0) {
                long w = unit.toMillis(wait);
                log.debug("主题为{}的消息队列目前订阅数为0且积压消息为0，当{}ms后若无生产者发布消息将自动删除该主题队列", topic, w);
                Thread thread = new Thread(() -> {
                    try {
                        int i = 0;
                        boolean flag = true;
                        do {
                            if (length(topic) > 0) {
                                flag = false;
                                break;
                            }
                            SyncHelper.sleepQuietly(1, TimeUnit.MILLISECONDS);
                        } while (i++ <= w);
                        if (flag) {
                            log.debug("主题：{}自动删除完成", topic);
                            removeMessageQueue(topic);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                thread.setDaemon(true);
                thread.setName("unsubListener");
                thread.start();
            }
        }
    }

    @Override
    public int length(String topic) {
        if (messageQueues.containsKey(topic)) {
            return messageQueues
                    .get(topic)
                    .length();
        }
        return 0;
    }

    @Override
    public void expire(String topic, MessageEntry<M> messageEntry) throws ErrorExpiredException {
        if (messageEntry == null || !messageQueues.containsKey(topic)) {
            throw new IllegalArgumentException("参数有误，ID非法或主题不存在");
        }
        try {
            boolean flag = messageQueues
                    .get(topic)
                    .remove(messageEntry);
            if (!flag) {
                throw new ErrorExpiredException("移出失败");
            }
        } catch (Exception e) {
            log.error("过期时发生异常");
            throw new ErrorExpiredException(e.getMessage());
        }
    }

    @Override
    public void destroy() {
        messageQueues = null;
        if (isOpenListener) {
            try {
                executorService.shutdown();
                isOpenListener = false;
            } catch (Exception e) {
                log.error("关闭守护线程发生异常：{}", e.getMessage());
            }
        }
        System.gc();
    }

    @Override
    public void addMessagePublishedListener(String topic, MessagePublishedListener<M> listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        if (hasTopic(topic)) {
            if (!this.publishedListenersMap.containsKey(topic)) {
                synchronized (publishedListenersMap) {
                    if (!this.publishedListenersMap.containsKey(topic)) {
                        this.publishedListenersMap.put(topic, new ArrayList<>());
                    }
                }
            }
            publishedListenersMap
                    .get(topic)
                    .add(listener);
        } else {
            throw new IllegalArgumentException("不存在相关主题：" + topic);
        }
    }

    @Override
    public List<MessagePublishedListener<M>> removeAllMessagePublishedListener(String topic) {
        if (publishedListenersMap.containsKey(topic)) {
            return publishedListenersMap.remove(topic);
        }
        return null;
    }

    @Override
    public boolean removeMessagePublishedListener(String topic, String listenerName) {
        if (publishedListenersMap.containsKey(topic) && !StringUtils.isEmpty(listenerName)) {
            return publishedListenersMap
                    .get(topic)
                    .removeIf(listener -> listener
                            .listenerName()
                            .equals(listenerName));
        }
        return false;
    }

    @Override
    public void addMessageExpiredListener(String topic, MessageExpiredListener<M> listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        if (hasTopic(topic)) {
            if (!this.expiredListenersMap.containsKey(topic)) {
                synchronized (expiredListenersMap) {
                    if (!this.expiredListenersMap.containsKey(topic)) {
                        this.expiredListenersMap.put(topic, new ArrayList<>());
                    }
                }
            }
            expiredListenersMap
                    .get(topic)
                    .add(listener);
        } else {
            throw new IllegalArgumentException("不存在相关主题：" + topic);
        }
    }

    @Override
    public boolean removeMessageExpiredListener(String topic, String listenerName) {
        if (expiredListenersMap.containsKey(topic) && !StringUtils.isEmpty(listenerName)) {
            return expiredListenersMap
                    .get(topic)
                    .removeIf(listener -> listener
                            .listenerName()
                            .equals(listenerName));
        }
        return false;
    }

    @Override
    public List<MessageExpiredListener<M>> removeAllMessageExpiredListener(String topic) {
        if (expiredListenersMap.containsKey(topic)) {
            return expiredListenersMap.remove(topic);
        }
        return null;
    }


    /**
     * <p>根据迭代器位置来使得一个元素过期，由于迭代器的弱一致性，多线程环境下可能会出现使用迭代器时
     * 发生插入\删除操作，由于该消息队列对于操作严格从队尾执行，因此对于插入修改能检测到，但是由于
     * 删除从队首进行，可能发生当迭代器获取下一个元素时为空，这时应该立即停止遍历，等待下一次</p>
     *
     * @param iterator 迭代器
     * @return void
     * @author Huang Yongxiang
     * @date 2022/3/21 11:42
     */
    public void expire(Iterator<MessageEntry<M>> iterator) throws ErrorExpiredException {
        if (iterator == null) {
            throw new ErrorExpiredException("过期失败，迭代器为空");
        }
        try {
            iterator.remove();
        } catch (UnsupportedOperationException e) {
            throw new ErrorExpiredException("过期失败，该迭代器不支持移除操作");
        } catch (IllegalStateException e) {
            throw new ErrorExpiredException("过期失败，可能发生删除操作");
        }
    }

    private void executeMessagePublishedListeners(String topic, M message) {
        if (publishedListenersMap.containsKey(topic)) {
            publishedListenersMap
                    .get(topic)
                    .forEach(listener -> {
                        try {
                            listener.onMessagePublished(message, messageQueues.get(topic));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        }
    }

    private void executeMessageExpiredListeners(String topic, M message) {
        if (expiredListenersMap.containsKey(topic)) {
            expiredListenersMap
                    .get(topic)
                    .forEach(listener -> {
                        try {
                            listener.onMessageExpired(message);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        }
    }

    @Override
    public boolean publishMessageNoBlock(M message, String topic) {
        if (!messageQueues.containsKey(topic)) {
            log.error("发布非阻塞消息失败，所要发布到的队列主题：{}不存在", topic);
            return false;
        }
        if (message == null) {
            log.error("禁止发布空消息");
            return false;
        }
        MessageEntry<M> messageEntry = new MessageEntry<>();
        messageEntry.setMessageId(IdGenerator.getNextIdAsLong());
        messageEntry.setMessage(message);
        messageEntry.setExpiringTime(defaultExpiringTime > 0 ? defaultExpiringTime + System.currentTimeMillis() : -1);
        if (messageQueues
                .get(topic)
                .publishMessage(messageEntry)) {
            executeMessagePublishedListeners(topic, message);
            return true;
        }
        return false;
    }

    public boolean publishMessageBlock(M message, String topic) {
        if (!messageQueues.containsKey(topic)) {
            log.error("发布阻塞消息失败，所要发布到的队列主题：{}不存在", topic);
            return false;
        }
        if (message == null) {
            log.error("禁止发布空消息");
            return false;
        }
        try {
            MessageEntry<M> messageEntry = new MessageEntry<>();
            messageEntry.setMessageId(IdGenerator.getNextIdAsLong());
            messageEntry.setMessage(message);
            messageEntry.setExpiringTime(defaultExpiringTime > 0 ? defaultExpiringTime + System.currentTimeMillis() : -1);
            if (messageQueues
                    .get(topic)
                    .publishMessageSync(messageEntry)) {
                executeMessagePublishedListeners(topic, message);
            } else {
                return false;
            }
        } catch (InterruptedException e) {
            log.warn("发布可阻塞消息发生异常，等待时被异常占用：{}", e.getMessage());
        }
        return false;
    }

    private void scheduleExpiringCheckSingleThread() {
        executorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ExpiringCheckSingleThread");
            thread.setDaemon(true);
            return thread;
        });
        executorService.scheduleAtFixedRate(() -> {
            for (Map.Entry<String, MessageQueue<MessageEntry<M>>> entry : messageQueues.entrySet()) {
                for (Iterator<MessageEntry<M>> it = entry
                        .getValue()
                        .iterator(); it.hasNext(); ) {
                    MessageEntry<M> message = it.next();
                    //如果达到过期时间则通知其过期
                    if (message.expiringTime >= 0 && message.expiringTime <= System.currentTimeMillis()) {
                        try {
                            log.debug("messageId：{}，消息内容：{},已过期", message.messageId, message.message);
                            expire(it);
                            executeMessageExpiredListeners(entry.getKey(), message.message);
                        } catch (ErrorExpiredException e) {
                            log.error("主题：{}，消息ID：{}过期失败：{}", entry.getKey(), message.getMessageId(), e.getMessage());
                        }
                    }
                }
            }
        }, 1, 1, TimeUnit.MILLISECONDS);
    }

    private void scheduleExpiringCheckTopicConcurrency() {
        executorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ExpiringCheckTopicConcurrencyThread");
            thread.setDaemon(true);
            return thread;
        });
        executorService.scheduleAtFixedRate(() -> {
            for (Map.Entry<String, MessageQueue<MessageEntry<M>>> entry : messageQueues.entrySet()) {
                Runnable work = () -> {
                    try {
                        for (Iterator<MessageEntry<M>> it = entry
                                .getValue()
                                .iterator(); it.hasNext(); ) {
                            MessageEntry<M> message = it.next();
                            //如果达到过期时间则通知其过期
                            if (message.expiringTime >= 0 && message.expiringTime <= System.currentTimeMillis()) {
                                try {
                                    log.debug("messageId：{}已过期", message.messageId);
                                    expire(it);
                                    executeMessageExpiredListeners(entry.getKey(), message.message);
                                } catch (ErrorExpiredException e) {
                                    log.error("主题：{}，消息ID：{}过期失败：{}", entry.getKey(), message.getMessageId(), e.getMessage());
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                };
                concurrencyThreads.submit(work);
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }


    @Setter
    @Accessors(chain = true)
    public static class Builder<M> {
        /**
         * 默认过期时间，对所有消息设置，-1则表示消息均为永久性消息，除非消费者取出，否则将会一直存在。谨慎使用！
         */
        private long defaultExpiringTime = -1;

        /**
         * 是否允许设置单个消息的过期时间
         */
        private boolean isAllowSetEntryExpired = false;

        /**
         * 允许的最大主题数
         */
        private int allowMaxTopicCount = 255;

        /**
         * 允许每个队列的最大消息数
         */
        private int allowMaxMessageCountPerQueue = 1000;

        /**
         * 过期监听器的策略
         */
        private ExpirationListenerPolicy listenerPolicy = ExpirationListenerPolicy.SINGLE_THREAD;

        private ThreadPoolExecutor concurrencyThreads;

        public Builder<M> setDefaultExpiringTime(long defaultExpiringTime, TimeUnit unit) {
            if (unit == TimeUnit.MICROSECONDS || unit == TimeUnit.NANOSECONDS) {
                throw new IllegalArgumentException("最小支持毫秒级");
            }
            this.defaultExpiringTime = unit.toMillis(defaultExpiringTime);
            return this;
        }

        /**
         * 当监听策略为TOPIC_CONCURRENCY时指定并发监听的线程池实现，默认min:3 max:10 keep:30S queue:10000
         *
         * @param concurrencyThreads 并发监听的线程池实现
         * @return com.example.autojob.skeleton.framework.mq.MessageQueueContext.Builder<M>
         * @author JingGe(* ^ ▽ ^ *)
         * @date 2023/6/28 9:30
         */
        public Builder<M> setConcurrencyThreads(ThreadPoolExecutor concurrencyThreads) {
            this.concurrencyThreads = concurrencyThreads;
            return this;
        }

        public <M1 extends M> MessageQueueContext<M1> build() {
            MessageQueueContext<M1> messageQueueContext = new MessageQueueContext<>();
            messageQueueContext.messageQueues = new ConcurrentHashMap<>(this.allowMaxTopicCount);
            messageQueueContext.isAllowSetEntryExpired = this.isAllowSetEntryExpired;
            messageQueueContext.allowMaxMessageCountPerQueue = this.allowMaxMessageCountPerQueue;
            messageQueueContext.defaultExpiringTime = this.defaultExpiringTime;
            messageQueueContext.allowMaxTopicCount = this.allowMaxTopicCount;
            messageQueueContext.listenerPolicy = this.listenerPolicy;
            if (this.listenerPolicy == ExpirationListenerPolicy.TOPIC_CONCURRENCY) {
                messageQueueContext.concurrencyThreads = new ThreadPoolExecutor(3, 10, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10000), new AbstractAutoJobPool.NamedThreadFactory("messageQueueExpire"));
            }
            return messageQueueContext;
        }

    }


}
