package com.jingge.autojob.skeleton.cluster.model;

import com.jingge.autojob.skeleton.cluster.api.ClusterAPI;
import com.jingge.autojob.skeleton.cluster.dto.ClusterMessage;
import com.jingge.autojob.skeleton.framework.config.AutoJobClusterConfig;
import com.jingge.autojob.skeleton.framework.network.handler.client.RPCClientProxy;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 * 集群请求客户端
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/26 12:40
 */
@Slf4j
public class AutoJobClusterClient implements Closeable, IClusterClient {
    private final ClusterNode clusterNode;
    private final AutoJobClusterConfig config;
    private final ClusterAPI clusterAPI;
    private final RPCClientProxy<ClusterAPI> clientProxy;
    private long lastResponseTime;


    public AutoJobClusterClient(ClusterNode clusterNode, AutoJobClusterConfig config) {
        this.clusterNode = clusterNode;
        this.config = config;
        this.clientProxy = new RPCClientProxy<>(clusterNode.getHost(), clusterNode.getPort(), ClusterAPI.class);
        this.clusterAPI = clientProxy.clientProxy();
    }

    /**
     * 判断节点是否还存活，如果对端节点离线或者对端节点禁止该节点访问时返回false
     *
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/7/26 14:29
     */
    @Override
    public boolean isAlive() {
        long start = System.currentTimeMillis();
        try {
            return clusterAPI.isAlive(new ClusterMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            lastResponseTime = System.currentTimeMillis() - start;
        }
    }

    /**
     * 获取节点拥有的其他节点的信息
     *
     * @return java.util.List<com.jingge.autojob.skeleton.cluster.context.ClusterNode>
     * @author Huang Yongxiang
     * @date 2022/7/26 14:31
     */
    @Override
    public List<ClusterNode> getClusterNodes() {
        long start = System.currentTimeMillis();
        try {
            return clusterAPI.getClusterNodes();
        } finally {
            lastResponseTime = System.currentTimeMillis() - start;
        }
    }

    @Override
    public Boolean registerShardingTask(AutoJobTask task) {
        long start = System.currentTimeMillis();
        try {
            return clusterAPI.registerShardingTask(task);
        } finally {
            lastResponseTime = System.currentTimeMillis() - start;
        }
    }

    public Boolean failoverTask(AutoJobTask task) {
        long start = System.currentTimeMillis();
        try {
            return clusterAPI.failoverTask(task);
        } finally {
            lastResponseTime = System.currentTimeMillis() - start;
        }
    }

    @Override
    public Boolean offLine() {
        long start = System.currentTimeMillis();
        try {
            ClusterNode localhost = ClusterNode.getLocalHostNode();
            return clusterAPI.offLine(localhost.getHost(), localhost.getPort());
        } finally {
            lastResponseTime = System.currentTimeMillis() - start;
        }
    }

    public long getLastResponseTime() {
        return lastResponseTime;
    }

    @Override
    public void close() throws IOException {
        try {
            offLine();
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            clientProxy.destroyProxy();
        }
    }
}
