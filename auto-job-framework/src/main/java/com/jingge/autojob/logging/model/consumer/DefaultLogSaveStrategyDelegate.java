package com.jingge.autojob.logging.model.consumer;

import com.jingge.autojob.logging.domain.AutoJobLog;
import com.jingge.autojob.logging.model.consumer.strategy.AutoJobLogDBStrategy;
import com.jingge.autojob.logging.model.consumer.strategy.AutoJobLogMemoryStrategy;
import com.jingge.autojob.skeleton.framework.config.AutoJobConfigHolder;

/**
 * 默认任务日志保存策略委派者
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/26 11:46
 */
public class DefaultLogSaveStrategyDelegate implements ILogSaveStrategyDelegate<AutoJobLog> {
    @Override
    public IAutoJobLogSaveStrategy<AutoJobLog> doDelegate(AutoJobConfigHolder configHolder, Class<AutoJobLog> type) {
        if (configHolder
                .getLogConfig()
                .getEnableMemory()) {
            throw new UnsupportedOperationException("内存日志模式不再支持");
            //return new AutoJobLogMemoryStrategy();
        }
        return new AutoJobLogDBStrategy();
    }
}
