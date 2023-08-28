package com.jingge.autojob.skeleton.db.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * <p>
 * 任务日志表
 * </p>
 *
 * @author Huang Yongxiang
 * @since 2022-08-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class AutoJobLogEntity implements Serializable {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 调度id
     */
    private Long schedulingId;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 录入时间戳
     */
    private Long writeTimestamp;

    /**
     * 写入时间
     */
    private Timestamp writeTime;

    /**
     * 日志级别
     */
    private String logLevel;

    /**
     * 记录信息
     */
    private String message;

    private Integer delFlag;


}
