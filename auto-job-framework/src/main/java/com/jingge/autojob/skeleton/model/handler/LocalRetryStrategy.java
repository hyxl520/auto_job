package com.jingge.autojob.skeleton.model.handler;

import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.config.AutoJobConstant;
import com.jingge.autojob.skeleton.framework.task.AutoJobContext;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import lombok.extern.slf4j.Slf4j;
import sun.security.provider.SHA;

import java.util.concurrent.TimeUnit;

/**
 * 本机重试策略
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-07-24 17:20
 * @email 1158055613@qq.com
 */
@Slf4j
public class LocalRetryStrategy implements AutoJobRetryStrategy {
    @Override
    public boolean retry(AutoJobTask task) {
        task
                .getTrigger()
                .setTriggeringTime((long) (System.currentTimeMillis() + task
                        .getRetryConfig()
                        .getInterval() * 60 * 1000));
        //分片错误直接放入调度队列
        if (task.isSharding() && task.isEnableSharding() && task
                .getShardingConfig()
                .isEnableShardingRetry()) {
            //log.warn("任务{}的分片{}发生异常，将直接放入调度队列等待重试", task.getId(), task.getShardingId());
            return AutoJobApplication
                    .getInstance()
                    .getRegister()
                    .registerTask(task, true, 0, TimeUnit.SECONDS);
        }

        if (task
                .getTrigger()
                .isNearTriggeringTime(5000)) {
            AutoJobApplication
                    .getInstance()
                    .getRegister()
                    .registerTask(task, true, 0, TimeUnit.SECONDS);
        }

        if (task.getType() == AutoJobTask.TaskType.DB_TASK) {
            AutoJobMapperHolder.TRIGGER_ENTITY_MAPPER.updateTriggeringTime(task.getId(), task
                    .getTrigger()
                    .getTriggeringTime());
        } else if (task.getType() == AutoJobTask.TaskType.MEMORY_TASk) {
            AutoJobApplication
                    .getInstance()
                    .getMemoryTaskContainer()
                    .updateTriggeringTime(task.getId(), task
                            .getTrigger()
                            .getTriggeringTime());
            AutoJobApplication
                    .getInstance()
                    .getMemoryTaskContainer()
                    .getByIdDirect(task.getId())
                    .getTrigger()
                    .getCurrentRepeatTimes()
                    .incrementAndGet();
        }
        return true;
    }
}
