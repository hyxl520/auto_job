package com.jingge.autojob.skeleton.lifecycle.event.imp;

import com.jingge.autojob.skeleton.cluster.model.ClusterNode;
import com.jingge.autojob.skeleton.lifecycle.event.TaskEvent;
import lombok.Getter;
import lombok.Setter;

/**
 * 任务转移事件，集群部署下该节点任务最终执行失败进行故障转移后发布
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/01 17:38
 */
@Getter
@Setter
public class TaskTransferEvent extends TaskEvent {
    private ClusterNode transferTo;
    private String logKey;

    public TaskTransferEvent() {
        super();
        this.level = "INFO";
    }
}
