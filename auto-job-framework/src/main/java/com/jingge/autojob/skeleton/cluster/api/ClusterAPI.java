package com.jingge.autojob.skeleton.cluster.api;

import com.jingge.autojob.skeleton.annotation.AutoJobRPCClient;
import com.jingge.autojob.skeleton.cluster.dto.ClusterMessage;
import com.jingge.autojob.skeleton.cluster.model.ClusterNode;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;

import java.util.List;

/**
 * 集群通信API接口
 *
 * @Author Huang Yongxiang
 * @Date 2022/10/19 14:05
 */
@AutoJobRPCClient("defaultClusterAPI")
public interface ClusterAPI {
    List<ClusterNode> getClusterNodes();

    Boolean failoverTask(AutoJobTask task);

    Boolean registerShardingTask(AutoJobTask task);

    Boolean isAlive(ClusterMessage clusterMessage);

    Boolean offLine(String host, int port);
}
