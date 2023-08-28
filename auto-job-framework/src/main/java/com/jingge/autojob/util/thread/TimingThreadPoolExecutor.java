package com.jingge.autojob.util.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 重写线程池，拓展记录任务的平均执行时长
 *
 * @Auther Huang Yongxiang
 * @Date 2022/03/28 15:24
 */
public class TimingThreadPoolExecutor extends ThreadPoolExecutor {
    private static final Logger logger = LoggerFactory.getLogger(TimingThreadPoolExecutor.class);
    private final ThreadLocal<Long> startTime = new ThreadLocal<>();
    /**
     * 每个任务的平均执行时长
     */
    private final AtomicLong averageTime = new AtomicLong(0);
    /**
     * 已执行完的任务总数
     */
    private final AtomicInteger callCount = new AtomicInteger(0);
    private final AtomicLong startCount = new AtomicLong(0);
    private final AtomicLong runningCount = new AtomicLong(0);
    /**
     * 总计执行时间
     */
    private final AtomicLong totalTime = new AtomicLong(0);
    private boolean allowTry = false;
    private int reTryTimes;

    public TimingThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public TimingThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public TimingThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public TimingThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        startTime.set(System.currentTimeMillis());
        startCount.incrementAndGet();
        runningCount.set(startCount.get() - callCount.get());
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        callCount.incrementAndGet();
        totalTime.addAndGet(System.currentTimeMillis() - startTime.get());
        averageTime.set(totalTime.get() / callCount.get());
        runningCount.set(startCount.get() - callCount.get());
    }

    @Override
    protected void terminated() {
        super.terminated();
        logger.info("线程池即将退出，总执行任务数：{}，总执行时长：{}ms，每个任务平均执行时长：{}ms", callCount.get(), totalTime.get(), averageTime.get());
    }

    public boolean update(int corePoolSize, int maxPoolSize) {
        maxPoolSize = Math.max(maxPoolSize, corePoolSize);
        if (corePoolSize > getActiveCount() && corePoolSize > 0 && corePoolSize != getCorePoolSize() && maxPoolSize != getMaximumPoolSize()) {
            setCorePoolSize(corePoolSize);
            setMaximumPoolSize(maxPoolSize);
            return true;
        }
        return false;
    }

    public AtomicLong getAverageTime() {
        return averageTime;
    }

    public AtomicInteger getCallCount() {
        return callCount;
    }

    public AtomicLong getTotalTime() {
        return totalTime;
    }

    public AtomicLong getStartCount() {
        return startCount;
    }

    public AtomicLong getRunningCount() {
        return runningCount;
    }

    public void setAllowTry(boolean allowTry) {
        this.allowTry = allowTry;
    }

    public void setReTryTimes(int reTryTimes) {
        this.reTryTimes = reTryTimes;
    }

    public boolean isAllowTry() {
        return allowTry;
    }

    public int getReTryTimes() {
        return reTryTimes;
    }

    public static class TaskEntry {
        private Runnable runnable;
        private Callable<?> callable;
    }
}
