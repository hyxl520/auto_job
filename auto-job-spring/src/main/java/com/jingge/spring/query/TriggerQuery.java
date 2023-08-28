package com.jingge.spring.query;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Huang Yongxiang
 * @date 2022-12-22 16:50
 * @email 1158055613@qq.com
 */
@Getter
@Setter
public class TriggerQuery {
    /**
     * 类型 0-cron like触发器 1-simple触发器 2-child触发器 3-Delay触发器
     */
    private Integer type;
    /**
     * cron like表达式
     */
    private String cronExpression;
    /**
     * 重复次数
     */
    private Integer repeatTimes;
    /**
     * 触发时间
     */
    private Long triggeringTime;
    /**
     * 周期 秒
     */
    private Long cycle;
    /**
     * 延迟 秒
     */
    private Long delay;
    /**
     * 最大执行时长 毫秒
     */
    private Long maximumExecutionTime;
    /**
     * 子任务id，逗号分割
     */
    private String childTasksId;
}
