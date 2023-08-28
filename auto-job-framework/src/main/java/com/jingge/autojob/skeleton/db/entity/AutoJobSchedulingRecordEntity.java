package com.jingge.autojob.skeleton.db.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

/**
 * 调度记录
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/29 16:21
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class AutoJobSchedulingRecordEntity {
    /**
     * 主键Id
     */
    private Long id;

    /**
     * 写入时间戳
     */
    private Long writeTimestamp;

    /**
     * 调度时间
     */
    private Timestamp schedulingTime;
    /**
     * 任务别名
     */
    private String taskAlias;
    /**
     * 任务id
     */
    private Long taskId;
    /**
     * 是否成功 0-否 1-是
     */
    private Integer isSuccess;
    /**
     * 是否正在运行 1-是 0-否
     */
    private Integer isRun;
    /**
     * 调度类型 0-普通调度 1-重试调度 2-分片调度
     */
    private Integer schedulingType;
    /**
     * 分片ID
     */
    private Long shardingId;
    /**
     * 执行机器
     */
    private String executingMachine;
    /**
     * 执行结果 JSON序列化
     */
    private String result;
    /**
     * 执行时长 ms
     */
    private Long executionTime;
    /**
     * 删除标识
     */
    private Integer delFlag;
}
