package com.jingge.autojob.logging.domain;

import com.jingge.autojob.skeleton.framework.task.AutoJobRunResult;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.util.json.JsonUtil;
import lombok.Data;

import java.util.Date;

/**
 * 调度记录
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/29 16:30
 */
@Data
public class AutoJobSchedulingRecord {
    /**
     * 调度ID
     */
    private long schedulingId;
    /**
     * 调度时间
     */
    private Date schedulingTime;
    /**
     * 任务别名
     */
    private String taskAlias;
    /**
     * 任务id
     */
    private Long taskId;
    /**
     * 是否成功
     */
    private boolean isSuccess;
    /**
     * 是否正在运行
     */
    private boolean isRun;
    /**
     * 调度类型 0-普通调度 1-重试调度 2-分片调度
     */
    private Integer schedulingType;
    /**
     * 分片ID
     */
    private Long shardingId;
    /**
     * 执行结果
     */
    private String result;
    /**
     * 执行时长 ms
     */
    private long executionTime;

    public AutoJobSchedulingRecord() {
    }

    public AutoJobSchedulingRecord(AutoJobTask task) {
        if (!task.getIsChildTask()) {
            schedulingTime = new Date(task
                    .getTrigger()
                    .getTriggeringTime());
        } else {
            schedulingTime = new Date();
        }
        taskAlias = task.getAlias();
        taskId = task.getId();
        isRun = task
                .getTrigger()
                .getIsRunning();
        shardingId = task.getShardingId();
        if (task
                .getTrigger()
                .getIsRetrying()) {
            schedulingType = 1;
        } else if (task.getIsShardingTask()) {
            schedulingType = 2;
        } else {
            schedulingType = 0;
        }
        AutoJobRunResult runResult = task.getRunResult();
        if (runResult != null && runResult.hasResult()) {
            isSuccess = task
                    .getRunResult()
                    .isRunSuccess();
            executionTime = task
                    .getRunResult()
                    .getFinishedTime() - task
                    .getTrigger()
                    .getTriggeringTime();
            if (runResult.getResult() != null) {
                result = JsonUtil.pojoToJsonString(task
                        .getRunResult()
                        .getResult());
            }
        }
    }
}
