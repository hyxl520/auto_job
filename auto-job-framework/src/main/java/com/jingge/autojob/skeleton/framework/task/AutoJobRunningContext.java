package com.jingge.autojob.skeleton.framework.task;

import com.jingge.autojob.logging.model.producer.AutoJobLogHelper;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.mail.IMailClient;
import com.jingge.autojob.util.convert.DefaultValueUtil;
import lombok.Getter;

/**
 * 任务运行上下文，上下文在任务启动时会绑定到线程，通过上下文你能得到很多任务相关的信息并且做一些操作
 *
 * @author Huang Yongxiang
 * @date 2023-01-11 9:23
 * @email 1158055613@qq.com
 */
@Getter
public class AutoJobRunningContext {
    /**
     * 当前调度记录ID
     */
    private final Long schedulingRecordID;
    /**
     * 当前执行的任务ID
     */
    private final Long taskId;
    /**
     * 当前任务持有的logHelper
     */
    private final AutoJobLogHelper logHelper;
    /**
     * 当前任务持有的邮件客户端
     */
    private final IMailClient mailClient;
    /**
     * 此次调度的启动时间
     */
    private final Long startTime;
    /**
     * 任务类型
     */
    private final AutoJobTask.TaskType taskType;
    /**
     * 总分片数
     */
    private Object shardingTotal;
    /**
     * 当前获得的分片数
     */
    private Object currentSharding;
    /**
     * 当前任务运行栈
     */
    private final AutoJobRunningStack currentStack;

    AutoJobRunningContext(AutoJobTask task) {
        if (task == null) {
            throw new NullPointerException("无法创建上下文");
        }
        taskId = task.id;
        logHelper = task.logHelper;
        taskType = task.type;
        schedulingRecordID = task
                .getTrigger()
                .getSchedulingRecordID();
        mailClient = DefaultValueUtil.defaultValue(task.mailClient, AutoJobApplication
                .getInstance()
                .getMailClient());
        startTime = task.trigger.triggeringTime;
        if (task.isEnableSharding()) {
            shardingTotal = task
                    .getShardingConfig()
                    .getTotal();
            currentSharding = task
                    .getShardingConfig()
                    .getCurrent();
        }
        currentStack = task.stack;
    }
}
