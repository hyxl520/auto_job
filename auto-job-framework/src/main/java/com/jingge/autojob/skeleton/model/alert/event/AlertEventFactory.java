package com.jingge.autojob.skeleton.model.alert.event;

import com.jingge.autojob.skeleton.cluster.model.ClusterNode;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lang.IAutoJobFactory;
import com.jingge.autojob.util.servlet.InetUtil;

/**
 * @Description 报警事件工厂
 * @Author Huang Yongxiang
 * @Date 2022/07/28 17:18
 */
public class AlertEventFactory implements IAutoJobFactory {
    /**
     * 获取当前运行的节点
     *
     * @return com.example.autojob.skeleton.cluster.context.ClusterNode
     * @author Huang Yongxiang
     * @date 2022/7/29 18:05
     */
    public static ClusterNode getLocalhostNode() {
        ClusterNode node = new ClusterNode();
        node
                .setHost(InetUtil.getLocalhostIp())
                .setPort(InetUtil.getPort());
        return node;
    }

    public static TaskRunErrorAlertEvent newTaskRunErrorAlertEvent(AutoJobTask errorTask, String errorStack) {
        TaskRunErrorAlertEvent taskRunErrorAlertEvent = new TaskRunErrorAlertEvent(String.format("任务：%d执行失败告警", errorTask.getId()), String.format("任务：%d执行失败", errorTask.getId()), errorTask);
        taskRunErrorAlertEvent.setNode(getLocalhostNode());
        taskRunErrorAlertEvent.setStackTrace(errorStack);
        return taskRunErrorAlertEvent;
    }

    public static ClusterOpenProtectedModelAlertEvent newClusterOpenProtectedModelAlertEvent() {
        ClusterNode node = getLocalhostNode();
        ClusterOpenProtectedModelAlertEvent event = new ClusterOpenProtectedModelAlertEvent(String.format("节点：%s:%s开启保护模式告警", node.getHost(), node.getPort()), String.format("节点：%s:%s开启保护模式", node.getHost(), node.getPort()));
        event.setNode(node);
        return event;
    }

    public static ClusterCloseProtectedModelEvent newClusterCloseProtectedModeEvent() {
        ClusterNode node = getLocalhostNode();
        ClusterCloseProtectedModelEvent event = new ClusterCloseProtectedModelEvent(String.format("节点：%s:%s关闭保护模式提醒", node.getHost(), node.getPort()), String.format("节点：%s:%s关闭保护模式", node.getHost(), node.getPort()));
        event.setNode(node);
        return event;
    }

    public static TaskRefuseHandleEvent newTaskRefuseHandleEvent(AutoJobTask refusedTask) {
        TaskRefuseHandleEvent event = new TaskRefuseHandleEvent(String.format("任务：%s被拒绝执行告警", refusedTask.getReference()), String.format("任务：%d被拒绝执行", refusedTask.getId()));
        event.setRefusedTask(refusedTask);
        event.setNode(getLocalhostNode());
        return event;
    }
}
