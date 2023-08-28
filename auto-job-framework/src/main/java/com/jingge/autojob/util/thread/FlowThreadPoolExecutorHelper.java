package com.jingge.autojob.util.thread;

import com.jingge.autojob.util.system.SystemUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 动态线程池，实现的功能有：
 * <li>1、采用构建者模式，通过通用参数构建线程池</li>
 * <li>2、拒绝处理，提交失败的任务将会放入溢出队列，并由一个线程提交消费</li>
 * <li>3、动态调节，实时监测线程池的流量，并且动态更新核心线程数和最大线程数</li>
 *
 * @Auther Huang Yongxiang
 * @Date 2022/03/25 9:13
 */
@Slf4j
@Getter
public class FlowThreadPoolExecutorHelper implements ThreadPoolExecutorHelper {
    private int corePoolSize;
    private int queueCapacity;
    private long keepAliveTime;
    private int maxPoolSize;
    private boolean isOverflowTaskStart = false;
    private boolean isTrafficMonitorStart = false;
    public boolean isStop = false;
    private double allowMaxResponseTime;
    final Object lock = new Object();
    /**
     * 具备任务时间统计的线程池
     */
    private TimingThreadPoolExecutor executor;
    /**
     * 流量管理器
     */
    private TrafficMonitor trafficMonitor;
    /**
     * 提交任务时失败时存放的任务
     */
    BlockingQueue<Object> overflowTask;

    private Thread submitOverflowThread;

