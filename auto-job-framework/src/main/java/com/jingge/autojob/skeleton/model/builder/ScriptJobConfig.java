package com.jingge.autojob.skeleton.model.builder;

import com.jingge.autojob.skeleton.framework.task.AutoJobTrigger;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-07-27 16:39
 * @email 1158055613@qq.com
 */
@Getter
public class ScriptJobConfig {

    private final AutoJobTrigger trigger;
    private final List<Object> values = new ArrayList<>();
    private final boolean isChildTask;

    private ScriptJobConfig(AutoJobTrigger trigger, List<Object> values, boolean isChildTask) {
        this.trigger = trigger;
        this.values.addAll(values);
        this.isChildTask = isChildTask;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private AutoJobTrigger trigger;
        private final List<Object> values = new ArrayList<>();
        private boolean isChildTask;

        /**
         * 添加一个简单触发器，添加多个触发器时前者将被后者覆盖
         *
         * @param firstTriggeringTime 首次触发时间
         * @param repeatTimes         重复次数，任务总触发次数=1+repeatTimes，-1表示无限次触发
         * @param cycle               周期
         * @param unit                时间单位
         * @return com.example.autojob.skeleton.model.builder.AutoJobScriptTaskBuilder
         * @author Huang Yongxiang
         * @date 2022/10/27 9:59
         */
        public Builder addASimpleTrigger(long firstTriggeringTime, int repeatTimes, long cycle, TimeUnit unit) {
            this.trigger = AutoJobTriggerFactory.newSimpleTrigger(firstTriggeringTime, repeatTimes, cycle, unit);
            return this;
        }

        /**
         * 添加一个cron-like表达式的触发器，添加多个触发器时前者将被后者覆盖
         *
         * @param cronExpression cron-like表达式
         * @param repeatTimes    重复次数，任务总触发次数=1+repeatTimes，-1表示无限次触发
         * @return com.example.autojob.skeleton.model.builder.AutoJobScriptTaskBuilder
         * @author Huang Yongxiang
         * @date 2022/10/27 10:03
         */
        public Builder addACronExpressionTrigger(String cronExpression, int repeatTimes) {
            this.trigger = AutoJobTriggerFactory.newCronExpressionTrigger(cronExpression, repeatTimes);
            return this;
        }

        /**
         * 添加一个子任务触发器，该任务将会作为一个子任务参与调度，添加多个触发器时前者将被后者覆盖
         *
         * @return com.example.autojob.skeleton.model.builder.AutoJobScriptTaskBuilder
         * @author Huang Yongxiang
         * @date 2022/10/27 10:09
         */
        public Builder addAChildTaskTrigger() {
            this.trigger = AutoJobTriggerFactory.newChildTrigger();
            isChildTask = true;
            return this;
        }

        /**
         * 添加一个延迟触发器，任务将会在给定延迟后触发一次，添加多个触发器时前者将被后者覆盖
         *
         * @param delay 距离现在延迟执行的时间
         * @param unit  时间单位
         * @return com.example.autojob.skeleton.model.builder.AutoJobScriptTaskBuilder
         * @author Huang Yongxiang
         * @date 2022/10/27 10:12
         */
        public Builder addADelayTrigger(long delay, TimeUnit unit) {
            this.trigger = AutoJobTriggerFactory.newDelayTrigger(delay, unit);
            return this;
        }

        /**
         * 添加一个插值，注意该方法调用顺序
         *
         * @param value 放入的插值
         * @return com.example.autojob.skeleton.model.builder.ScriptJobConfig.Builder
         * @author JingGe(* ^ ▽ ^ *)
         * @date 2023/7/28 14:00
         */
        public Builder addValue(Object value) {
            values.add(value);
            return this;
        }

        public ScriptJobConfig build() {
            return new ScriptJobConfig(trigger, values, isChildTask);
        }
    }
}
