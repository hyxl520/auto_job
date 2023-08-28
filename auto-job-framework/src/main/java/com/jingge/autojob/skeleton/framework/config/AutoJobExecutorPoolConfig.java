package com.jingge.autojob.skeleton.framework.config;

import com.jingge.autojob.util.io.PropertiesHolder;
import lombok.Getter;

/**
 * 执行器池配置
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/24 10:28
 */
@Getter
public class AutoJobExecutorPoolConfig extends AbstractAutoJobConfig {
    /*=================Fast Pool配置=================>*/
    private Integer fastPoolQueueLength;
    private Boolean enableFastPoolUpdate;
    private Double fastPoolAllowMaximumResponseTime;
    private Double fastPoolTrafficUpdateCycle;
    private Double fastPoolAdjustedThreshold;
    private Integer fastPoolInitialCoreThreadCount;
    private Integer fastPoolMinCoreThreadCount;
    private Integer fastPoolMaxCoreThreadCount;
    private Double fastPoolCoreThreadKeepAliveTime;
    private Integer fastPoolInitialThreadCount;
    private Integer fastPoolMinThreadCount;
    private Integer fastPoolMaxThreadCount;
    /*=======================Finished======================<*/

    /*=================Slow Pool配置=================>*/
    private Integer slowPoolQueueLength;
    private Boolean enableSlowPoolUpdate;
    private Double slowPoolAllowMaximumResponseTime;
    private Double slowPoolTrafficUpdateCycle;
    private Double slowPoolAdjustedThreshold;
    private Integer slowPoolInitialCoreThreadCount;
    private Integer slowPoolMinCoreThreadCount;
    private Integer slowPoolMaxCoreThreadCount;
    private Double slowPoolCoreThreadKeepAliveTime;
    private Integer slowPoolInitialThreadCount;
    private Integer slowPoolMinThreadCount;
    private Integer slowPoolMaxThreadCount;
    /*=======================Finished======================<*/



    private Double relegationThreshold;

    public AutoJobExecutorPoolConfig(PropertiesHolder propertiesHolder) {
        super(propertiesHolder);
        fastPoolQueueLength = propertiesHolder.getProperty("autoJob.executor.fastPool.queueLength", Integer.class, "100");
        enableFastPoolUpdate = propertiesHolder.getProperty("autoJob.executor.fastPool.update.enable", Boolean.class, "true");
        fastPoolAllowMaximumResponseTime = propertiesHolder.getProperty("autoJob.executor.fastPool.update.allowTaskMaximumResponseTime", Double.class, "5");
        fastPoolTrafficUpdateCycle = propertiesHolder.getProperty("autoJob.executor.fastPool.update.trafficUpdateCycle", Double.class, "5");
        fastPoolAdjustedThreshold = propertiesHolder.getProperty("autoJob.executor.fastPool.update.adjustedThreshold", Double.class, "0.5");
        fastPoolInitialCoreThreadCount = propertiesHolder.getProperty("autoJob.executor.fastPool.coreThread.initial", Integer.class, "5");
        fastPoolMinCoreThreadCount = propertiesHolder.getProperty("autoJob.executor.fastPool.coreThread.min", Integer.class, "1");
        fastPoolMaxCoreThreadCount = propertiesHolder.getProperty("autoJob.executor.fastPool.coreThread.max", Integer.class, "10");
        fastPoolCoreThreadKeepAliveTime = propertiesHolder.getProperty("autoJob.executor.fastPool.coreThread.keepAliveTime", Double.class, "60");
        fastPoolInitialThreadCount = propertiesHolder.getProperty("autoJob.executor.fastPool.maxThread.initial", Integer.class, "5");
        fastPoolMinThreadCount = propertiesHolder.getProperty("autoJob.executor.fastPool.maxThread.min", Integer.class, "10");
        fastPoolMaxThreadCount = propertiesHolder.getProperty("autoJob.executor.fastPool.maxThread.max", Integer.class, "100");
        slowPoolQueueLength = propertiesHolder.getProperty("autoJob.executor.slowPool.queueLength", Integer.class, "100");
        enableSlowPoolUpdate = propertiesHolder.getProperty("autoJob.executor.slowPool.update.enable", Boolean.class, "false");
        slowPoolAllowMaximumResponseTime = propertiesHolder.getProperty("autoJob.executor.slowPool.update.allowTaskMaximumResponseTime", Double.class, "5");
        slowPoolTrafficUpdateCycle = propertiesHolder.getProperty("autoJob.executor.slowPool.update.trafficUpdateCycle", Double.class, "5");
        slowPoolAdjustedThreshold = propertiesHolder.getProperty("autoJob.executor.slowPool.update.adjustedThreshold", Double.class, "0.5");
        slowPoolInitialCoreThreadCount = propertiesHolder.getProperty("autoJob.executor.slowPool.coreThread.initial", Integer.class, "10");
        slowPoolMinCoreThreadCount = propertiesHolder.getProperty("autoJob.executor.slowPool.coreThread.min", Integer.class, "1");
        slowPoolMaxCoreThreadCount = propertiesHolder.getProperty("autoJob.executor.slowPool.coreThread.max", Integer.class, "10");
        slowPoolCoreThreadKeepAliveTime = propertiesHolder.getProperty("autoJob.executor.slowPool.coreThread.keepAliveTime", Double.class, "120");
        slowPoolInitialThreadCount = propertiesHolder.getProperty("autoJob.executor.slowPool.maxThread.initial", Integer.class, "50");
        slowPoolMinThreadCount = propertiesHolder.getProperty("autoJob.executor.slowPool.maxThread.min", Integer.class, "10");
        slowPoolMaxThreadCount = propertiesHolder.getProperty("autoJob.executor.slowPool.maxThread.max", Integer.class, "100");
        relegationThreshold = propertiesHolder.getProperty("autoJob.executor.relegation.threshold", Double.class, "3");

    }

    public AutoJobExecutorPoolConfig() {
        super();
    }
}
