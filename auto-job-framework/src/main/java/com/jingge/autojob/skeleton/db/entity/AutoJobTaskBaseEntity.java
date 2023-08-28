package com.jingge.autojob.skeleton.db.entity;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

/**
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-03-15 14:44
 * @email 1158055613@qq.com
 */
@Getter
@Setter
public class AutoJobTaskBaseEntity {
    /**
     * 主键ID
     */
    protected Long id;

    /**
     * 任务别名
     */
    protected String alias;

    /**
     * 版本ID
     */
    protected Long VersionId;

    /**
     * 任务参数
     */
    protected String params;

    /**
     * 任务对应的触发器
     */
    protected Long triggerId;

    /**
     * 任务类型，目前已占用的类型有：0-方法型任务 1-脚本型任务
     */
    protected Integer type;

    /**
     * 是否是子任务
     */
    protected Integer isChildTask;

    /**
     * 是否是分片任务
     */
    protected Integer isShardingTask;

    /**
     * 启动锁 0-未上锁 1-已上锁
     */
    protected Integer runLock;

    /**
     * 任务优先级
     */
    protected Integer taskLevel;

    /**
     * 版本号
     */
    protected Long version;

    /**
     * 运行状态
     */
    protected Integer runningStatus;

    /**
     * 预留字段，所属于
     */
    protected Long belongTo;

    /**
     * 状态 0-停用 1-启用
     */
    protected Integer status;

    /**
     * 执行的机器，指定后只有指定机器才能执行该任务
     */
    protected String executableMachines;

    /**
     * 创建时间
     */
    protected Timestamp createTime;

    /**
     * 删除标识
     */
    protected Integer delFlag;

}
