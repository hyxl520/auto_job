package com.jingge.autojob.skeleton.model.handler;

import com.jingge.autojob.skeleton.annotation.AutoJob;
import com.jingge.autojob.skeleton.enumerate.SchedulingStrategy;
import com.jingge.autojob.skeleton.framework.config.AutoJobRetryConfig;
import com.jingge.autojob.skeleton.framework.config.AutoJobShardingConfig;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.framework.task.AutoJobWrapper;
import com.jingge.autojob.skeleton.model.builder.AutoJobMethodTaskBuilder;
import com.jingge.autojob.util.bean.ObjectUtil;
import com.jingge.autojob.util.convert.DefaultValueUtil;
import com.jingge.autojob.util.convert.StringUtils;
import com.jingge.autojob.util.id.IdGenerator;
import com.jingge.autojob.util.servlet.InetUtil;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * AutoJob注解包装器
 *
 * @author Huang Yongxiang
 * @date 2022-12-03 18:04
 * @email 1158055613@qq.com
 */
public class AutoJobAnnotationWrapper implements AutoJobWrapper {
    @Override
    public AutoJobTask wrapper(Method method, Class<?> clazz) {
        AutoJob autoJob = method.getAnnotation(AutoJob.class);
        if (autoJob == null) {
            return null;
        }
        long taskId;
        if (autoJob.id() < 0 || autoJob.asType() == AutoJobTask.TaskType.DB_TASK) {
            taskId = IdGenerator.getNextIdAsLong();
        } else {
            taskId = autoJob.id();
        }
        AutoJobShardingConfig shardingConfig = new AutoJobShardingConfig(taskId, autoJob
                .shardingConfig()
                .enable(), autoJob
                .shardingConfig()
                .total()).setEnableShardingRetry(autoJob
                .shardingConfig()
                .enableShardingRetry());
        return new AutoJobMethodTaskBuilder(method.getDeclaringClass(), method.getName())
                .setTaskId(taskId)
                .setSaveStrategy(autoJob.saveStrategy())
                .setTaskAlias(DefaultValueUtil.chooseString("Default".equals(autoJob.alias()), method.getName(), autoJob.alias()))
                .addAllExecutableMachine(getExecutableMachine(autoJob.executableMachines()))
                .setParams(autoJob.attributes())
                .setMethodObjectFactory(ObjectUtil.getClassInstance(autoJob.methodObjectFactory()))
                .setTaskType(autoJob.asType())
                .setShardingConfig(shardingConfig)
                .setSchedulingStrategy(autoJob.schedulingStrategy())
                .setTaskLevel(autoJob.taskLevel())
                .setRetryConfig(new AutoJobRetryConfig().setTaskId(taskId))
                .setTrigger(autoJob
                        .schedulingStrategy()
                        .createTrigger(taskId, autoJob))
                .build()
                .setVersionId((Long) DefaultValueUtil.chooseNumber(autoJob.id() == -1 || autoJob.asType() == AutoJobTask.TaskType.MEMORY_TASk, null, autoJob.id()))
                .setIsChildTask(autoJob.schedulingStrategy() == SchedulingStrategy.AS_CHILD_TASK);
    }

    private List<String> getExecutableMachine(String machines) {
        if (StringUtils.isEmpty(machines)) {
            return Collections.emptyList();
        }
        List<String> mList = new LinkedList<>();
        String[] ms = machines.split(",");
        for (String s : ms) {
            if ("localhost".equalsIgnoreCase(s.trim()) || "local".equalsIgnoreCase(s.trim())) {
                mList.add(InetUtil.getLocalhostIp());
            } else {
                mList.add(s);
            }
        }
        return mList;
    }

}
