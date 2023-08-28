package com.jingge.autojob.skeleton.cluster.model;

import com.jingge.autojob.util.convert.StringUtils;
import com.jingge.autojob.util.servlet.InetUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 集群节点
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/26 9:09
 */
@Getter
@Setter
@Accessors(chain = true)
public class ClusterNode {
    /**
     * 节点主机地址
     */
    private String host;
    /**
     * 节点TCP端口号
     */
    private Integer port;
    /**
     * 上次响应时长
     */
    private Long lastResponseTime;
    /**
     * 上次请求是否成功
     */
    private Boolean isLastRequestSuccess;
    /**
     * 是否在线
     */
    private Boolean isOnline;

    public ClusterNode(String host, Integer port, Long lastResponseTime, Boolean isLastRequestSuccess, Boolean isOnline) {
        this.host = host;
        this.port = port;
        this.lastResponseTime = lastResponseTime;
        this.isLastRequestSuccess = isLastRequestSuccess;
        this.isOnline = isOnline;
    }

    public ClusterNode() {
    }

    public ClusterNode(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    public static ClusterNode getLocalHostNode() {
        ClusterNode clusterNode = new ClusterNode();
        clusterNode.setHost(InetUtil.getLocalhostIp());
        clusterNode.setPort(InetUtil.getPort());
        return clusterNode;
    }

    public static boolean isLocalHostNode(ClusterNode node) {
        if (node == null || StringUtils.isEmpty(node.getHost()) || node.getPort() == null) {
            return false;
        }
        return isLocalHostNode(node.host, node.port);
    }

    public static boolean isLocalHostNode(String host, int port) {
        return InetUtil.localhostIP.equals(host) && InetUtil.getPort() == port;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        if (hashCode() != obj.hashCode()) {
            return false;
        }
        return this
                .toString()
                .equals(obj.toString());
    }

    @Override
    public String toString() {
        return String.format("%s:%d", host, port);
    }
}
