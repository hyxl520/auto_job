package com.jingge.autojob.skeleton.framework.task;

import com.jingge.autojob.skeleton.framework.config.TimeConstant;
import com.jingge.autojob.util.convert.StringUtils;
import com.jingge.autojob.util.cron.util.CronUtil;
import com.jingge.autojob.util.id.IdGenerator;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 任务触发器
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/11 9:52
 */
@Getter
@Setter
@Slf4j
@Accessors(chain = true)
public class AutoJobTrigger {
    /**
     * 触发器ID
     */
    private Long triggerId;
    /**
     * 触发时间
     */
    protected Long triggeringTime;
    /**
     * 当前触发记录ID
     */
    protected Long schedulingRecordID;
    /**
     * cron like表达式
     */
    protected String cronExpression;
    /**
     * 重复次数
     */
    protected Integer repeatTimes;
    /**
     * 已完成次数
     */
    protected AtomicInteger finishedTimes;
    /**
     * 当前重试次数
     */
    protected AtomicInteger currentRepeatTimes;
    /**
     * 上次触发时间
     */
    protected Long lastTriggeringTime;
    /**
     * 上次是否运行成功
     */
    protected Boolean isLastSuccess;
    /**
     * 任务主键
     */
    protected Long taskId;
    /**
     * 子任务的ID列表
     */
    protected List<Long> childTask;
    /**
     * 周期
     */
    protected Long cycle;
    /**
     * 最大运行时长，毫秒
     */
    protected Long maximumExecutionTime;
    /**
     * 上次运行时间
     */
    protected long lastRunTime = 0;
    /**
     * 最近一次启动时间
     */
    private long startRunTime;
    /**
     * 是否正在运行
     */
    private Boolean isRunning = false;
    /**
     * 是否暂停
     */
    private Boolean isPause = false;
    /**
     * 是否是调度重试
     */
    private Boolean isRetrying = false;

    public AutoJobTrigger(Long triggeringTime, String cronExpression, Integer repeatTimes, int finishedTimes, long cycle) {
        this.cronExpression = cronExpression;
        this.cycle = cycle;
        this.repeatTimes = repeatTimes;
        this.maximumExecutionTime = TimeConstant.A_DAY;
        this.finishedTimes = new AtomicInteger(finishedTimes);
        this.currentRepeatTimes = new AtomicInteger(0);
        if (this.repeatTimes == null) {
            this.repeatTimes = -1;
        }
        if (triggeringTime == null) {
            this.triggeringTime = computeNextTriggeringTime();
        } else {
            this.triggeringTime = triggeringTime;
        }

    }

    public AutoJobTrigger(long triggeringTime, String cronExpression) {
        this(triggeringTime, cronExpression, -1, 0, 0L);
    }


    /**
     * 创建一个单一的周期性的触发器
     *
     * @param triggeringTime 任务的起点时间，下次调度时间将会基于此时间计算
     * @param repeatTimes    总执行次数
     * @param cycle          周期 ms
     * @author Huang Yongxiang
     * @date 2022/8/11 15:07
     */
    public AutoJobTrigger(Long triggeringTime, int repeatTimes, long cycle) {
        this(triggeringTime, null, repeatTimes, 0, cycle);
    }

    /**
     * 创建一个可结束的cron-like触发器
     *
     * @param cronExpression cron-like表达式
     * @param repeatTimes    重复次数 >0可结束，-1为永久
     * @author Huang Yongxiang
     * @date 2022/8/11 15:06
     */
    public AutoJobTrigger(String cronExpression, Integer repeatTimes) {
        this(null, cronExpression, repeatTimes, 0, 0L);
    }

    /**
     * 创建一个CronLike的触发器
     *
     * @param cronExpression cron-like表达式
     * @author Huang Yongxiang
     * @date 2022/8/11 15:05
     */
    public AutoJobTrigger(String cronExpression) {
        this(cronExpression, -1);
    }

    public AutoJobTrigger(Integer repeatTimes, Long cycle) {
        this(null, repeatTimes, cycle);
    }

    public AutoJobTrigger() {
    }

    /**
     * 调用此方法后，调度器会记录一个启动时间，与之对应的方法是finished，配套记录任务运行时长，用于优化任务调度
     *
     * @return void
     * @author Huang Yongxiang
     * @date 2022/8/11 14:58
     */
    protected void start() {
        startRunTime = System.currentTimeMillis();
        schedulingRecordID = IdGenerator.getNextIdAsLong();
    }

