package com.jingge.autojob.skeleton.annotation;

import com.jingge.autojob.skeleton.enumerate.SaveStrategy;
import com.jingge.autojob.skeleton.enumerate.SchedulingStrategy;
import com.jingge.autojob.skeleton.enumerate.StartTime;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.model.executor.DefaultMethodObjectFactory;
import com.jingge.autojob.skeleton.model.executor.IMethodObjectFactory;
import com.jingge.autojob.skeleton.model.task.method.MethodTask;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 该注解用在某个方法上，该方法在框架启动时会被扫描到并且被包装成一个{@link MethodTask}对象参与调度
 *
 * @Auther Huang Yongxiang
 * @Date 2022/01/23 18:42
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoJob {
    /*=================基础信息=================>*/

    /**
     * 版本ID，指定该ID有一定好处，比如在子任务上指定id，可以通过{@link #childTasksId()}关联到该子任务；对于DB任务，该id可以作为一个版本id，同一个版本id里的DB任务为同一个注解扫描任务的不同版本，任务调度的一定是最新版本，DB任务不指定该ID时会基于任务引用的哈希码生成，会有一定的冲突率，因此建议在DB型任务上指定该字段，指定的ID必须是非负数
     */
    long id() default -1L;

    /**
     * 任务别名
     */
    String alias() default "Default";

    /**
     * 参数，支持simple参数
     */
    String attributes() default "";

    /**
     * 子任务ID，可以是版本ID，也可以是任务ID；多个逗号分割，特别注意，内存任务的子任务只能是内存任务，DB任务的子任务只能是DB任务
     */
    String childTasksId() default "";

    /**
     * 最大执行时长，毫秒，超出时系统将会尝试停止，默认是24小时
     */
    long maximumExecutionTime() default 24 * 60 * 60 * 1000;

    /**
     * 方法依赖的类工厂，工厂必须提供无参构造方法
     */
    Class<? extends IMethodObjectFactory> methodObjectFactory() default DefaultMethodObjectFactory.class;

    /**
     * 该job以何种类型方式调度
     */
    AutoJobTask.TaskType asType() default AutoJobTask.TaskType.MEMORY_TASk;

    /**
     * 任务级别，相同时间情况下高优先级任务会被优先调度
     */
    int taskLevel() default -1;

    /**
     * 可执行的机器地址，ip or ip:port，port是cluster config 中的port
     */
    String executableMachines() default "";
    /*=======================Finished======================<*/


    /*=================调度信息=================>*/

    /**
     * 分片配置
     */
    ShardingConfig shardingConfig() default @ShardingConfig();

    /**
     * 保存策略
     */
    SaveStrategy saveStrategy() default SaveStrategy.SAVE_IF_ABSENT;

    /**
     * 调度策略
     */
    SchedulingStrategy schedulingStrategy() default SchedulingStrategy.JOIN_SCHEDULING;

    /**
     * 启动时间 yyyy-MM-dd HH:mm:ss格式
     */
    String startTime() default "";

    /**
     * 默认启动时间，和startTime同时存在时以startTime为准
     */
    StartTime defaultStartTime() default StartTime.EMPTY;

    /**
     * cron like表达式，存在时优先使用该表达式进行调度
     */
    String cronExpression() default "";

    /**
     * 重复次数，一个任务的总执行次数=1+repeatTimes，-1表示永久执行
     */
    int repeatTimes() default 0;

    /**
     * 周期：默认为秒，当重复次数大于0时该值必须大于0
     */
    long cycle() default 0;

    /**
     * 周期的时间单位，默认为秒
     */
    TimeUnit cycleUnit() default TimeUnit.SECONDS;
    /*=======================Finished======================<*/


}
