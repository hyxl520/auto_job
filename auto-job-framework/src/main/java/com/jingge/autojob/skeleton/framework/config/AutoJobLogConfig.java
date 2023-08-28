package com.jingge.autojob.skeleton.framework.config;

import com.jingge.autojob.util.io.PropertiesHolder;
import lombok.Getter;

import java.nio.charset.Charset;

/**
 * @Description
 * @Author Huang Yongxiang
 * @Date 2022/07/07 17:37
 */
@Getter
public class AutoJobLogConfig extends AbstractAutoJobConfig {

    private Boolean enableMemory;

    private Integer memoryLength;

    private Double memoryDefaultExpireTime;

    private Boolean enableRunLogMemory;

    private Charset scriptTaskLogCharset;

    private Integer memoryRunLogLength;

    private Double memoryRunLogDefaultExpireTime;

    private Integer saveWhenBufferReachSize;

    private Long saveWhenOverTime;

    public AutoJobLogConfig(PropertiesHolder propertiesHolder) {
        super(propertiesHolder);
        if (propertiesHolder != null) {
            enableMemory = propertiesHolder.getProperty("autoJob.logging.taskLog.memory.enable", Boolean.class, "true");
            memoryLength = propertiesHolder.getProperty("autoJob.logging.taskLog.memory.length", Integer.class, "100");
            memoryDefaultExpireTime = propertiesHolder.getProperty("autoJob.logging.taskLog.memory.enable", Double.class, "10");
            enableRunLogMemory = propertiesHolder.getProperty("autoJob.logging.runLog.memory.enable", Boolean.class, "true");
            scriptTaskLogCharset = Charset.forName(propertiesHolder.getProperty("autoJob.logging.scriptTask.encoding", "UTF-8"));
            memoryRunLogLength = propertiesHolder.getProperty("autoJob.logging.runLog.memory.length", Integer.class, "100");
            memoryRunLogDefaultExpireTime = propertiesHolder.getProperty("autoJob.logging.runLog.memory.defaultExpireTime", Double.class, "10");
            saveWhenBufferReachSize = propertiesHolder.getProperty("autoJob.logging.strategy.saveWhenBufferReachSize", Integer.class, "10");
            saveWhenOverTime = propertiesHolder.getProperty("autoJob.logging.strategy.saveWhenOverTime", Long.class, "10");
            if (enableMemory || enableRunLogMemory) {
                throw new UnsupportedOperationException("内存日志模式不再支持");
            }
        }
    }

    public AutoJobLogConfig() {
    }
}
