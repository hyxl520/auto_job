package com.jingge.autojob.logging.model.consumer;

import com.jingge.autojob.skeleton.framework.config.AutoJobConfigHolder;

/**
 * 抽象日志保存策略委派者，主要功能是通过一定的逻辑选择相关的策略来进行保存，保存策略可以看看IAutoJobLogSaveStrategy
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/26 11:08
 * @see IAutoJobLogSaveStrategy
 */
public interface ILogSaveStrategyDelegate<L> {
    /**
     * 委派者的核心方法，该方法根据不同的逻辑来返回对应的保存策略
     *
     * @param configHolder 配置源
     * @param type         日志类型
     * @return com.jingge.autojob.logging.model.consumer.IAutoJobLogSaveStrategy<L>
     * @author Huang Yongxiang
     * @date 2022/8/26 11:45
     */
    IAutoJobLogSaveStrategy<L> doDelegate(AutoJobConfigHolder configHolder, Class<L> type);
}
