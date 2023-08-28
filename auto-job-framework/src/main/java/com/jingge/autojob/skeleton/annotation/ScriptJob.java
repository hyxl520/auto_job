package com.jingge.autojob.skeleton.annotation;

import com.jingge.autojob.skeleton.enumerate.SaveStrategy;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.model.builder.AutoJobScriptTaskBuilder;
import com.jingge.autojob.skeleton.model.builder.ScriptJobConfig;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 通过注解方式定义ScriptJob
 * ，这种方式创建的脚本任务非常简单，仅仅支持命令，想要更丰富的支持，请使用{@link AutoJobScriptTaskBuilder}来创建脚本任务，该注解标注在一个方法上，这个方法的返回值必须是{@link ScriptJobConfig}
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-07-27 16:27
 * @email 1158055613@qq.com
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ScriptJob {
    /**
     * 要执行的命令行或命令行模板，使用{}来替代插值
     */
    String value();

    /**
     * 任务类型
     */
    AutoJobTask.TaskType taskType() default AutoJobTask.TaskType.MEMORY_TASk;

    /**
     * 保存策略
     */
    SaveStrategy saveStrategy() default SaveStrategy.SAVE_IF_ABSENT;

    /**
     * 版本ID，等同于{@link AutoJob}里面的{@link AutoJob#id()}
     */
    long versionID() default -1;

    /**
     * 脚本最长执行时间
     */
    long maximumExecutingTime() default 24 * 60 * 60;

    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 子任务ID，可以是版本ID，也可以是任务ID；多个逗号分割，特别注意，内存任务的子任务只能是内存任务，DB任务的子任务只能是DB任务
     */
    String childTasksId() default "";

    /**
     * 分片配置
     */
    ShardingConfig shardingConfig() default @ShardingConfig;
}
