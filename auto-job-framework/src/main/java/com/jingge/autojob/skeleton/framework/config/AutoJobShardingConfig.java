package com.jingge.autojob.skeleton.framework.config;

import com.jingge.autojob.skeleton.cluster.model.ClusterNode;
import com.jingge.autojob.skeleton.framework.task.ShardingStrategy;

import java.util.List;
import java.util.Map;

/**
 * 分片配置
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-07-28 15:35
 * @email 1158055613@qq.com
 */
public class AutoJobShardingConfig implements StorableConfig {
    private Long taskID;

    /**
     * 是否启用分片
     */
    private boolean enable;

    /**
     * 总分片
     */
    private Object total;

    /**
     * 持有分片
     */
    private Object current;

    /**
     * 当分片发生异常是否允许分片进行重试
     */
    private boolean enableShardingRetry;

    public AutoJobShardingConfig(Long taskID, boolean enable, Object total) {
        this.taskID = taskID;
        this.enable = enable;
        this.total = total;
    }

    public AutoJobShardingConfig(boolean enable, Object total, boolean enableShardingRetry) {
        this.enable = enable;
        this.total = total;
        this.enableShardingRetry = enableShardingRetry;
    }

    public AutoJobShardingConfig() {
    }

    public AutoJobShardingConfig setTaskID(Long taskID) {
        this.taskID = taskID;
        return this;
    }

    public AutoJobShardingConfig setCurrent(Object current) {
        this.current = current;
        return this;
    }

    public AutoJobShardingConfig setEnable(boolean enable) {
        this.enable = enable;
        return this;
    }

    public AutoJobShardingConfig setTotal(Object total) {
        this.total = total;
        return this;
    }

    public AutoJobShardingConfig setEnableShardingRetry(boolean enableShardingRetry) {
        this.enableShardingRetry = enableShardingRetry;
        return this;
    }

    public Object getTotal() {
        return total;
    }

    public Object getCurrent() {
        return current;
    }

    public boolean isEnable() {
        return enable && AutoJobConfigHolder
                .getInstance()
                .getAutoJobConfig()
                .getEnableCluster();
    }

    public boolean isEnableShardingRetry() {
        return enableShardingRetry;
    }

    /**
     * 利用该配置进行分片
     *
     * @param clusterNodes     分片所基于的集群节点
     * @param shardingStrategy 分片策略
     * @return java.util.Map<com.example.autojob.skeleton.cluster.model.ClusterNode, java.lang.Object>
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/7/28 16:24
     */
    public Map<ClusterNode, Object> executionSharding(List<ClusterNode> clusterNodes, ShardingStrategy shardingStrategy) {
        if (!shardingStrategy.isAvailable(this)) {
            throw new UnsupportedOperationException("该配置对于给定的分片策略不可用，请检查兼容性");
        }
        return shardingStrategy.executionSharding(this, clusterNodes);
    }

    @Override
    public ConfigSerializer getSerializer() {
        return new ConfigJsonSerializerAndDeserializer();
    }

    @Override
    public Long getTaskId() {
        return taskID;
    }
}
