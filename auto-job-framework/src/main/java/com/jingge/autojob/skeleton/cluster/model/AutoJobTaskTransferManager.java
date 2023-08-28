package com.jingge.autojob.skeleton.cluster.model;

import com.jingge.autojob.skeleton.framework.config.AutoJobConfigHolder;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lifecycle.ITaskEventHandler;
import com.jingge.autojob.skeleton.lifecycle.TaskEventFactory;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskErrorEvent;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskFinishedEvent;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskReceivedEvent;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskTransferEvent;
import com.jingge.autojob.skeleton.lifecycle.manager.TaskEventManager;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务故障转移管理器
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/01 16:38
 */
@Slf4j
public class AutoJobTaskTransferManager extends AbstractAsyncClusterTaskManager implements ITaskEventHandler<TaskReceivedEvent> {
    private final Map<Long, ClusterNode> transferFromMap;

    public AutoJobTaskTransferManager(AutoJobConfigHolder configHolder, AutoJobClusterManager manager) {
        super(configHolder, manager);
        this.transferFromMap = new ConcurrentHashMap<>();
    }


    @Override
    public String daemonThreadName() {
        return "failoverTaskDaemonThread";
    }

    @Override
    public boolean withDaemon() {
        return true;
    }

    @Override
    public long daemonThreadExecutingCycle() {
        return 1;
    }

    @Override
    public Runnable daemon() {
        log.debug("任务故障转移管理器启动");
        return () -> {
            List<ClusterNode> triedNodes = null;
            try {
                AutoJobTask task = readQueueHead();
                if (task == null) {
                    return;
                }
                int nodeCount = manager
                        .getClusterContext()
                        .length();
                boolean find = false;
                triedNodes = new ArrayList<>();
                for (int i = 0; i < nodeCount; i++) {
                    //获取最优节点
                    ClusterNode node = manager
                            .getClusterContext()
                            .getOptimal();
                    if (node != null) {
                        //避免重复执行
                        if (transferFromMap.containsKey(task.getId()) && node.equals(transferFromMap.get(task.getId()))) {
                            continue;
                        }
                        triedNodes.add(node);
                        if (!manager
                                .getClusterClientMap()
                                .containsKey(node)) {
                            manager
                                    .getClusterClientMap()
                                    .put(node, new AutoJobClusterClient(node, config));
                        }
                        //获取客户端
                        AutoJobClusterClient client = manager
                                .getClusterClientMap()
                                .get(node);
                        if (client.failoverTask(task)) {
                            log.info("任务：{}已转移到节点：{}:{}运行", task.getId(), node.getHost(), node.getPort());
                            TaskEventManager
                                    .getInstance()
                                    .publishTaskEvent(TaskEventFactory.newTaskTransferEvent(task, node), TaskTransferEvent.class, true);
                            find = true;
                            break;
                        }
                    }
                }
                if (!find) {
                    log.error("没有找到集群节点进行任务：{}的故障转移", task.getId());
                    TaskEventManager
                            .getInstance()
                            .publishTaskEvent(TaskEventFactory.newErrorEvent(task), TaskErrorEvent.class, true);
                    TaskEventManager
                            .getInstance()
                            .publishTaskEvent(TaskEventFactory.newFinishedEvent(task), TaskFinishedEvent.class, true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                takeQueueHead();
                //更新所有已尝试过的节点状态
                if (triedNodes != null && triedNodes.size() > 0) {
                    triedNodes.forEach(node -> {
                        manager
                                .getClusterContext()
                                .updateStatus(node);
                    });
                }
            }
        };
    }

    @Override
    public void doHandle(TaskReceivedEvent event) {
        transferFromMap.put(event
                .getTask()
                .getId(), event.getTransferFrom());
    }
}