    protected void finished() {
        if (startRunTime < 0) {
            throw new UnsupportedOperationException("本次调度未记录启动时间");
        }
        lastRunTime = System.currentTimeMillis() - startRunTime;
    }

    public boolean hasChildTask() {
        return childTask != null && childTask.size() > 0;
    }

    /**
     * 获取任务上次执行时长，与start和finished方法配套使用
     *
     * @return long
     * @author Huang Yongxiang
     * @date 2022/8/11 15:00
     */
    public long getLastRunTime() {
        return lastRunTime;
    }

    public boolean isReachTriggerTime() {
        long in = triggeringTime - System.currentTimeMillis();
        return in >= 0 && in < 1000;
    }

    /**
     * 是否已靠近触发时间
     *
     * @param mills 毫秒
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/8/20 17:20
     */
    public boolean isNearTriggeringTime(long mills) {
        if (isReachTriggerTime()) {
            return true;
        }
        long now = System.currentTimeMillis();
        return now < triggeringTime && now >= triggeringTime - mills;
    }


    public AutoJobTrigger setFinishedTimes(int finishedTimes) {
        if (this.finishedTimes == null) {
            this.finishedTimes = new AtomicInteger(finishedTimes);
            return this;
        }
        this.finishedTimes.set(finishedTimes);
        return this;
    }

    public int getFinishedTimes() {
        if (finishedTimes == null) {
            return 0;
        }
        return finishedTimes.get();
    }

    /**
     * 获取调度器的下次执行时间，初始调度时间指定的情况下计算将从起点时间开始，否则基于现在
     *
     * @return long 下次触发时间
     * @author Huang Yongxiang
     * @date 2022/8/11 15:11
     */
    public long nextTriggeringTime() {
        if (!isNextReachable()) {
            return -1;
        }
        return computeNextTriggeringTime();
    }


    private long computeNextTriggeringTime() {
        long next = 0;
        if (!StringUtils.isEmpty(cronExpression)) {
            next = CronUtil
                    .next(cronExpression, new Date())
                    .getTime();
        } else {
            next = System.currentTimeMillis() + cycle;
        }
        return next - next % 1000;
    }

    public long getTheNThTriggeringTime(int n) {
        if (!isTheNThReachable(n)) {
            return -1;
        }
        List<Long> future = futureTriggeringTime(n);
        return future.get(future.size() - 1);
    }

    public boolean update() {
        finishedTimes.incrementAndGet();
        return true;
    }

    public boolean isNextReachable() {
        if (isPause != null && isPause) {
            return false;
        }
        if (repeatTimes < 0) {
            return true;
        }
        return finishedTimes.get() <= repeatTimes && (!StringUtils.isEmpty(cronExpression) || cycle != null);
    }

    public boolean isTheNThReachable(int n) {
        if (isPause != null && isPause) {
            return false;
        }
        if (repeatTimes < 0) {
            return true;
        }
        return n <= repeatTimes - finishedTimes.get() && (!StringUtils.isEmpty(cronExpression) || cycle != null);
    }


    /**
     * 刷新触发时间
     *
     * @return boolean 是否具有下次执行的机会
     * @author Huang Yongxiang
     * @date 2022/8/11 10:25
     */
    public boolean refresh() {
        if (!isNextReachable()) {
            return false;
        }
        triggeringTime = nextTriggeringTime();
        return triggeringTime > 0;
    }

    /**
     * 返回未来的n次触发时间，n取决于count、重复次数以及已完成次数，可能n会小于count
     *
     * @param count 要返回的未来调度的次数
     * @return java.util.List<java.lang.Long>
     * @author Huang Yongxiang
     * @date 2022/8/11 10:11
     */
    public List<Long> futureTriggeringTime(int count) {
        if (!isTheNThReachable(count)) {
            return Collections.emptyList();
        }
        int scheduleCount = Math.min(count, repeatTimes - finishedTimes.get());

        if (!StringUtils.isEmpty(cronExpression)) {
            return CronUtil
                    .nextCount(scheduleCount, cronExpression, new Date())
                    .stream()
                    .map(Date::getTime)
                    .collect(Collectors.toList());
        } else {
            List<Long> future = new ArrayList<>();
            long last = new Date().getTime();
            for (int i = 0; i < scheduleCount; i++) {
                long next = last + cycle;
                future.add(next);
                last = next;
            }
            return future;
        }
    }


}
