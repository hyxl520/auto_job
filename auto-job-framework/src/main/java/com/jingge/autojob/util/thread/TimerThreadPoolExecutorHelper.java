package com.jingge.autojob.util.thread;

import com.jingge.autojob.util.convert.StringUtils;
import com.jingge.autojob.util.cron.util.CronSequenceGenerator;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import net.jodah.expiringmap.internal.NamedThreadFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

/**
 * 该类对线程池{@link ThreadPoolExecutor}进行了封装，提供定时调整策略，默认策略：每天8点调整到C-10 M-20，每天21点调整到 C-3 M-5
 *
 * @author Huang Yongxiang
 * @date 2022-12-06 9:53
 * @email 1158055613@qq.com
 */
@Slf4j
public class TimerThreadPoolExecutorHelper implements ThreadPoolExecutorHelper {
    private ThreadPoolExecutor threadPoolExecutor;
    private List<TimerEntry> timerEntries;
    private volatile boolean isWorking;
    private volatile boolean isShutdown;

    private TimerThreadPoolExecutorHelper() {

    }

    public static Builder builder() {
        return new Builder();
    }

    private void worker() {
        if (isWorking) {
            return;
        }
        Thread worker = new Thread(() -> {
            do {
                try {
                    SyncHelper.sleepQuietly(1, TimeUnit.MILLISECONDS);
                    for (TimerEntry t : timerEntries) {
                        if (t.isReach(1000)) {
                            if (t.coreThreadCount >= 0) {
                                threadPoolExecutor.setCorePoolSize(t.coreThreadCount);
                            }
                            if (t.maximizeThreadCount > 0) {
                                threadPoolExecutor.setMaximumPoolSize(t.maximizeThreadCount);
                            }
                            if (t.keepAliveTime > 0) {
                                threadPoolExecutor.setKeepAliveTime(t.keepAliveTime, TimeUnit.MILLISECONDS);
                            }
                            t.refresh();
                            if (t.triggerListener != null) {
                                t.triggerListener.onTrigger(t.cronExpression, threadPoolExecutor);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } while (!isShutdown);
        });
        worker.setDaemon(true);
        worker.setName("timerThreadPoolWorker");
        isWorking = true;
        isWorking = true;
        worker.start();
    }

    @Override
    public Future<?> submit(Runnable runnable) {
        return threadPoolExecutor.submit(runnable);
    }

    @Override
    public <V> Future<V> submit(Callable<V> callable) {
        return threadPoolExecutor.submit(callable);
    }

    @Override
    public void shutdown() {
        threadPoolExecutor.shutdown();
        isShutdown = true;
    }

    @Override
    public List<Runnable> shutdownNow() {
        List<Runnable> runnableList = threadPoolExecutor.shutdownNow();
        isShutdown = true;
        return runnableList;
    }

    @Override
    public void setThreadFactory(ThreadFactory threadFactory) {
        threadPoolExecutor.setThreadFactory(threadFactory);
    }

    public void addTimerEntry(TimerEntry timerEntry) {
        timerEntries.add(timerEntry);
    }

    public int getCoreThreadCount() {
        return threadPoolExecutor.getCorePoolSize();
    }

    public int getMaximizeThreadCount() {
        return threadPoolExecutor.getMaximumPoolSize();
    }

    public long getKeepAliveTime(TimeUnit unit) {
        return threadPoolExecutor.getKeepAliveTime(unit);
    }

    @ToString
    public static class TimerEntry {
        private long triggeringTime;
        /**
         * cron-like表达式
         */
        private final String cronExpression;
        /**
         * 核心线程数，<0不作调整
         */
        private final int coreThreadCount;
        /**
         * 最大线程数，<0不作调整
         */
        private final int maximizeThreadCount;
        /**
         * 核心线程的空闲存活时长，<0不作调整
         */
        private long keepAliveTime;
        /**
         * 触发监听器
         */
        private TriggerListener triggerListener;

        public TimerEntry setTriggerListener(TriggerListener triggerListener) {
            this.triggerListener = triggerListener;
            return this;
        }

        public TimerEntry(String cronExpression, int coreThreadCount, int maximizeThreadCount, long keepAliveTime, TimeUnit unit) {
            this(cronExpression, coreThreadCount, maximizeThreadCount, null, keepAliveTime, unit);
        }

        public TimerEntry(String cronExpression, int coreThreadCount, int maximizeThreadCount, TriggerListener listener, long keepAliveTime, TimeUnit unit) {
            if (!CronSequenceGenerator.isValidExpression(cronExpression)) {
                throw new IllegalArgumentException("cron表达式不合法");
            }
            this.cronExpression = cronExpression;
            this.coreThreadCount = coreThreadCount;
            this.triggerListener = listener;
            this.maximizeThreadCount = maximizeThreadCount;
            if (unit != null && keepAliveTime > 0) {
                this.keepAliveTime = unit.toMillis(keepAliveTime);
            }
        }


        public boolean isReach(long interval) {
            return Math.abs(triggeringTime - System.currentTimeMillis()) <= interval;
        }

        public void refresh() {
            try {
                Thread.sleep(1000 - triggeringTime % 1000);
            } catch (InterruptedException ignored) {
            }
            triggeringTime = new CronSequenceGenerator(cronExpression)
                    .next(new Date())
                    .getTime();
        }
    }

    public interface TriggerListener {
        void onTrigger(String cronExpression, ThreadPoolExecutor threadPoolExecutor);
    }

    @Setter
    @Accessors(chain = true)
    public static class Builder {
        private final List<TimerEntry> timerEntries = new ArrayList<>();
        /**
         * 初始核心线程数，默认值3
         */
        public int initialCoreTreadCount = 3;
        /**
         * 初始最大线程数，默认值5
         */
        public int initialMaximizeTreadCount = 5;
        /**
         * 默认核心线程最大空闲生存时长，默认60秒
         */
        public long initialCoreTreadKeepAliveTime = 60000;
        /**
         * 线程工厂，默认使用：timerThread-%d格式
         */
        private ThreadFactory threadFactory = new NamedThreadFactory("timerThread-%d");
        /**
         * 任务队列容量
         */
        private int taskQueueCapacity = 100;
        /**
         * 线程池，配置了后只需要配置调整策略即可
         */
        public ThreadPoolExecutor threadPoolExecutor;

        /**
         * 添加一个调整策略
         *
         * @param timerEntry 调整策略
         * @return com.sccl.common.thread.TimerThreadPoolExecutor.Builder
         * @author Huang Yongxiang
         * @date 2022/12/6 10:43
         */
        public Builder addTimerEntry(TimerEntry timerEntry) {
            if (timerEntry != null) {
                timerEntries.add(timerEntry);
            }
            return this;
        }

        /**
         * 添加一个调整策略
         *
         * @param cronExpression      触发的cron表达式，<0不作调整
         * @param coreThreadCount     触发时调整到的核心线程数，<0不作调整
         * @param maximizeThreadCount 触发时调整到的最大线程数，<0不作调整
         * @param keepAliveTime       触发调整到的空闲核心线程存活时长，<0不作调整
         * @param unit                时间单位，null默认秒
         * @return com.sccl.common.thread.TimerThreadPoolExecutor.Builder
         * @author Huang Yongxiang
         * @date 2022/12/6 10:31
         */
        public Builder addTimerEntry(String cronExpression, int coreThreadCount, int maximizeThreadCount, long keepAliveTime, TimeUnit unit) {
            if (!StringUtils.isEmpty(cronExpression)) {
                timerEntries.add(new TimerEntry(cronExpression, coreThreadCount, maximizeThreadCount, keepAliveTime, unit));
            }
            return this;
        }

        public Builder setInitialCoreTreadKeepAliveTime(long initialCoreTreadKeepAliveTime, TimeUnit unit) {
            this.initialCoreTreadKeepAliveTime = unit.toMillis(initialCoreTreadKeepAliveTime);
            return this;
        }

        public TimerThreadPoolExecutorHelper build() {
            TimerThreadPoolExecutorHelper timerThreadPoolExecutorHelper = new TimerThreadPoolExecutorHelper();
            if (timerEntries.size() == 0) {
                timerEntries.addAll(Arrays.asList(new TimerEntry("0 0 8 * * ?", 10, 20, -1, null), new TimerEntry("0 0 21 * * ?", 3, 5, -1, null)));
            }
            timerThreadPoolExecutorHelper.timerEntries = timerEntries;
            timerEntries.forEach(TimerEntry::refresh);
            timerThreadPoolExecutorHelper.threadPoolExecutor = threadPoolExecutor == null ? new ThreadPoolExecutor(initialCoreTreadCount, initialMaximizeTreadCount, initialCoreTreadKeepAliveTime, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(taskQueueCapacity), threadFactory) : threadPoolExecutor;
            timerThreadPoolExecutorHelper.worker();
            return timerThreadPoolExecutorHelper;
        }
    }

    public static void main(String[] args) {
        TimerEntry timerEntry = new TimerEntry("0 33 16 * * ?", 0, 1, -1, null);
        timerEntry.setTriggerListener((cronExpression, threadPoolExecutor1) -> {
            System.out.println("触发成功，" + threadPoolExecutor1.getCorePoolSize() + ";" + threadPoolExecutor1.getMaximumPoolSize());
        });
        TimerThreadPoolExecutorHelper executor = TimerThreadPoolExecutorHelper
                .builder()
                .addTimerEntry(timerEntry)
                .build();
        executor.submit(() -> {
            System.out.println("hello");

        });
        SyncHelper.sleepQuietly(5, TimeUnit.MINUTES);
    }


}
