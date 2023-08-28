package com.jingge.autojob.skeleton.framework.pool;

import com.jingge.autojob.util.convert.StringUtils;
import com.jingge.autojob.util.thread.ThreadPoolExecutorHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AutoJob执行池，任何实现了Executable接口的类都可交由该执行器池执行
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/02 13:10
 */
@Slf4j
public abstract class AbstractAutoJobPool {
    /**
     * 慢线程池
     */
    private final ThreadPoolExecutorHelper slowThreadPool;
    /**
     * 快线程池
     */
    private final ThreadPoolExecutorHelper fastThreadPool;
    private final IRefuseHandler refuseHandler;

    public AbstractAutoJobPool(String poolName, IRefuseHandler refusedHandler, ThreadPoolExecutorHelper fastThreadPool, ThreadPoolExecutorHelper slowThreadPool) {
        if (fastThreadPool == null || slowThreadPool == null) {
            throw new NullPointerException();
        }
        if (!StringUtils.isEmpty(poolName)) {
            slowThreadPool.setThreadFactory(new NamedThreadFactory(poolName + "-slowPool"));
            fastThreadPool.setThreadFactory(new NamedThreadFactory(poolName + "-fastPool"));
        } else {
            slowThreadPool.setThreadFactory(new NamedThreadFactory("slowPool"));
            fastThreadPool.setThreadFactory(new NamedThreadFactory("fastPool"));
        }
        this.slowThreadPool = slowThreadPool;
        this.fastThreadPool = fastThreadPool;
        this.refuseHandler = refusedHandler;
    }


    public void submit2FastPool(Executable executable, RunnablePostProcessor postProcessor) {
        if (executable == null) {
            throw new NullPointerException();
        }
        AutoJobPoolExecutor executor = new AutoJobPoolExecutor();
        if (!connect2Executor(executable, executor)) {
            throw new RuntimeException(executable + "无法与执行器建立连接");
        }
        executor.setRunnablePostProcessor(postProcessor);
        try {
            fastThreadPool.submit(executor);
        } catch (RejectedExecutionException e) {
            if (refuseHandler != null) {
                refuseHandler.doHandle(executable, postProcessor, this);
            } else {
                e.printStackTrace();
            }
        }
    }

    public void submit2SlowPool(Executable executable, RunnablePostProcessor postProcessor) {
        if (executable == null) {
            throw new NullPointerException();
        }
        AutoJobPoolExecutor executor = new AutoJobPoolExecutor();
        if (!connect2Executor(executable, executor)) {
            throw new RuntimeException(executable + "无法与执行器建立连接");
        }
        executor.setRunnablePostProcessor(postProcessor);
        try {
            slowThreadPool.submit(executor);
        } catch (RejectedExecutionException e) {
            if (refuseHandler != null) {
                refuseHandler.doHandle(executable, postProcessor, this);
            } else {
                e.printStackTrace();
            }
        }
    }


    public void shutdown() {
        fastThreadPool.shutdown();
        slowThreadPool.shutdown();
    }

    public void shutdownNow() {
        fastThreadPool.shutdownNow();
        slowThreadPool.shutdownNow();
    }

    protected boolean connect2Executor(Executable executable, AutoJobPoolExecutor executor) {
        return executor.connect(executable, executable.getExecuteParams());
    }

    public static class NamedThreadFactory implements ThreadFactory {

        private final ThreadGroup threadGroup;

        private final AtomicInteger threadNumber = new AtomicInteger(1);

        public final String namePrefix;

        public NamedThreadFactory(String name) {
            SecurityManager s = System.getSecurityManager();
            threadGroup = (s != null) ? s.getThreadGroup() : Thread
                    .currentThread()
                    .getThreadGroup();
            if (null == name || "".equals(name.trim())) {
                name = "pool";
            }
            AtomicInteger poolNumber = new AtomicInteger(1);
            namePrefix = name + "-" + poolNumber.getAndIncrement() + "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(threadGroup, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon()) t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY) t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }


}
