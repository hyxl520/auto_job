package com.jingge.autojob.logging.domain;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 任务的运行日志
 *
 * @Auther Huang Yongxiang
 * @Date 2022/01/13 13:25
 */
@Data
@Accessors(chain = true)
public class AutoJobLog {
    private long id;
    /**
     * 调度id
     */
    private Long schedulingId;
    private long taskId;
    private String inputTime;
    private String level;
    private String message;
}
