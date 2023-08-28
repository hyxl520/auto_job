package com.jingge.autojob.skeleton.framework.config;

import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.util.io.PropertiesHolder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 重试配置
 *
 * @author Huang Yongxiang
 * @date 2022-12-30 14:08
 * @email 1158055613@qq.com
 */
@Getter
@Setter
@Accessors(chain = true)
public class AutoJobRetryConfig extends AbstractAutoJobConfig implements StorableConfig {
    /**
     * 是否启用重试机制
     */
    private Boolean enable;
    /**
     * 重试策略
     */
    private RetryStrategy retryStrategy;
    /**
     * 重试次数
     */
    private Integer retryCount;
    /**
     * 重试间隔：分钟
     */
    private Double interval;
    /**
     * 关联到的任务ID
     */
    private Long taskId;

    public AutoJobRetryConfig() {
        super(null);
        AutoJobRetryConfig global = AutoJobApplication
                .getInstance()
                .getConfigHolder()
                .getAutoJobConfig()
                .getRetryConfig();
        this.enable = global.enable;
        this.retryCount = global.retryCount;
        this.interval = global.interval;
        this.retryStrategy = global.retryStrategy;
    }

    public AutoJobRetryConfig(boolean enable, RetryStrategy retryStrategy, int retryCount, double interval) {
        this.enable = enable;
        this.retryStrategy = retryStrategy;
        this.retryCount = retryCount;
        this.interval = interval;
    }

    public AutoJobRetryConfig(boolean enable, RetryStrategy retryStrategy, int retryCount, double interval, long taskId) {
        this.enable = enable;
        this.retryCount = retryCount;
        this.interval = interval;
        this.taskId = taskId;
        this.retryStrategy = retryStrategy;
    }

    public AutoJobRetryConfig(PropertiesHolder propertiesHolder) {
        super(propertiesHolder);
        enable = propertiesHolder.getProperty("autoJob.scheduler.finished.error.retry.enable", Boolean.class, "true");
        retryStrategy = RetryStrategy.findByName(propertiesHolder.getProperty("autoJob.scheduler.finished.error.retry" + ".strategy", String.class, "LOCAL_RETRY"));
        retryCount = propertiesHolder.getProperty("autoJob.scheduler.finished.error.retry.retryCount", Integer.class, "3");
        interval = propertiesHolder.getProperty("autoJob.scheduler.finished.error.retry.interval", Double.class, "5");
    }

    public long getNextRetryTime() {
        if (enable == null || !enable) {
            return -1;
        }
        return (long) (System.currentTimeMillis() + interval * 60 * 1000);
    }

    @Override
    public ConfigSerializer getSerializer() {
        return new ConfigJsonSerializerAndDeserializer();
    }
}
