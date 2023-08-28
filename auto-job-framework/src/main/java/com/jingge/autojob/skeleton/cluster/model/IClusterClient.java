package com.jingge.autojob.skeleton.cluster.model;

import com.jingge.autojob.skeleton.framework.task.AutoJobTask;

import java.util.List;

/**
 * 集群通信客户端接口
 *
 * @author Huang Yongxiang
 * @date 2023-02-08 11:51
 * @email 1158055613@qq.com
 */
public interface IClusterClient {
    boolean isAlive();

    List<ClusterNode> getClusterNodes();

    Boolean registerShardingTask(AutoJobTask task);

    Boolean failoverTask(AutoJobTask task);

    Boolean offLine();
}
