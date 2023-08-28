package com.jingge.autojob.skeleton.cluster.model;

import com.jingge.autojob.skeleton.framework.config.AutoJobClusterConfig;
import com.jingge.autojob.skeleton.framework.config.AutoJobConfigHolder;
import com.jingge.autojob.skeleton.lang.WithDaemonThread;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.processor.IAutoJobEnd;
import com.jingge.autojob.util.convert.StringUtils;
import com.jingge.autojob.util.servlet.InetUtil;
import com.jingge.autojob.util.thread.ScheduleTaskUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 集群管理器
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/26 12:49
 */
@Slf4j
public class AutoJobClusterManager implements WithDaemonThread, IAutoJobEnd {

    private final AutoJobClusterConfig config;

    private final AutoJobClusterContext clusterContext;

    private final Map<ClusterNode, AutoJobClusterClient> clusterClientMap;

    private final ScheduleTaskUtil clusterManagerDaemonThread;

    private final Map<ClusterNode, AtomicInteger> clusterNodeOfflineCount;

    public AutoJobClusterManager() {
        this.config = AutoJobApplication
                .getInstance()
                .getConfigHolder()
                .getClusterConfig();
        this.clusterContext = AutoJobClusterContext
                .builder()
                .setOpenProtectedMode(config.getEnableProtectedModel())
                .setOpenProtectedModeThreshold(config.getOpenProtectedModelThreshold())
                .build();
        this.clusterClientMap = new ConcurrentHashMap<>();
        this.clusterNodeOfflineCount = new HashMap<>();
        //注册一个节点
        if (!StringUtils.isEmpty(config.getClusterNodeUrl())) {
            String[] url = config
                    .getClusterNodeUrl()
                    .split(":");
            if (url.length == 2) {
                ClusterNode clusterNode = new ClusterNode();
                if ("localhost".equals(url[0])) {
                    clusterNode.setHost(InetUtil.getLocalhostIp());
                } else {
                    clusterNode.setHost(url[0]);
                }
                clusterNode.setPort(Integer.parseInt(url[1]));
                this.clusterContext.insertNode(clusterNode);
                this.clusterClientMap.put(clusterNode, new AutoJobClusterClient(clusterNode, config));
            }
        }
        clusterManagerDaemonThread = ScheduleTaskUtil.build(true, "clusterManagerDaemonThread");
        startWork();
    }

    public AutoJobClusterContext getClusterContext() {
        return clusterContext;
    }

    public Map<ClusterNode, AutoJobClusterClient> getClusterClientMap() {
        return clusterClientMap;
    }

    private Runnable initialize() {
        return () -> {
            //轮询上下文中所有的节点
            for (Iterator<ClusterNode> iterator = clusterContext.iterator(); iterator.hasNext(); ) {
                try {
                    ClusterNode node = iterator.next();
                    if (isMe(node)) {
                        continue;
                    }
                    if (!clusterNodeOfflineCount.containsKey(node)) {
                        clusterNodeOfflineCount.put(node, new AtomicInteger(0));
                    }
                    //如果相关节点的客户端不存在新增客户端
                    if (!clusterClientMap.containsKey(node)) {
                        clusterClientMap.put(node, new AutoJobClusterClient(node, config));
                    }
                    //验证节点
                    AutoJobClusterClient clusterClient = clusterClientMap.get(node);
                    if (clusterClient.isAlive()) {
                        node.setIsLastRequestSuccess(true);
                        node.setIsOnline(true);
                        node.setLastResponseTime(clusterClient.getLastResponseTime());

                        clusterNodeOfflineCount
                                .get(node)
                                .set(0);
                        //获取该节点的注册信息
                        List<ClusterNode> clusterNodeList = clusterClient.getClusterNodes();
                        //log.debug("节点：{}拥有节点{}个", node, clusterNodeList.size());
                        //将除了自己以外的节点尝试注册进本地注册表
                        for (ClusterNode n : clusterNodeList) {
                            //log.debug("拥有节点：{}", n);
                            if ((n.getIsOnline() != null && !n.getIsOnline()) || (n.getIsLastRequestSuccess() != null && !n.getIsLastRequestSuccess())) {
                                continue;
                            }
                            this.clusterContext.insertNode(n);
                        }
                    } else {
                        if (clusterContext.isInProtectedMode()) {
                            continue;
                        }
                        //更新其下线次数
                        clusterNodeOfflineCount
                                .get(node)
                                .incrementAndGet();
                        log.warn("节点：{}离线{}次", node, clusterNodeOfflineCount
                                .get(node)
                                .get());
                        node.setIsLastRequestSuccess(false);
                        node.setLastResponseTime(clusterClient.getLastResponseTime());
                        if (clusterNodeOfflineCount
                                .get(node)
                                .get() >= config.getNodeOffLineThreshold()) {
                            node.setIsOnline(false);
                            log.warn("节点：{}:{}离线", node.getHost(), node.getPort());
                            if (clusterContext.offLine(node.getHost(), node.getPort())) {
                                log.warn("节点：{}:{}已被剔除", node.getHost(), node.getPort());
                                clusterClientMap
                                        .remove(node)
                                        .close();
                                clusterNodeOfflineCount.remove(node);
                            } else {
                                clusterNodeOfflineCount
                                        .get(node)
                                        .set(0);
                                log.warn("该节点已处于保护模式");
                            }
                        }
                    }
                    if (AutoJobConfigHolder
                            .getInstance()
                            .isDebugEnable()) {
                        log.warn("扫描完成，此时集群节点有：");
                        clusterContext
                                .getAllNodes()
                                .forEach(item -> {
                                    if (!ClusterNode.isLocalHostNode(item)) {
                                        log.warn("节点地址：{}，是否在线：{}，上次响应时长：{}ms", item.toString(), item.getIsOnline(), item.getLastResponseTime());
                                    } else {
                                        log.warn("节点地址：localhost，是否在线：true，上次响应时长：0ms");
                                    }
                                });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private boolean isMe(ClusterNode node) {
        try {
            String host = InetUtil.getLocalhostIp();
            int port = InetUtil.getPort();
            if (host == null) {
                return false;
            }
            return node != null && host.equals(node.getHost()) && port == node.getPort();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    @Override
    public void startWork() {
        Runnable runnable = initialize();
        clusterManagerDaemonThread.EFixedRateTask(runnable, 0, (long) (config.getNodeSyncCycle() * 1000), TimeUnit.MILLISECONDS);
    }

    @Override
    public void end() {
        try {
            clusterContext.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
