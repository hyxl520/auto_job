package com.jingge.autojob.skeleton.db.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * <p>
 * 任务调度日志表
 * </p>
 *
 * @author Huang Yongxiang
 * @since 2022-08-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class AutoJobRunLogEntity implements Serializable {

    /**
     * 主键
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
     * 任务类型：MEMORY_TASK：内存型任务 DB_TASK：数据库任务
     */
    private String taskType;

    /**
     * 1：运行成功 0：运行失败
     */
    private Integer runStatus;

    /**
     * 调度次数
     */
    private Integer scheduleTimes;

    /**
     * 信息
     */
    private String message;

    /**
     * 任务结果
     */
    private String result;

    /**
     * 错误堆栈
     */
    private String errorStack;

    /**
     * 录入时间戳
     */
    private Long writeTimestamp;

    /**
     * 录入时间
     */
    private Timestamp writeTime;

    /**
     * 删除标识
     */
    private Integer delFlag;


}
