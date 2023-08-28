package com.jingge.autojob.skeleton.cluster.model;

import com.jingge.autojob.skeleton.framework.config.AutoJobClusterConfig;
import com.jingge.autojob.skeleton.framework.config.AutoJobConfigHolder;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lang.WithDaemonThread;
import com.jingge.autojob.util.thread.ScheduleTaskUtil;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 异步集群任务抽象管理器
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-08-01 15:58
 * @email 1158055613@qq.com
 */
public abstract class AbstractAsyncClusterTaskManager implements WithDaemonThread {
    protected final AutoJobClusterManager manager;
    protected final AutoJobClusterConfig config;
    private final Queue<AutoJobTask> waitHandleTaskQueue;
    private ScheduleTaskUtil daemonThread;
    private long cycle;

    public AbstractAsyncClusterTaskManager(AutoJobConfigHolder configHolder, AutoJobClusterManager manager) {
        this.config = configHolder.getClusterConfig();
        this.waitHandleTaskQueue = new LinkedBlockingQueue<>();
        this.manager = manager;
        if (withDaemon()) {
            this.cycle = daemonThreadExecutingCycle();
            if (this.cycle <= 0) {
                this.cycle = 1;
            }
            this.daemonThread = ScheduleTaskUtil.build(true, daemonThreadName());
            startWork();
        }

    }

    public boolean addTask(AutoJobTask task) {
        if (waitHandleTaskQueue.contains(task)) {
            return true;
        }
        return waitHandleTaskQueue.offer(task);
    }

    public abstract Runnable daemon();

    public abstract String daemonThreadName();

    public abstract boolean withDaemon();

    public abstract long daemonThreadExecutingCycle();

    public AutoJobTask readQueueHead() {
        return waitHandleTaskQueue.peek();
    }

    public AutoJobTask takeQueueHead() {
        return this.waitHandleTaskQueue.poll();
    }

    @Override
    public final void startWork() {
        daemonThread.EFixedRateTask(daemon(), 0, cycle, TimeUnit.MILLISECONDS);
    }
}
