package com.jingge.autojob.skeleton.lifecycle.event.imp;

import com.jingge.autojob.skeleton.cluster.model.ClusterNode;
import com.jingge.autojob.skeleton.lifecycle.event.TaskEvent;
import lombok.Getter;
import lombok.Setter;

/**
 * 任务接收事件，集群部署下该节点接收到其他节点转移的任务时发布
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/01 17:41
 */
@Getter
@Setter
public class TaskReceivedEvent extends TaskEvent {
    private ClusterNode transferFrom;

    public TaskReceivedEvent() {
        super();
        this.level = "INFO";
    }
}