    private FlowThreadPoolExecutorHelper() {
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 该构建者使用ThreadPoolExecutor原始风格构建，必要参数均设置有默认值
     *
     * @return com.example.autojob.util.thread.ThreadPoolExecutorHelper.ClassicBuilder
     * @author Huang Yongxiang
     * @date 2022/4/18 11:14
     */
    public static ClassicBuilder classicBuilder() {
        return new ClassicBuilder();
    }


    @Override
    public Future<?> submit(Runnable runnable) throws RejectedExecutionException {
        if (runnable == null) {
            log.error("提交的任务为空");
            return null;
        }
        if (!isOverflowTaskStart) {
            synchronized (lock) {
                if (!isOverflowTaskStart) {
                    scheduleSubmitOverflowTask();
                    isOverflowTaskStart = true;
                }
            }
        }
        if (!isTrafficMonitorStart) {
            synchronized (lock) {
                if (!isTrafficMonitorStart) {
                    trafficMonitor.worker(executor);
                    isTrafficMonitorStart = true;
                }
            }
        }
        trafficMonitor.callCount.incrementAndGet();
        try {
            return executor.submit(runnable);
        } catch (RejectedExecutionException e) {
            log.debug("提交任务时发生异常：{}，将把任务存入溢出队列", e.getMessage());
            boolean flag = overflowTask.offer(runnable);
            if (!flag) {
                log.error("任务插入溢出队列失败，可能是溢出任务数已经达到了2^31");
                throw e;
            }
        }
        return null;
    }

    @Override
    public <V> Future<V> submit(Callable<V> callable) {
        if (callable == null) {
            log.error("提交的任务为空");
            return null;
        }
        if (!isOverflowTaskStart) {
            synchronized (lock) {
                if (!isOverflowTaskStart) {
                    scheduleSubmitOverflowTask();
                    isOverflowTaskStart = true;
                }
            }
        }
        if (!isTrafficMonitorStart) {
            synchronized (lock) {
                if (!isTrafficMonitorStart) {
                    trafficMonitor.worker(executor);
                    isTrafficMonitorStart = true;
                }
            }
        }
        trafficMonitor.callCount.incrementAndGet();
        try {
            return executor.submit(callable);
        } catch (RejectedExecutionException e) {
            log.debug("提交任务时发生异常：{}，将把任务存入溢出队列", e.getMessage());
            boolean flag = overflowTask.offer(callable);
            if (!flag) {
                log.error("任务插入溢出队列失败，可能是溢出任务数已经达到了2^31");
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public void shutdown() {
        executor.shutdown();
        trafficMonitor.isStop = true;
        isStop = true;
    }

    @Override
    public List<Runnable> shutdownNow() {
        List<Runnable> runnableList = executor.shutdownNow();
        trafficMonitor.isStop = true;
        trafficMonitor.trafficUpdateThread.interrupt();
        isStop = true;
        submitOverflowThread.interrupt();
        return runnableList;
    }

    @Override
    public void setThreadFactory(ThreadFactory threadFactory) {
        executor.setThreadFactory(threadFactory);
    }

    public void update(int corePoolSize, int maxPoolSize) {
        if (corePoolSize > 0 && maxPoolSize > 0 && corePoolSize > executor.getActiveCount() && maxPoolSize >= corePoolSize) {
            this.executor.setCorePoolSize(corePoolSize);
            this.executor.setMaximumPoolSize(maxPoolSize);
        }
    }

    public static void main(String[] args) {
        Runnable runnable = () -> {
            Random random = new Random();
            long sleep = random.nextInt(5000) % (3000 - 500 + 1) + 500;
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        FlowThreadPoolExecutorHelper executorHelper = FlowThreadPoolExecutorHelper
                .builder()
                .setCostTimePerTask(1.5)
                .setMinTaskCountPerSecond(0)
                .setMaxTaskCountPerSecond(1200)
                .setAllowMaxResponseTime(1.5)
                .setThreadPoolType(ThreadPoolType.IO_INTENSIVE)
                .setAllowMaxCoreThreadCount(100)
                .setAllowMaxThreadCount(100)
                .setAllowMinThreadCount(5)
                .setAllowMinCoreThreadCount(1)
                .setAllowUpdate(true)
                .setUpdateType(UpdateType.USE_FLOW)
                .setTrafficListenerCycle(5, TimeUnit.SECONDS)
                .build();
        for (int i = 0; i < 100; i++) {
            try {
                executorHelper.submit(runnable);
            } catch (RejectedExecutionException e) {
                log.error("线程池已满，提交任务失败");
            }
        }
        System.out.println("100个任务提交完成");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < 1000; i++) {
            try {
                executorHelper.submit(runnable);
            } catch (RejectedExecutionException e) {
                log.error("线程池已满，提交任务失败");
            }
        }
        System.out.println("1000个任务提交完成");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < 50; i++) {
            try {
                executorHelper.submit(runnable);
            } catch (RejectedExecutionException e) {
                log.error("线程池已满，提交任务失败");
            }
        }
        System.out.println("50个任务提交完成");
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("总执行任务数：" + executorHelper.executor.getCallCount());

    }


    private void scheduleSubmitOverflowTask() {
        if (isOverflowTaskStart) {
            return;
        }
        submitOverflowThread = new Thread(() -> {
            do {
                try {
                    SyncHelper.sleepQuietly(1, TimeUnit.MILLISECONDS);
                    Object task = null;
                    do {
                        SyncHelper.sleepQuietly(1, TimeUnit.MILLISECONDS);
                        task = overflowTask.peek();
                    } while (task == null);
                    try {
                        if (task instanceof Runnable) {
                            executor.submit((Runnable) task);
                        } else if (task instanceof Callable) {
                            executor.submit((Callable<?>) task);
                        }
                        //删除元素
                        overflowTask.take();
                        //log.info("提交溢出任务成功");
                    } catch (RejectedExecutionException ignored) {
                        //log.warn("此时线程池依然无空闲线程，等待下一次尝试");
                    } catch (InterruptedException ignored) {
                        //log.error("等待溢出任务队列是被异常占用");
                    }
                } catch (Exception e) {
                    if (!isStop) {
                        e.printStackTrace();
                    }
                }
            } while (!isStop);
        });
        submitOverflowThread.setDaemon(true);
        submitOverflowThread.setName("submitOverflowThread");
        submitOverflowThread.start();
    }

    /**
     * 流量管理器，存储特定时间段的流量情况，动态更新线程池参数
     */
    public static class TrafficMonitor {
        /**
         * 每个任务平均执行时长
         */
        private final AtomicLong averageTime;
        /**
         * 指定时间区间的任务流量
         */
        private final AtomicInteger callCount;
        /**
         * 3个周期内的最大流量
         */
        private final AtomicInteger maxCallCount;
        /**
         * 计数器
         */
        private final AtomicInteger counter;

        /**
         * 允许最大的核心线程数，计算值不会超过该值
         */
        private int allowMaxCoreThreadCount;

        /**
         * 允许最大的线程数，计算值不会超过该值
         */
        private int allowMaxThreadCount;

        private double allowMaxResponseTime;

        private long queueCapacity;

        /**
         * 允许更新的最小线程数
         */
        private int allowMinCoreThreadCount;
        /**
         * 允许更新的最大线程数
         */
        private int allowMinThreadCount;

        /**
         * 流量监控周期：s
         */
        private long trafficListenerCycle;

        /**
         * 变化的阈值0-1
         */
        private double updateThreshold;

        /**
         * 线程池类型
         */
        private ThreadPoolType threadPoolType;

        /**
         * 是否允许更新
         */
        private boolean allowUpdate;

        /**
         * 更新类型
         */
        private UpdateType updateType = UpdateType.USE_FLOW;

        private Thread trafficUpdateThread;

        private boolean isStop = false;


        public TrafficMonitor() {
            averageTime = new AtomicLong(0);
            callCount = new AtomicInteger(0);
            maxCallCount = new AtomicInteger(0);
            counter = new AtomicInteger(0);
        }


        /**
         * 判断当前流量是否达到阈值
         *
         * @return int 0-没达到 1-达到且超过 -1-达到且低低于
         * @author Huang Yongxiang
         * @date 2022/8/22 10:11
         */
        private int isReachThreshold(long coreThreadCount, long queueCapacity) {
            double realResponseTime = queueCapacity * 1.0 / (coreThreadCount * 1.0 / (averageTime.get() / 1000.0));
            double change = realResponseTime - allowMaxResponseTime;
            double changePercent = (Math.abs(change) == 0 ? 0 : (Math.abs(change) * 1.0) / allowMaxResponseTime);
            if (changePercent > updateThreshold) {
                //log.warn("最近{}秒流量：{}，实际响应时间：{}，期望最大响应时间：{}，当前变化率：{}%", trafficListenerCycle, callCount.get(), realResponseTime, allowMaxResponseTime, (change < 0 ? -1 : 1) * changePercent * 100);
                if (change < 0) {
                    return -1;
                } else if (change > 0) {
                    return 1;
                }
            }
            return 0;
        }

        public void worker(TimingThreadPoolExecutor executor) {
            trafficUpdateThread = new Thread(() -> {
                do {
                    try {
                        SyncHelper.sleepQuietly(trafficListenerCycle * 1000, TimeUnit.MILLISECONDS);
                        if (threadPoolType == ThreadPoolType.IO_INTENSIVE) {
                            //log.info("线程池类型：IO密集型，活动线程数：{},最近{}秒流量：{}，平均每个任务执行时长：{}ms，最近3" + "个周期最大流量：{}", executor.getRunningCount().get(), trafficListenerCycle, callCount.get(), averageTime.get(), maxCallCount.get());
                        } else {
                            double use = SystemUtil.getSystemCpuLoad();
                            while (use < 0) {
                                use = SystemUtil.getSystemCpuLoad();
                                SyncHelper.sleepQuietly(500, TimeUnit.MILLISECONDS);
                            }
                            log.debug("线程池类型：CPU密集型，最近{}秒流量：{}，最近3个周期最大流量：{}，CPU核心数：{}，CPU使用率：{}%", trafficListenerCycle, callCount.get(), maxCallCount.get(), SystemUtil.getSystemCpuCount(), String.format("%.2f", use * 100));
                        }
                        /*=================计算3个周期最大流量=================>*/
                        counter.incrementAndGet();
                        if (counter.get() == 4) {
                            maxCallCount.set(0);
                            counter.set(0);
                        } else {
                            maxCallCount.set(Math.max(callCount.get(), maxCallCount.get()));
                        }
                        averageTime.set(executor
                                .getAverageTime()
                                .get());
                        /*=======================Finished======================<*/

                        /*=================尝试更新=================>*/
                        int flag = isReachThreshold(executor.getCorePoolSize(), executor
                                .getQueue()
                                .size());
                        if (allowUpdate && flag != 0) {
                            updateType.update(executor, this);
                            averageTime.set(0);
                        }
                        callCount.set(0);
                        /*=======================Finished======================<*/
                    } catch (Exception e) {
                        if (!isStop) {
                            e.printStackTrace();
                        }
                    }
                } while (!isStop);
            });
            trafficUpdateThread.setName("trafficUpdateThread");
            trafficUpdateThread.setDaemon(true);
            trafficUpdateThread.start();
        }
    }

    @Setter
    @Accessors(chain = true)
    public static class Builder {
        /**
         * 每秒最大处理任务数
         */
        private int maxTaskCountPerSecond = 500;

        /**
         * 每秒最小处理任务数
         */
        private int minTaskCountPerSecond = 100;

        /**
         * 80%的情况下每秒最大处理任务数
         */
        private int eightyPercentTaskCountPerSecond = -1;

        /**
         * 每项任务花费时间：秒
         */
        private double costTimePerTask = 0.1;

        /**
         * 允许的最大响应时间：秒
         */
        private double allowMaxResponseTime = 5;

        /**
         * 最大空闲时长：秒，如果流量降低，当一个线程这个时间内没有收到任务，就会退出直到总数目为核心线程数
         */
        private long keepAliveTime = 60;

        /**
         * 允许最大的核心线程数，计算值不会超过该值
         */
        private int allowMaxCoreThreadCount = 10;

        /**
         * 允许最大的线程数，计算值不会超过该值
         */
        private int allowMaxThreadCount = 10;

        /**
         * 允许更新的最小线程数
         */
        private int allowMinCoreThreadCount = 1;
        /**
         * 允许更新的最大线程数
         */
        private int allowMinThreadCount = 5;

        /**
         * 是否允许根据流量动态更新
         */
        private boolean allowUpdate = true;

        private ThreadPoolType threadPoolType = ThreadPoolType.IO_INTENSIVE;

        /**
         * 流量监控周期：s
         */
        private long trafficListenerCycle = 5;

        /**
         * 变化的阈值0-1
         */
        private double updateThreshold = 0.5;

        /**
         * 更新类型
         */
        private UpdateType updateType = UpdateType.USE_FLOW;

        private ThreadFactory threadFactory;


        private Builder() {
        }


        private boolean paramsCheck() {
            if (maxTaskCountPerSecond < minTaskCountPerSecond || minTaskCountPerSecond < 0) {
                log.error("每秒处理任务数非法");
                return false;
            }
            if (costTimePerTask <= 0) {
                log.error("每项任务花费时长只能为正数");
                return false;
            }
            if (allowMaxResponseTime <= 0) {
                log.error("允许的最大响应时间应该为正数");
                return false;
            }
            if (eightyPercentTaskCountPerSecond > 0) {
                log.debug("此时将使用eightyPercentTaskCountPerSecond参数计算");
            } else {
                log.debug("此时将使用maxTaskCountPerSecond和minTaskCountPerSecond参数计算");
            }
            if (trafficListenerCycle <= 0) {
                return false;
            }
            return !(updateThreshold < 0);
        }

        private int corePoolSize() {
            int res;
            if (eightyPercentTaskCountPerSecond < 0) {
                int minCount = (int) (minTaskCountPerSecond * costTimePerTask);
                int maxCount = (int) (maxTaskCountPerSecond * costTimePerTask);
                res = (minCount + maxCount) / 2;
            } else {
                res = (int) (eightyPercentTaskCountPerSecond * costTimePerTask);
            }
            if (allowMaxCoreThreadCount > 0) {
                res = Math.min(res, allowMaxCoreThreadCount);
            }
            return res == 0 ? 1 : res;
        }

        private int queueCapacity() {
            int res = (int) ((corePoolSize() / costTimePerTask) * allowMaxResponseTime);
            return Math.max(res, 5);
        }

        private int maxPoolSize() {
            int res = (int) ((maxTaskCountPerSecond - queueCapacity()) * costTimePerTask);
            if (allowMaxThreadCount > 0) {
                res = Math.min(res, allowMaxThreadCount);
            }
            return Math.max(res, corePoolSize());
        }

        public Builder setTrafficListenerCycle(long trafficListenerCycle, TimeUnit unit) {
            this.trafficListenerCycle = unit.toSeconds(trafficListenerCycle);
            return this;
        }

        public FlowThreadPoolExecutorHelper build() {
            if (threadPoolType == ThreadPoolType.IO_INTENSIVE) {
                return getIOHelper();
            }
            return getCPUHelper();
        }

        private FlowThreadPoolExecutorHelper getIOHelper() {
            if (!paramsCheck()) {
                throw new IllegalArgumentException("参数非法，无法构建");
            }
            FlowThreadPoolExecutorHelper flowThreadPoolExecutorHelper = new FlowThreadPoolExecutorHelper();
            /*=================基础参数=================>*/
            flowThreadPoolExecutorHelper.corePoolSize = corePoolSize();
            flowThreadPoolExecutorHelper.keepAliveTime = keepAliveTime;
            flowThreadPoolExecutorHelper.queueCapacity = queueCapacity();
            flowThreadPoolExecutorHelper.maxPoolSize = maxPoolSize();
            flowThreadPoolExecutorHelper.overflowTask = new LinkedBlockingQueue<>();
            if (threadFactory != null) {
                flowThreadPoolExecutorHelper.executor = new TimingThreadPoolExecutor(flowThreadPoolExecutorHelper.corePoolSize, flowThreadPoolExecutorHelper.maxPoolSize, keepAliveTime, TimeUnit.SECONDS, new LinkedBlockingQueue<>(flowThreadPoolExecutorHelper.queueCapacity), threadFactory);
            } else {
                flowThreadPoolExecutorHelper.executor = new TimingThreadPoolExecutor(flowThreadPoolExecutorHelper.corePoolSize, flowThreadPoolExecutorHelper.maxPoolSize, keepAliveTime, TimeUnit.SECONDS, new LinkedBlockingQueue<>(flowThreadPoolExecutorHelper.queueCapacity));
            }
            /*=======================Finished======================<*/

            /*=================流量管理器参数=================>*/
            TrafficMonitor trafficMonitor = new TrafficMonitor();
            trafficMonitor.allowMinThreadCount = allowMinThreadCount;
            trafficMonitor.updateType = updateType;
            trafficMonitor.allowMaxThreadCount = allowMaxThreadCount;
            trafficMonitor.allowMinCoreThreadCount = allowMinCoreThreadCount;
            trafficMonitor.allowMaxCoreThreadCount = allowMaxCoreThreadCount;
            trafficMonitor.allowMaxResponseTime = allowMaxResponseTime;
            trafficMonitor.allowUpdate = allowUpdate;
            trafficMonitor.trafficListenerCycle = trafficListenerCycle;
            trafficMonitor.updateThreshold = updateThreshold;
            trafficMonitor.threadPoolType = ThreadPoolType.IO_INTENSIVE;
            flowThreadPoolExecutorHelper.trafficMonitor = trafficMonitor;
            /*=======================Finished======================<*/
            log.debug("IO密集型线程池：核心线程数：{}，最大线程数：{}，队列最大长度：{}，线程最大空闲时长：{}秒", flowThreadPoolExecutorHelper.corePoolSize, flowThreadPoolExecutorHelper.maxPoolSize, flowThreadPoolExecutorHelper.queueCapacity, keepAliveTime);
            return flowThreadPoolExecutorHelper;
        }

        private FlowThreadPoolExecutorHelper getCPUHelper() {
            //获取CPU核心数
            int cpuCount = 0;
            do {
                SyncHelper.sleepQuietly(100, TimeUnit.MILLISECONDS);
                cpuCount = SystemUtil.getSystemCpuCount();
            } while (cpuCount <= 0);

            FlowThreadPoolExecutorHelper flowThreadPoolExecutorHelper = new FlowThreadPoolExecutorHelper();
            /*=================基础参数=================>*/
            flowThreadPoolExecutorHelper.corePoolSize = cpuCount + 1;
            flowThreadPoolExecutorHelper.keepAliveTime = keepAliveTime;
            flowThreadPoolExecutorHelper.queueCapacity = Math.max(5, (int) (((cpuCount + 1) / costTimePerTask) * allowMaxResponseTime));
            flowThreadPoolExecutorHelper.overflowTask = new LinkedBlockingQueue<>();
            flowThreadPoolExecutorHelper.maxPoolSize = cpuCount + 1;
            flowThreadPoolExecutorHelper.allowMaxResponseTime = allowMaxResponseTime;
            if (threadFactory != null) {
                flowThreadPoolExecutorHelper.executor = new TimingThreadPoolExecutor(flowThreadPoolExecutorHelper.corePoolSize, flowThreadPoolExecutorHelper.maxPoolSize, keepAliveTime, TimeUnit.SECONDS, new LinkedBlockingQueue<>(flowThreadPoolExecutorHelper.queueCapacity), threadFactory);
            } else {
                flowThreadPoolExecutorHelper.executor = new TimingThreadPoolExecutor(flowThreadPoolExecutorHelper.corePoolSize, flowThreadPoolExecutorHelper.maxPoolSize, keepAliveTime, TimeUnit.SECONDS, new LinkedBlockingQueue<>(flowThreadPoolExecutorHelper.queueCapacity));
            }
            /*=======================Finished======================<*/

            /*=================流浪管理器参数=================>*/
            TrafficMonitor trafficMonitor = new TrafficMonitor();
            trafficMonitor.allowMaxCoreThreadCount = allowMaxCoreThreadCount;
            trafficMonitor.allowMinCoreThreadCount = allowMinCoreThreadCount;
            trafficMonitor.allowMaxThreadCount = allowMaxThreadCount;
            trafficMonitor.updateType = updateType;
            trafficMonitor.allowMinThreadCount = allowMinThreadCount;
            trafficMonitor.allowMaxResponseTime = allowMaxResponseTime;
            trafficMonitor.allowUpdate = allowUpdate;
            trafficMonitor.updateThreshold = updateThreshold;
            trafficMonitor.trafficListenerCycle = trafficListenerCycle;
            trafficMonitor.threadPoolType = ThreadPoolType.CPU_INTENSIVE;
            flowThreadPoolExecutorHelper.trafficMonitor = trafficMonitor;
            /*=======================Finished======================<*/

            log.info("CPU密集型线程池-将不允许动态更新：核心线程数：{}，最大线程数：{}，队列最大长度：{}，线程最大空闲时长：{}秒", flowThreadPoolExecutorHelper.corePoolSize, flowThreadPoolExecutorHelper.maxPoolSize, flowThreadPoolExecutorHelper.queueCapacity, keepAliveTime);
            return flowThreadPoolExecutorHelper;
        }
    }

    @Setter
    @Accessors(chain = true)
    public static class ClassicBuilder {
        /**
         * 核心线程数：默认值10
         */
        private int coreThreadCount = 10;
        /**
         * 最大线程数：默认值10
         */
        private int maxThreadCount = 10;
        /**
         * 等待队列的最大长度：默认值100
         */
        private int queueLength = 100;
        /**
         * 最大空闲时长：秒，如果流量降低，当一个线程这个时间内没有收到任务，就会退出直到总数目为核心线程数：默认值60
         */
        private long keepAliveTime = 60;
        /**
         * 允许最大的核心线程数，计算值不会超过该值，默认不指定
         */
        private int allowMaxCoreThreadCount = 10;
        /**
         * 允许最大的线程数，计算值不会超过该值，默认不指定
         */
        private int allowMaxThreadCount = 10;
        /**
         * 允许更新的最小线程数
         */
        private int allowMinCoreThreadCount = 1;
        /**
         * 允许更新的最大线程数
         */
        private int allowMinThreadCount = 5;
        /**
         * 任务允许的最大响应时间：秒
         */
        private double allowMaxResponseTime = 5;
        /**
         * 是否允许根据流量动态更新
         */
        private boolean allowUpdate = true;
        /**
         * 更新类型
         */
        private UpdateType updateType = UpdateType.USE_FLOW;
        /**
         * 更新阈值
         */
        private double updateThreshold = 0.5;
        /**
         * 流量监控周期：s
         */
        private long trafficListenerCycle = 5;
        /**
         * 线程工厂
         */
        private ThreadFactory threadFactory;

        public FlowThreadPoolExecutorHelper build() {
            FlowThreadPoolExecutorHelper executorHelper = new FlowThreadPoolExecutorHelper();
            /*=================基础参数=================>*/
            executorHelper.corePoolSize = coreThreadCount;
            executorHelper.maxPoolSize = maxThreadCount;
            executorHelper.allowMaxResponseTime = allowMaxResponseTime;
            executorHelper.queueCapacity = queueLength;
            executorHelper.overflowTask = new LinkedBlockingQueue<>();
            if (threadFactory != null) {
                executorHelper.executor = new TimingThreadPoolExecutor(coreThreadCount, maxThreadCount, keepAliveTime, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(queueLength), threadFactory);
            } else {
                executorHelper.executor = new TimingThreadPoolExecutor(coreThreadCount, maxThreadCount, keepAliveTime, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(queueLength));

            }
            /*=======================Finished======================<*/

            /*=================流量管理器参数=================>*/
            TrafficMonitor trafficMonitor = new TrafficMonitor();
            trafficMonitor.allowUpdate = allowUpdate;
            trafficMonitor.updateType = updateType;
            trafficMonitor.allowMaxCoreThreadCount = allowMaxCoreThreadCount;
            trafficMonitor.allowMinCoreThreadCount = allowMinCoreThreadCount;
            trafficMonitor.allowMaxResponseTime = allowMaxResponseTime;
            trafficMonitor.allowMaxThreadCount = allowMaxThreadCount;
            trafficMonitor.allowMinThreadCount = allowMinThreadCount;
            trafficMonitor.threadPoolType = ThreadPoolType.IO_INTENSIVE;
            trafficMonitor.trafficListenerCycle = trafficListenerCycle;
            trafficMonitor.updateThreshold = updateThreshold;
            executorHelper.trafficMonitor = trafficMonitor;
            /*=======================Finished======================<*/
            return executorHelper;
        }

        public ClassicBuilder setUpdateThreshold(double updateThreshold) {
            if (updateThreshold <= 0) {
                throw new IllegalArgumentException();
            }
            this.updateThreshold = updateThreshold;
            return this;
        }

        public ClassicBuilder setTrafficListenerCycle(long trafficListenerCycle, TimeUnit unit) {
            if (trafficListenerCycle <= 0) {
                throw new IllegalArgumentException();
            }
            this.trafficListenerCycle = unit.toSeconds(trafficListenerCycle);
            return this;
        }
    }

    public enum ThreadPoolType {
        /**
         * CPU密集型任务，该类线程池的线程数将按照运行机器的CPU核心数计算
         */
        CPU_INTENSIVE,
        /**
         * IO密集型任务
         */
        IO_INTENSIVE;

    }

    public enum UpdateType {
        /**
         * 根据流量计算
         */
        USE_FLOW {
            public void update(TimingThreadPoolExecutor executor, TrafficMonitor trafficMonitor) {
                double trafficPerSecond = ((1.0 * trafficMonitor.callCount.get()) / trafficMonitor.trafficListenerCycle);
                double maxTrafficPerSecond = ((1.0 * trafficMonitor.maxCallCount.get()) / trafficMonitor.trafficListenerCycle);
                double averageTimeSecond = trafficMonitor.averageTime.get() / 1000.0;
                if (trafficMonitor.allowUpdate && trafficMonitor.isReachThreshold(executor.getCorePoolSize(), executor
                        .getQueue()
                        .size()) != 0) {
                    if (averageTimeSecond > 0) {
                        int newCorePoolSize = (int) (trafficPerSecond * averageTimeSecond);
                        int newMaxPoolSize = (int) ((maxTrafficPerSecond - executor
                                .getQueue()
                                .size()) * averageTimeSecond);
                        newCorePoolSize = Math.max(trafficMonitor.allowMinCoreThreadCount, newCorePoolSize);
                        newCorePoolSize = Math.min(trafficMonitor.allowMaxCoreThreadCount, newCorePoolSize);
                        newMaxPoolSize = Math.max(trafficMonitor.allowMinThreadCount, newMaxPoolSize);
                        newMaxPoolSize = Math.min(trafficMonitor.allowMaxThreadCount, newMaxPoolSize);
                        newMaxPoolSize = Math.max(newMaxPoolSize, newCorePoolSize);
                        if (executor.update(newCorePoolSize, newMaxPoolSize)) {
                            log.debug("线程池每秒平均流量：{}，周期最大流量：{}，平均每秒最大流量：{}，平局执行时长：{}，更新成功：核心线程数{}，最大线程数{}，更新后理论响应时间{}s", trafficPerSecond, trafficMonitor.callCount.get(), maxTrafficPerSecond, averageTimeSecond, newCorePoolSize, newMaxPoolSize, executor
                                    .getQueue()
                                    .size() * 1.0 / (newCorePoolSize * 1.0 / averageTimeSecond));
                        }

                    }

                }
            }
        },
        /**
         * 两倍更新，增加扩大两倍，减少降低到1/2
         */
        TWO_TIMES_UPDATE {
            public void update(TimingThreadPoolExecutor executor, TrafficMonitor trafficMonitor) {
                boolean isOver = trafficMonitor.isReachThreshold(executor.getCorePoolSize(), executor
                        .getQueue()
                        .size()) == 1;
                if (isOver) {
                    int newCorePoolSize = Math.min(trafficMonitor.allowMaxCoreThreadCount, executor.getCorePoolSize() * 2);
                    int newMaxPoolSize = Math.min(trafficMonitor.allowMaxThreadCount, executor.getMaximumPoolSize() * 2);
                    if (trafficMonitor.allowUpdate && newMaxPoolSize >= newCorePoolSize) {
                        executor.setCorePoolSize(newCorePoolSize);
                        executor.setMaximumPoolSize(newMaxPoolSize);
                        log.debug("更新成功：核心线程数{}，最大线程数{}", newCorePoolSize, newMaxPoolSize);
                    }
                } else {
                    int newCorePoolSize = (int) Math.max(trafficMonitor.allowMinCoreThreadCount, executor.getCorePoolSize() * 0.5);
                    int newMaxPoolSize = (int) Math.max(trafficMonitor.allowMinThreadCount, executor.getMaximumPoolSize() * 0.5);
                    if (trafficMonitor.allowUpdate && newMaxPoolSize >= newCorePoolSize) {
                        if (executor.update(newCorePoolSize, newMaxPoolSize)) {
                            log.debug("更新成功：核心线程数{}，最大线程数{}", newCorePoolSize, newMaxPoolSize);
                        }
                    }
                }
            }
        };

        public void update(TimingThreadPoolExecutor executor, TrafficMonitor trafficMonitor) {
            throw new UnsupportedOperationException();
        }


    }
}
