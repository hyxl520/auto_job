package com.jingge.autojob.skeleton.db.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * <p>
 * 触发器表
 * </p>
 *
 * @author Huang Yongxiang
 * @since 2022-08-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class AutoJobTriggerEntity implements Serializable {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * cron-like表达式
     */
    private String cronExpression;

    /**
     * 上次运行时长
     */
    private Long lastRunTime;

    /**
     * 上次触发时间
     */
    private Long lastTriggeringTime;

    /**
     * 下次触发时间
     */
    private Long nextTriggeringTime;

    /**
     * 上次调度是否成功 0-否 1-是
     */
    private Integer isLastSuccess;

    /**
     * 总需执行次数
     */
    private Integer repeatTimes;

    /**
     * 已完成次数
     */
    private Integer finishedTimes;

    /**
     * 当前重试次数
     */
    private Integer currentRepeatTimes;

    /**
     * 任务周期
     */
    private Long cycle;

    /**
     * 任务Id
     */
    private Long taskId;

    /**
     * 子任务ID，多个逗号分割
     */
    private String childTasksId;

    /**
     * 最大运行时长
     */
    protected Long maximumExecutionTime;

    /**
     * 是否正在运行 0-否 1-是
     */
    protected Integer isRun;

    /**
     * 是否停止调度 0-否 1-是
     */
    private Integer isPause;

    /**
     * 创建时间
     */
    private Timestamp createTime;

    /**
     * 逻辑删除标识
     */
    private Integer delFlag;


}
