package com.jingge.autojob.skeleton.framework.task;

import com.jingge.autojob.skeleton.enumerate.SaveStrategy;
import com.jingge.autojob.skeleton.enumerate.SchedulingStrategy;
import com.jingge.autojob.skeleton.framework.config.AutoJobMailConfig;
import com.jingge.autojob.skeleton.framework.config.AutoJobRetryConfig;
import com.jingge.autojob.skeleton.framework.config.AutoJobShardingConfig;
import com.jingge.autojob.skeleton.model.executor.IMethodObjectFactory;

import java.util.List;

/**
 * 模板公共接口
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-03-28 16:21
 * @email 1158055613@qq.com
 */
public interface Template {

    long id();

    Long versionID();

    AutoJobTask.TaskType taskType();

    AutoJobRetryConfig retryConfig();

    String alias();

    Object[] params();

    String cron();

    List<String> executableMachines();

    int repeatTimes();

    long maximumExecutionTime();

    IMethodObjectFactory methodObjectFactory();

    List<Long> childTasks();

    SchedulingStrategy schedulingStrategy();

    AutoJobMailConfig mailConfig();

    ShardingStrategy shardingStrategy();

    AutoJobShardingConfig shardingConfig();

    SaveStrategy saveStrategy();
}
