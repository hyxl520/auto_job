package com.jingge.autojob.logging.domain;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @Description 任务的执行日志
 * @Auther Huang Yongxiang
 * @Date 2022/01/19 15:36
 */
@Data
@Accessors(chain = true)
public class AutoJobRunLog {
    private Long id;
    /**
     * 调度id
     */
    private Long schedulingId;
    private Long taskId;
    private String taskType;
    private Integer runStatus;
    private String message;
    private String writeTime;
    private String errorStack;
    private String runResult;
}
