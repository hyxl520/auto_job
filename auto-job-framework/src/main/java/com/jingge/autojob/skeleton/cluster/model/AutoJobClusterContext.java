package com.jingge.autojob.skeleton.cluster.model;

import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.config.AutoJobClusterConfig;
import com.jingge.autojob.skeleton.lang.LifeCycleHook;
import com.jingge.autojob.skeleton.lang.WithDaemonThread;
import com.jingge.autojob.skeleton.model.alert.AlertEventHandlerDelegate;
import com.jingge.autojob.skeleton.model.alert.event.AlertEventFactory;
import com.jingge.autojob.util.convert.StringUtils;
import com.jingge.autojob.util.thread.ScheduleTaskUtil;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 集群的上下文
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/25 17:06
 */
@Slf4j
public class AutoJobClusterContext implements Closeable, WithDaemonThread, LifeCycleHook {
    /**
     * 存储集群节点的容器
     */
    private Map<String, ClusterNode> clusterNodeContainer;
    /**
     * 节点队列，位于队列头的节点是给定策略下的最优节点
     */
    private Queue<ClusterNode> clusterNodes;
    /**
     * 集群节点选择策略比较器
     */
    private Comparator<ClusterNode> clusterNodeComparator;
    /**
     * 是否开启保护模式
     */
    private AtomicBoolean isOpenProtectedMode;
    /**
     * 开启保护模式的阈值
     */
    private double openProtectedModeThreshold;
    /**
     * 守护线程
     */
    private ScheduleTaskUtil clusterDaemonThread;

    private boolean isDaemonThreadStart = false;

    private int maxNodeCount;

