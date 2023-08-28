package com.jingge.autojob.skeleton.cluster.model;

import com.jingge.autojob.skeleton.framework.config.AutoJobConfigHolder;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.util.id.IdGenerator;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 任务分片管理器
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-08-01 15:58
 * @email 1158055613@qq.com
 */
@Slf4j
public class AutoJobTaskShardingManager extends AbstractAsyncClusterTaskManager {
    public AutoJobTaskShardingManager(AutoJobConfigHolder configHolder, AutoJobClusterManager manager) {
        super(configHolder, manager);
    }

    @Override
    public Runnable daemon() {
        return () -> {
            try {
                AutoJobTask task = readQueueHead();
                if (!task.getIsShardingTask()) {
                    takeQueueHead();
                }

                Map<ClusterNode, Object> sharding;
                List<ClusterNode> allNodes = this.manager
                        .getClusterContext()
                        .getAllNodes();
                //if (AutoJobConfigHolder
                //        .getInstance()
                //        .isDebugEnable()) {
                //    log.warn("集群当前支持分片的节点数公有{}个", allNodes.size());
                //}
                try {
                    sharding = task
                            .getShardingConfig()
                            .executionSharding(allNodes, task.getShardingStrategy());
                    if (sharding == null) {
                        throw new Exception("分片后的内容为空");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    log.warn("任务{}构建分片失败：{}", task.getId(), e.getMessage());
                    takeQueueHead();
                    return;
                }
                List<Object> errorSharding = new ArrayList<>();
                task.setShardingId(IdGenerator.getNextIdAsLong());
                for (Map.Entry<ClusterNode, Object> entry : sharding.entrySet()) {
                    AutoJobClusterClient clusterClient;
                    if (manager
                            .getClusterClientMap()
                            .containsKey(entry.getKey())) {
                        clusterClient = manager
                                .getClusterClientMap()
                                .get(entry.getKey());
                    } else {
                        clusterClient = new AutoJobClusterClient(entry.getKey(), config);
                        manager
                                .getClusterClientMap()
                                .put(entry.getKey(), clusterClient);
                    }

                    task
                            .getShardingConfig()
                            .setCurrent(entry.getValue());
                    try {
                        if (clusterClient.registerShardingTask(task)) {
                            if (AutoJobConfigHolder
                                    .getInstance()
                                    .isDebugEnable()) {
                                log.warn("任务{}的分片已经转移到节点{}运行", task.getId(), entry
                                        .getKey()
                                        .toString());
                            }
                            takeQueueHead();
                        } else {
                            log.warn("任务{}的分片转移到节点{}异常", task.getId(), entry
                                    .getKey()
                                    .toString());
                            errorSharding.add(entry.getValue());
                        }
                    } catch (Exception e) {
                        log.warn("任务{}的分片转移到节点{}异常：{}", task.getId(), entry
                                .getKey()
                                .toString(), e.getMessage());
                        errorSharding.add(entry.getValue());
                    }
                }
                if (errorSharding.size() > 0) {
                    AutoJobClusterClient local = manager
                            .getClusterClientMap()
                            .get(ClusterNode.getLocalHostNode());
                    for (Object s : errorSharding) {
                        AutoJobTask newOne = AutoJobTask.deepCopyFrom(task);
                        newOne.setType(AutoJobTask.TaskType.MEMORY_TASk);
                        newOne
                                .getTrigger()
                                .setRepeatTimes(0);
                        newOne
                                .getShardingConfig()
                                .setCurrent(s);
                        local.registerShardingTask(newOne);
                    }
                    log.info("任务{}的{}个异常分片已转移到本节点运行", task.getId(), errorSharding.size());
                }
                if (AutoJobConfigHolder
                        .getInstance()
                        .isDebugEnable()) {
                    log.warn("任务{}的所有分片(共计{}个)已广播完成", task.getId(), sharding.size());
                }
            } catch (Exception ignored) {
            }
        };
    }


    @Override
    public String daemonThreadName() {
        return "shardingTaskManagerThread";
    }

    @Override
    public boolean withDaemon() {
        return AutoJobConfigHolder
                .getInstance()
                .getAutoJobConfig()
                .getEnableCluster();
    }

    @Override
    public long daemonThreadExecutingCycle() {
        return 1;
    }
}
