package com.jingge.autojob.util.thread;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.*;

/**
 * 异步任务构建器，提供单例模式和多例模式，调用静态方法instance()和使用静态任务执行方法
 * 均是使用的单例对象，单例模式下提交的任务将是串行执行，如果你不能保证你执行的任务都能在可接受的时间内完成，
 * 请优先使用build()方法构建多例来运行
 *
 * @Auther Huang Yongxiang
 * @Date 2022/03/22 9:39
 */
@Slf4j
public class ScheduleTaskUtil {
    private static final String DEFAULT_THREAD_NAME = "scheduleThread";
    private ScheduledExecutorService executorService;


    private ScheduleTaskUtil() {
    }

    public static ScheduleTaskUtil build() {
        ScheduleTaskUtil scheduleTaskUtil = new ScheduleTaskUtil();
        scheduleTaskUtil.executorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(() -> {
                try {
                    runnable.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, DEFAULT_THREAD_NAME + System.currentTimeMillis());
            thread.setDaemon(true);
            return thread;
        });
        return scheduleTaskUtil;
    }

    /**
     * 构建一个异步任务调度工具实例
     *
     * @param isDaemon   是否是守护线程
     * @param threadName 线程名
     * @return com.example.autojob.util.thread.ScheduleTaskUtil
     * @author Huang Yongxiang
     * @date 2022/3/22 10:46
     */
    public static ScheduleTaskUtil build(boolean isDaemon, String threadName) {
        ScheduleTaskUtil scheduleTaskUtil = new ScheduleTaskUtil();
        scheduleTaskUtil.executorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(() -> {
                try {
                    runnable.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, threadName);
            thread.setDaemon(isDaemon);
            return thread;
        });
        return scheduleTaskUtil;
    }

    public void shutdown() {
        executorService.shutdown();
    }

    public List<Runnable> shutdownNow() {
        return executorService.shutdownNow();
    }

    public ScheduledFuture<Object> EOneTimeTask(Callable<Object> runnable, long delay, TimeUnit unit) {
        if (runnable == null || delay < 0 || unit == null) {
            throw new NullPointerException();
        }
        return executorService.schedule(runnable, delay == 0 ? 1 : delay, unit);
    }

    public void EFixedRateTask(Runnable runnable, long delay, long rate, TimeUnit unit) {
        if (runnable == null || delay < 0 || unit == null || rate <= 0) {
            throw new NullPointerException();
        }
        executorService.scheduleAtFixedRate(runnable, delay == 0 ? 1 : delay, rate, unit);
    }

    public void EFixedRateDelayTask(Runnable runnable, long delay, long nextDaly, TimeUnit unit) {
        if (runnable == null || delay < 0 || unit == null || nextDaly <= 0) {
            throw new NullPointerException();
        }
        executorService.scheduleWithFixedDelay(runnable, delay, nextDaly, unit);
    }


    public static ScheduledExecutorService instance() {
        return ThreadHolder.executorService;
    }


    /**
     * <p>执行一次性任务，如果该任务是一个不可退出任务，如死循环，则在单例模式下后面的任务几乎永远不会执行，
     * 因此使用静态方法请保证你所执行的任务不会发生死循环。简单来说，该方法提交的任务实际是串行进行，但是
     * 独立于主线程外</p>
     *
     * @param runnable 任务
     * @param delay    启动前延迟
     * @param unit     delay的时间单位
     * @return void
     * @author Huang Yongxiang
     * @date 2022/3/22 10:01
     */
    public static ScheduledFuture<Object> oneTimeTask(Callable<Object> runnable, long delay, TimeUnit unit) {
        if (runnable == null || unit == null) {
            throw new NullPointerException();
        }
        return instance().schedule(runnable, delay <= 0 ? 1 : delay, unit);
    }

    public static void oneTimeTask(Runnable runnable, long delay, TimeUnit unit) {
        if (runnable == null || unit == null) {
            throw new NullPointerException();
        }
        instance().schedule(() -> {
            runnable.run();
            return null;
        }, delay <= 0 ? 1 : delay, unit);
    }

    private static class ThreadHolder {
        private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(() -> {
                try {
                    runnable.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, DEFAULT_THREAD_NAME);
            thread.setDaemon(true);
            return thread;
        });
    }

}
