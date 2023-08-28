package com.jingge.autojob.skeleton.framework.task;

import com.jingge.autojob.skeleton.cluster.model.ClusterNode;
import com.jingge.autojob.skeleton.framework.config.AutoJobShardingConfig;

import java.util.List;
import java.util.Map;

/**
 * 分片策略
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-07-28 15:52
 * @email 1158055613@qq.com
 */
public interface ShardingStrategy {
    default boolean isAvailable(AutoJobShardingConfig config) {
        return config != null && config.isEnable() && config.getTotal() != null;
    }

    /**
     * 进行分片，分片是以集群节点为维度的
     *
     * @param config       分片配置
     * @param clusterNodes 分片所基于的集群节点
     * @return java.util.Map<com.example.autojob.skeleton.cluster.model.ClusterNode, java.lang.Object>
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/7/28 16:15
     */
    Map<ClusterNode, Object> executionSharding(AutoJobShardingConfig config, List<ClusterNode> clusterNodes);
}
