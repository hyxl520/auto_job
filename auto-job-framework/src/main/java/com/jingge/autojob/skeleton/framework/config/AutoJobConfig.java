package com.jingge.autojob.skeleton.framework.config;

import com.jingge.autojob.skeleton.annotation.HotLoadable;
import com.jingge.autojob.skeleton.enumerate.DatabaseType;
import com.jingge.autojob.skeleton.framework.container.CleanStrategy;
import com.jingge.autojob.util.io.PropertiesHolder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * 框架配置
 *
 * @Author Huang Yongxiang
 * @Date 2022/06/29 17:52
 */
@Getter
@Slf4j
public class AutoJobConfig extends AbstractAutoJobConfig {

    private Boolean enableDebug;

    private Integer schedulingQueueLength;

    private Integer memoryContainerLength;

    private Boolean enableStackTrace;

    private Integer stackDepth;

    private CleanStrategy cleanStrategy;

    private Boolean enableAnnotation;

    private Double annotationDefaultDelayTime;

    private Boolean enableAnnotationFilter;

    private List<String> annotationClassPathPattern;

    private DatabaseType databaseType;

    @HotLoadable
    private Boolean enableRegisterFilter;

    @HotLoadable
    private List<String> filterClassPathList;

    private AutoJobRetryConfig retryConfig;

    private Boolean enableCluster;

    @HotLoadable
    private Boolean taskRunErrorAlert;

    @HotLoadable
    private Boolean clusterOpenProtectedModeAlert;

    @HotLoadable
    private Boolean clusterCloseProtectedModeAlert;

    @HotLoadable
    private Boolean taskRefuseHandleAlert;

    private AutoJobExecutorPoolConfig executorPoolConfig;

    private AutoJobMailConfig mailConfig;

    public AutoJobConfig(PropertiesHolder propertiesHolder) {
        super(propertiesHolder);
        if (propertiesHolder != null) {
            enableDebug = propertiesHolder.getProperty("autoJob.debug.enable", Boolean.class, "false");
            schedulingQueueLength = propertiesHolder.getProperty("autoJob.context.schedulingQueue.length", Integer.class, "1000");
            memoryContainerLength = propertiesHolder.getProperty("autoJob.context.memoryContainer.length", Integer.class, "200");
            enableStackTrace = propertiesHolder.getProperty("autoJob.context.running.stackTrace.enable", Boolean.class, "true");
            stackDepth = propertiesHolder.getProperty("autoJob.context.running.stackTrace.depth", Integer.class, "16");
            cleanStrategy = CleanStrategy.findWithName(propertiesHolder.getProperty("autoJob.context.memoryContainer.cleanStrategy", String.class, "KEEP_FINISHED"));
            databaseType = DatabaseType.findByName(propertiesHolder.getProperty("autoJob.database.type", String.class, "mysql"));
            if (databaseType == null) {
                log.warn("未知的数据库类型：{}，将使用默认数据库类型：MySQL", propertiesHolder.getProperty("autoJob.database.type", String.class));
            }
            enableAnnotation = propertiesHolder.getProperty("autoJob.annotation.enable", Boolean.class, "false");
            enableAnnotationFilter = propertiesHolder.getProperty("autoJob.annotation.filter.enable", Boolean.class, "false");
            annotationClassPathPattern = Arrays.asList(propertiesHolder
                    .getProperty("autoJob.annotation.filter.classPattern", "")
                    .split(","));
            annotationDefaultDelayTime = propertiesHolder.getProperty("autoJob.annotation.defaultDelayTime", Double.class, "30");
            enableRegisterFilter = propertiesHolder.getProperty("autoJob.register.filter.enable", Boolean.class, "false");
            filterClassPathList = Arrays.asList(propertiesHolder
                    .getProperty("autoJob.register.filter.classPath", "")
                    .split(","));
            enableCluster = propertiesHolder.getProperty("autoJob.cluster.enable", Boolean.class, "false");
            taskRunErrorAlert = propertiesHolder.getProperty("autoJob.emailAlert.config" + ".taskRunError", Boolean.class, "true");
            clusterOpenProtectedModeAlert = propertiesHolder.getProperty("autoJob.emailAlert" + ".config.clusterOpenProtectedMode", Boolean.class, "true");
            clusterCloseProtectedModeAlert = propertiesHolder.getProperty("autoJob.emailAlert" + ".config.clusterCloseProtectedMode", Boolean.class, "true");
            taskRefuseHandleAlert = propertiesHolder.getProperty("autoJob.emailAlert.config" + ".taskRefuseHandle", Boolean.class, "true");
            executorPoolConfig = new AutoJobExecutorPoolConfig(propertiesHolder);
            retryConfig = new AutoJobRetryConfig(propertiesHolder);
            mailConfig = new AutoJobMailConfig(propertiesHolder);
        }
    }

    public AutoJobConfig setEnableDebug(Boolean enableDebug) {
        this.enableDebug = enableDebug;
        return this;
    }
}
