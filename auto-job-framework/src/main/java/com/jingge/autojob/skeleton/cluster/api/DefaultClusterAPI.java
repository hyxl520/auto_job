package com.jingge.autojob.skeleton.cluster.api;

import com.jingge.autojob.skeleton.annotation.AutoJobRPCService;
import com.jingge.autojob.skeleton.cluster.dto.ClusterMessage;
import com.jingge.autojob.skeleton.cluster.model.AutoJobClusterManager;
import com.jingge.autojob.skeleton.cluster.model.ClusterNode;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.config.*;
import com.jingge.autojob.skeleton.framework.config.AutoJobConfigHolder;
import com.jingge.autojob.skeleton.framework.config.AutoJobConstant;
import com.jingge.autojob.skeleton.framework.config.AutoJobRetryConfig;
import com.jingge.autojob.skeleton.framework.config.RetryStrategy;
import com.jingge.autojob.skeleton.framework.network.handler.client.RPCRequestHelper;
import com.jingge.autojob.skeleton.framework.network.protocol.RPCHeader;
import com.jingge.autojob.skeleton.framework.task.AutoJobRunningStatus;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lifecycle.TaskEventFactory;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskMissFireEvent;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskReceivedEvent;
import com.jingge.autojob.skeleton.lifecycle.manager.TaskEventManager;
import com.jingge.autojob.util.convert.DateUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 默认的集群通信API
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-07-24 14:29
 * @email 1158055613@qq.com
 */
@AutoJobRPCService("defaultClusterAPI")
@Slf4j
public class DefaultClusterAPI implements ClusterAPI {
    private final AutoJobClusterManager clusterManager = AutoJobApplication
            .getInstance()
            .getClusterManager();
    AutoJobConfigHolder configHolder = AutoJobApplication
            .getInstance()
            .getConfigHolder();

    public DefaultClusterAPI() {
        if (configHolder == null || !configHolder
                .getAutoJobConfig()
                .getEnableCluster()) {
            throw new IllegalStateException("AutoJob未开启集群模式");
        }
    }

    @Override
    public List<ClusterNode> getClusterNodes() {
        RPCHeader header = RPCRequestHelper.getCurrentHeader();
        if (!clusterManager
                .getClusterContext()
                .hasNode(header.getSendIp(), header.getSendPort())) {
            clusterManager
                    .getClusterContext()
                    .insertNode(new ClusterNode(header.getSendIp(), header.getSendPort()));
        }
        return clusterManager
                .getClusterContext()
                .getAllNodes();
    }


    @Override
    public Boolean failoverTask(AutoJobTask task) {
        if (task.getRetryConfig() == null) {
            log.warn("任务{}的重试配置为null，不支持故障转移", task.getId());
            return false;
        }
        long next = task
                .getRetryConfig()
                .getNextRetryTime();
        task
                .getTrigger()
                .setTriggeringTime(next);
        RPCHeader header = RPCRequestHelper.getCurrentHeader();
        TaskEventManager
                .getInstance()
                .publishTaskEventSync(TaskEventFactory.newTaskReceivedEvent(task, new ClusterNode(header.getSendIp(), header.getSendPort())), TaskReceivedEvent.class, true);
        boolean flag = AutoJobApplication
                .getInstance()
                .getRegister()
                .registerTask(task, true, 0, TimeUnit.SECONDS);
        if (flag) {
            log.info("接收到来自节点{}:{}的故障转移任务：{}，将于{}在本机器尝试执行", header.getSendIp(), header.getSendPort(), task.getId(), DateUtils.formatDateTime(next));
        } else {
            log.warn("任务注册到调度队列失败，无法进行故障转移");
        }
        return flag;
    }

    @Override
    public Boolean registerShardingTask(AutoJobTask task) {
        if (task == null || task.getTrigger() == null) {
            return false;
        }
        RPCHeader header = RPCRequestHelper.getCurrentHeader();
        //分片任务的元数据定义
        //task
        //        .getTrigger()
        //        .setTriggeringTime(System.currentTimeMillis() + AutoJobConstant.beforeSchedulingInTimeWheel);
        if (task
                .getTrigger()
                .getTriggeringTime() - 1000 < System.currentTimeMillis()) {
            TaskEventManager
                    .getInstance()
                    .publishTaskEvent(TaskEventFactory.newTaskMissFireEvent(task), TaskMissFireEvent.class, true);
        }
        //如果不是本机发起的，即从远程来的就更新成内存任务，并且只执行一次
        if (!ClusterNode.isLocalHostNode(header.getSendIp(), header.getSendPort())) {
            if (AutoJobConfigHolder
                    .getInstance()
                    .isDebugEnable()) {
                log.warn("任务非来自本节点，更新任务元数据");
            }
            task.setType(AutoJobTask.TaskType.SHARDING);
            task
                    .getTrigger()
                    .setRepeatTimes(0);
        }
        task.setRetryConfig(new AutoJobRetryConfig().setRetryStrategy(RetryStrategy.LOCAL_RETRY));
        task.setIsAlreadyBroadcastSharding(true);
        task.setRunningStatus(AutoJobRunningStatus.SCHEDULING);

        boolean flag = AutoJobApplication
                .getInstance()
                .getRegister()
                .registerTask(task, true, 0, TimeUnit.SECONDS);
        if (flag) {
            log.info("接收到来自节点{}:{}的分片任务：{}，已放入调度队列，将于{}在本机器执行", header.getSendIp(), header.getSendPort(), task.getId(), DateUtils.formatDateTime(task
                    .getTrigger()
                    .getTriggeringTime()));
        } else {
            log.warn("任务注册到调度队列失败，无法进行分片执行");
        }
        return flag;
    }

    @Override
    public Boolean isAlive(ClusterMessage clusterMessage) {
        return true;
    }


    @Override
    public Boolean offLine(String host, int port) {
        return clusterManager
                .getClusterContext()
                .offLine(host, port);
    }
}
