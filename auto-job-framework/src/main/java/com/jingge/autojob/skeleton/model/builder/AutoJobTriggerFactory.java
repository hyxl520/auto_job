package com.jingge.autojob.skeleton.model.builder;

import com.jingge.autojob.skeleton.framework.task.AutoJobTrigger;
import com.jingge.autojob.skeleton.lang.IAutoJobFactory;

import java.util.concurrent.TimeUnit;

/**
 * 触发器工厂
 *
 * @Author Huang Yongxiang
 * @Date 2022/10/27 10:38
 * @Email 1158055613@qq.com
 */
public class AutoJobTriggerFactory implements IAutoJobFactory {
    public static AutoJobTrigger newSimpleTrigger(long firstTriggeringTime, int repeatTimes, long cycle, TimeUnit unit) {
        return new AutoJobTrigger(firstTriggeringTime, repeatTimes, unit.toMillis(cycle));
    }

    public static AutoJobTrigger newCronExpressionTrigger(String cronExpression, int repeatTimes) {
        return new AutoJobTrigger(cronExpression, repeatTimes);
    }

    public static AutoJobTrigger newChildTrigger() {
        return new AutoJobTrigger(Long.MAX_VALUE, 0, 0);
    }

    public static AutoJobTrigger newDelayTrigger(long delay, TimeUnit unit) {
        return new AutoJobTrigger(System.currentTimeMillis() + unit.toMillis(delay), 0, 0);
    }
}
