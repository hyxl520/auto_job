package com.jingge.autojob.skeleton.framework.config;

/**
 * 框架的一些常量
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-03-15 14:18
 * @email 1158055613@qq.com
 */
public class AutoJobConstant {
    /**
     * 提前调度进调度队列的时间
     */
    public static final long beforeSchedulingInQueue = 10 * 1000;
    /**
     * DB调度器执行的周期
     */
    public static final long dbSchedulerRate = 5 * 1000;
    /**
     * 内存调度器执行的周期
     */
    public static final long memorySchedulerRate = 1000;
    /**
     * 提前调度进时间轮的时间
     */
    public static final long beforeSchedulingInTimeWheel = 5 * 1000;
}
