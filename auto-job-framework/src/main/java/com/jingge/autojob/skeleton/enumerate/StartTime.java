package com.jingge.autojob.skeleton.enumerate;

import com.jingge.autojob.skeleton.framework.config.TimeConstant;

/**
 * 提供部分默认启动时间
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/13 9:53
 */
public enum StartTime {
    /**
     * 现在（为了能够被准确执行，时间将会基于注册时间偏移15秒）
     */
    NOW(15000),

    /**
     * 稍后一分钟
     */
    THEN_ONE_MINUTE(TimeConstant.A_MINUTE),

    /**
     * 稍后三分钟
     */
    THEN_THREE_MINUTES(TimeConstant.A_MINUTE * 3),

    /**
     * 稍后五分钟
     */
    THEN_FIVE_MINUTES(TimeConstant.A_MINUTE * 5),

    /**
     * 稍后半小时
     */
    THEN_HALF_HOUR(TimeConstant.A_MINUTE * 30),

    /**
     * 稍后一个小时
     */
    THEN_HOUR(TimeConstant.A_HOUR),

    /**
     * empty
     */
    EMPTY(-1L);

    private final long offset;

    StartTime(long startTime) {
        this.offset = startTime;
    }

    public long valueOf() {
        return System.currentTimeMillis() + offset;
    }
}