    private AutoJobClusterContext() {
        this.clusterNodeComparator = new DefaultComparator();
        this.clusterNodeContainer = new ConcurrentHashMap<>();
        this.clusterNodes = new PriorityBlockingQueue<>(5, this.clusterNodeComparator);
        insertNode(ClusterNode.getLocalHostNode());
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean insertNode(ClusterNode node) {
        String key = getNodeKey(node);
        if (StringUtils.isEmpty(key)) {
            return false;
        }
        if (!clusterNodeContainer.containsKey(key)) {
            clusterNodeContainer.put(key, node);
            clusterNodes.offer(node);
        }
        if (clusterNodeContainer.size() > maxNodeCount) {
            maxNodeCount = clusterNodeContainer.size();
        }
        return true;
    }

    /**
     * 获取最优节点，最优节点使用完并且更新后必须调用updateStatus方法将节点更新，否则该节点将会失效
     *
     * @return com.jingge.autojob.skeleton.cluster.context.ClusterNode
     * @author Huang Yongxiang
     * @date 2022/7/26 11:03
     */
    public ClusterNode getOptimal() {
        if (clusterNodeContainer.size() > 0) {
            ClusterNode node = clusterNodes.poll();
            if (ClusterNode.isLocalHostNode(node)) {
                return null;
            }
            return node;
        }
        return null;
    }

    /**
     * 更新节点状态
     *
     * @param node 更新后的节点
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/7/26 11:05
     */
    public boolean updateStatus(ClusterNode node) {
        String key = getNodeKey(node);
        if (!StringUtils.isEmpty(key) && clusterNodeContainer.containsKey(key) && clusterNodes
                .stream()
                .noneMatch(item -> key.equals(getNodeKey(item)))) {
            return clusterNodes.offer(node);
        }
        return false;
    }

    public List<ClusterNode> getAllNodes() {
        return new ArrayList<>(clusterNodeContainer.values());
    }

    public Iterator<ClusterNode> iterator() {
        return clusterNodeContainer
                .values()
                .iterator();
    }

    public int getOfflineCount() {
        return (int) getAllNodes()
                .stream()
                .filter(item -> item.getIsOnline() != null && !item.getIsOnline())
                .count();
    }

    public int length() {
        return clusterNodeContainer.size();
    }

    public int getOnlineCount() {
        return length() - getOfflineCount();
    }

    public boolean hasNode(String host, int port) {
        String key = getNodeKey(host, port);
        return !StringUtils.isEmpty(key) && clusterNodeContainer.containsKey(key);
    }

    public boolean isInProtectedMode() {
        return isOpenProtectedMode.get();
    }

    public boolean offLine(String host, int port) {
        String key = getNodeKey(host, port);
        if (!StringUtils.isEmpty(key) && clusterNodeContainer.containsKey(key) && !isOpenProtectedMode.get()) {
            ClusterNode node = clusterNodeContainer.remove(key);
            clusterNodes.remove(node);
            return true;
        }
        return false;
    }

    private String getNodeKey(ClusterNode node) {
        if (node == null || StringUtils.isEmpty(node.getHost()) || node.getPort() == null) {
            return null;
        }
        return getNodeKey(node.getHost(), node.getPort());
    }

    private String getNodeKey(String host, int port) {
        if (StringUtils.isEmpty(host)) {
            return null;
        }
        return String.format("%s:%s", host, port);
    }


    @Override
    public void close() throws IOException {
        beforeClose();
        clusterNodeContainer.clear();
        clusterNodes.clear();
        clusterNodeContainer = null;
        clusterNodes = null;
        clusterDaemonThread.shutdown();
        System.gc();
    }

    @Override
    public void afterInitialize(Object... params) {

    }

    @Override
    public void beforeClose(Object... params) throws IOException {
        AutoJobClusterConfig clusterConfig = AutoJobApplication
                .getInstance()
                .getConfigHolder()
                .getClusterConfig();
        //向所有节点汇报下线
        for (Map.Entry<String, ClusterNode> entry : clusterNodeContainer.entrySet()) {
            if (ClusterNode.isLocalHostNode(entry.getValue())) {
                continue;
            }
            AutoJobClusterClient client = AutoJobApplication
                    .getInstance()
                    .getClusterManager()
                    .getClusterClientMap()
                    .get(entry.getValue());
            client = client == null ? new AutoJobClusterClient(entry.getValue(), clusterConfig) : client;
            client.close();
        }
    }

    public void startWork() {
        if (isDaemonThreadStart) {
            return;
        }
        AtomicBoolean flag = new AtomicBoolean(false);
        Runnable runnable = () -> {
            if (length() > 0) {
                isOpenProtectedMode.set((length() * 1.0) / maxNodeCount <= openProtectedModeThreshold);
            }
            if (isInProtectedMode() && !flag.get()) {
                log.warn("已进入保护模式");
                AlertEventHandlerDelegate
                        .getInstance()
                        .doHandle(AlertEventFactory.newClusterOpenProtectedModelAlertEvent());
                flag.set(true);
            }
            if (!isInProtectedMode() && flag.get()) {
                log.warn("已退出保护模式");
                AlertEventHandlerDelegate
                        .getInstance()
                        .doHandle(AlertEventFactory.newClusterCloseProtectedModeEvent());
                flag.set(false);
            }
        };
        clusterDaemonThread.EFixedRateTask(runnable, 0, 1, TimeUnit.SECONDS);
        isDaemonThreadStart = true;
    }

    private static class DefaultComparator implements java.util.Comparator<ClusterNode> {
        @Override
        public int compare(ClusterNode o1, ClusterNode o2) {
            //优先请求新节点
            if (isNewNode(o1) && !isNewNode(o2)) {
                return 1;
            } else if (!isNewNode(o1) && isNewNode(o2)) {
                return -1;
            } else if (isNewNode(o1) && isNewNode(o2)) {
                return 0;
            } else {
                //优先请求上次请求成功的节点
                if (!o1.getIsLastRequestSuccess() && o2.getIsLastRequestSuccess()) {
                    return -1;
                } else if (o1.getIsLastRequestSuccess() && !o2.getIsLastRequestSuccess()) {
                    return 1;
                }
                //上次请求状态相同的情况下比较上次响应时长
                else {
                    return Long.compare(o1.getLastResponseTime(), o2.getLastResponseTime());
                }
            }
        }

        private boolean isNewNode(ClusterNode node) {
            return node.getIsLastRequestSuccess() == null && node.getLastResponseTime() == null;
        }
    }

    @Setter
    @Accessors(chain = true)
    public static class Builder {
        /**
         * 集群节点选择策略比较器
         */
        private Comparator<ClusterNode> clusterNodeComparator = new DefaultComparator();
        /**
         * 是否开启保护模式
         */
        private boolean isOpenProtectedMode = false;
        /**
         * 开启保护模式的阈值
         */
        private double openProtectedModeThreshold = 0.8;

        public AutoJobClusterContext build() {
            AutoJobClusterContext clusterContext = new AutoJobClusterContext();
            clusterContext.isOpenProtectedMode = new AtomicBoolean(false);
            clusterContext.openProtectedModeThreshold = openProtectedModeThreshold;
            clusterContext.clusterNodeComparator = clusterNodeComparator;
            if (isOpenProtectedMode) {
                clusterContext.clusterDaemonThread = ScheduleTaskUtil.build(true, "clusterContextDaemonThread");
                clusterContext.startWork();
            }
            return clusterContext;
        }
    }


}
