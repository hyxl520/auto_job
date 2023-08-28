package com.jingge.autojob.api.task.params;

import lombok.Data;

/**
 * 触发器修改参数
 *
 * @author Huang Yongxiang
 * @date 2022-12-01 14:10
 * @email 1158055613@qq.com
 */
@Data
public class TriggerEditParams {
    /**
     * cron-like表达式
     */
    private String cronExpression;
    /**
     * 任务周期
     */
    private Long cycle;
    /**
     * 子任务ID，多个逗号分割
     */
    private String childTasksId;
    /**
     * 总需执行次数
     */
    private Integer repeatTimes;
    /**
     * 最大运行时长
     */
    private Long maximumExecutionTime;
}
