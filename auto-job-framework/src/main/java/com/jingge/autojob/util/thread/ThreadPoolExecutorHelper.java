package com.jingge.autojob.util.thread;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;

/**
 * 封装线程池的抽象接口
 *
 * @author Huang Yongxiang
 * @date 2022-12-07 15:46
 * @email 1158055613@qq.com
 */
public interface ThreadPoolExecutorHelper {
    Future<?> submit(Runnable runnable) throws RejectedExecutionException;

    <V> Future<V> submit(Callable<V> callable) throws RejectedExecutionException;

    void shutdown();

    List<Runnable> shutdownNow();

    void setThreadFactory(ThreadFactory threadFactory);
}
