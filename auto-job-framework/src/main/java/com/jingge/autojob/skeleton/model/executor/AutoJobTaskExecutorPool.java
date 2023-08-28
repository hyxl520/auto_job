package com.jingge.autojob.skeleton.model.executor;

import com.jingge.autojob.skeleton.framework.pool.AbstractAutoJobPool;
import com.jingge.autojob.skeleton.framework.pool.IRefuseHandler;
import com.jingge.autojob.util.thread.ThreadPoolExecutorHelper;

/**
 * 任务执行器池
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/02 15:36
 */
public class AutoJobTaskExecutorPool extends AbstractAutoJobPool {
    private static final String POOL_NAME = "taskExecutorPool";

    public AutoJobTaskExecutorPool(IRefuseHandler refusedHandler, ThreadPoolExecutorHelper fastThreadPool, ThreadPoolExecutorHelper slowThreadPool) {
        super(POOL_NAME, refusedHandler, fastThreadPool, slowThreadPool);
    }
}
