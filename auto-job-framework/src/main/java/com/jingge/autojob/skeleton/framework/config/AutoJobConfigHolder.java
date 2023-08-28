package com.jingge.autojob.skeleton.framework.config;

import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.util.io.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * AutoJob配置源，配置优先级：文件 less than 输入流 less than 虚拟机参数  less than 启动变量（args）
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/12 9:52
 */
@Slf4j
public class AutoJobConfigHolder {
    /**
     * 框架基础配置
     */
    private final AutoJobConfig autoJobConfig;
    /**
     * 日志配置
     */
    private final AutoJobLogConfig logConfig;
    /**
     * 集群配置
     */
    private final AutoJobClusterConfig clusterConfig;
    private final PropertiesHolder propertiesHolder;

    /**
     * 使用配置持有者创建一个配置源
     *
     * @param propertiesHolder 配置持有者
     * @author Huang Yongxiang
     * @date 2022/8/12 17:16
     */
    public AutoJobConfigHolder(PropertiesHolder propertiesHolder) {
        this.propertiesHolder = propertiesHolder;
        autoJobConfig = new AutoJobConfig(propertiesHolder);
        logConfig = new AutoJobLogConfig(propertiesHolder);
        clusterConfig = new AutoJobClusterConfig(propertiesHolder);
        log.info("AutoJob加载配置文件：{}", propertiesHolder.getProperty("configFiles"));
    }

    /**
     * 使用资源路径创建一个配置源，支持classpath下的yaml和properties格式
     *
     * @param resourceNamePattern 资源名，支持模式匹配，如application-*.yml
     * @author Huang Yongxiang
     * @date 2022/8/12 17:14
     */
    public AutoJobConfigHolder(String[] args, String... resourceNamePattern) {
        this(PropertiesHolder
                .builder()
                .setArgs(args)
                .setIgnoreSystemProperties(false)
                .addAllPropertiesFile(Arrays.asList(resourceNamePattern))
                .build());
    }

    /**
     * 以输入流的方式创建一个配置源
     *
     * @param inputStreams 输入流
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/7/26 16:42
     */
    public AutoJobConfigHolder(String[] args, List<InputStream> inputStreams) {
        this(PropertiesHolder
                .builder()
                .setArgs(args)
                .addOthers(inputStreams)
                .setIgnoreSystemProperties(false)
                .build());
    }

    public AutoJobConfig getAutoJobConfig() {
        if (autoJobConfig == null) {
            return new AutoJobConfig(propertiesHolder);
        }
        return autoJobConfig;
    }

    public AutoJobLogConfig getLogConfig() {
        if (logConfig == null) {
            return new AutoJobLogConfig(propertiesHolder);
        }
        return logConfig;
    }

    public AutoJobClusterConfig getClusterConfig() {
        if (clusterConfig == null) {
            return new AutoJobClusterConfig(propertiesHolder);
        }
        return clusterConfig;
    }

    public PropertiesHolder getPropertiesHolder() {
        return propertiesHolder;
    }

    /**
     * 获取配置源实例，该方法是从{@link AutoJobApplication}里获取的
     *
     * @return com.example.autojob.skeleton.framework.config.AutoJobConfigHolder
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/7/26 16:30
     */
    public static AutoJobConfigHolder getInstance() {
        return AutoJobApplication
                .getInstance()
                .getConfigHolder();
    }

    public boolean isDebugEnable() {
        return autoJobConfig.getEnableDebug() != null && autoJobConfig.getEnableDebug();
    }
}
