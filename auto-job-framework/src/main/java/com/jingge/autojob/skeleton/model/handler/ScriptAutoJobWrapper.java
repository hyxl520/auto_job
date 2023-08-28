package com.jingge.autojob.skeleton.model.handler;

import com.jingge.autojob.skeleton.annotation.ScriptJob;
import com.jingge.autojob.skeleton.enumerate.SchedulingStrategy;
import com.jingge.autojob.skeleton.framework.config.AutoJobRetryConfig;
import com.jingge.autojob.skeleton.framework.config.AutoJobShardingConfig;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.framework.task.AutoJobWrapper;
import com.jingge.autojob.skeleton.model.builder.AutoJobScriptTaskBuilder;
import com.jingge.autojob.skeleton.model.builder.ScriptJobConfig;
import com.jingge.autojob.util.bean.ObjectUtil;
import com.jingge.autojob.util.convert.StringUtils;
import com.jingge.autojob.util.id.IdGenerator;
import com.jingge.autojob.util.message.MessageManager;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-07-27 16:56
 * @email 1158055613@qq.com
 */
@Slf4j
public class ScriptAutoJobWrapper implements AutoJobWrapper {
    @Override
    public AutoJobTask wrapper(Method method, Class<?> clazz) {
        if (method == null || clazz == null) {
            return null;
        }
        ScriptJob scriptJob = method.getAnnotation(ScriptJob.class);
        if (scriptJob == null || StringUtils.isEmpty(scriptJob.value())) {
            return null;
        }
        if (!ScriptJobConfig.class.isAssignableFrom(method.getReturnType())) {
            log.warn("方法：{}#{}的返回类型不符合注解@ScriptJob的要求", clazz.getName(), method.getName());
            return null;
        }
        try {
            ScriptJobConfig config = (ScriptJobConfig) method.invoke(ObjectUtil.getClassInstance(clazz));
            if (config == null) {
                return null;
            }
            Object[] values = new Object[config
                    .getValues()
                    .size()];
            for (int i = 0; i < config
                    .getValues()
                    .size(); i++) {
                values[i] = config
                        .getValues()
                        .get(i);
            }
            String cmd = MessageManager.formatMsgLikeSlf4j(scriptJob.value(), values);
            long taskID = IdGenerator.getNextIdAsLong();
            AutoJobShardingConfig shardingConfig = new AutoJobShardingConfig(taskID, scriptJob
                    .shardingConfig()
                    .enable(), scriptJob
                    .shardingConfig()
                    .total()).setEnableShardingRetry(scriptJob
                    .shardingConfig()
                    .enableShardingRetry());
            List<Long> childTaskList = SchedulingStrategy.splitChildTaskId(scriptJob.childTasksId());
            return new AutoJobScriptTaskBuilder()
                    .setVersionID(scriptJob.versionID() == -1 ? null : scriptJob.versionID())
                    .setTaskId(taskID)
                    .setTrigger(config
                            .getTrigger()
                            .setMaximumExecutionTime(scriptJob
                                    .timeUnit()
                                    .toMillis(scriptJob.maximumExecutingTime()))
                            .setChildTask(childTaskList))
                    .setShardingConfig(shardingConfig)
                    .setSaveStrategy(scriptJob.saveStrategy())
                    .setChildTask(config.isChildTask())
                    .setTaskType(scriptJob.taskType())
                    .setRetryConfig(new AutoJobRetryConfig())
                    .setSchedulingStrategy(config.isChildTask() ? SchedulingStrategy.AS_CHILD_TASK : SchedulingStrategy.JOIN_SCHEDULING)
                    .createNewWithCmd(cmd);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }
}
