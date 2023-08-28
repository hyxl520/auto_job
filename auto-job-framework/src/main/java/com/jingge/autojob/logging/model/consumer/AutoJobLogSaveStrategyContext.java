package com.jingge.autojob.logging.model.consumer;

import java.util.List;

/**
 * 任务保存策略上下文，封装策略的执行逻辑
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/12 17:07
 */
public class AutoJobLogSaveStrategyContext<L> {
    private final IAutoJobLogSaveStrategy<L> strategy;

    public AutoJobLogSaveStrategyContext(IAutoJobLogSaveStrategy<L> strategy) {
        this.strategy = strategy;
    }

    public void doHandle(String taskPath, List<L> logList) {
        strategy.doHandle(taskPath, logList);
    }
}
