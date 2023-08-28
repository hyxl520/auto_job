package com.jingge.autojob.skeleton.model.handler;

import com.jingge.autojob.skeleton.db.entity.EntityConvertor;
import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;
import com.jingge.autojob.skeleton.framework.config.AutoJobRetryConfig;
import com.jingge.autojob.skeleton.framework.config.ConfigJsonSerializerAndDeserializer;
import com.jingge.autojob.skeleton.framework.config.RetryStrategy;
import com.jingge.autojob.skeleton.framework.task.AutoJobRunningStatus;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lifecycle.TaskEventFactory;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskErrorEvent;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskFinishedEvent;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskRetryEvent;
import com.jingge.autojob.skeleton.lifecycle.manager.TaskEventManager;
import com.jingge.autojob.util.convert.DefaultValueUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 任务重试管理器
 *
 * @author Huang Yongxiang
 * @date 2022-12-30 14:05
 * @email 1158055613@qq.com
 */
@Slf4j
public class AutoJobRetryHandler {
    //private final Map<Long, AtomicInteger> retryMap = new ConcurrentHashMap<>();

    private AutoJobRetryHandler() {
    }

    public static AutoJobRetryHandler getInstance() {
        return InstanceHolder.HANDLER;
    }

    public boolean retry(AutoJobTask task) {
        //尝试查询重试配置，查询配置懒加载可以减轻调度负担
        if (task.getType() == AutoJobTask.TaskType.DB_TASK && task.getRetryConfig() == null) {
            task.setRetryConfig((AutoJobRetryConfig) EntityConvertor.entity2StorableConfig(AutoJobMapperHolder.CONFIG_ENTITY_MAPPER.selectByTaskIdAndType(AutoJobRetryConfig.class.getName(), task.getId()), new ConfigJsonSerializerAndDeserializer()));
        }
        AutoJobRetryConfig config = task.getRetryConfig();
        if (config == null) {
            throw new NullPointerException("任务重试配置为空");
        }
        if (!task.isAllowRetry()) {
            return false;
        }
        try {
            task.updateRunningStatus(AutoJobRunningStatus.RETRYING);
            task
                    .getTrigger()
                    .setIsRetrying(true);
            if (DefaultValueUtil
                    .defaultValue(task
                            .getRetryConfig()
                            .getRetryStrategy(), RetryStrategy.LOCAL_RETRY)
                    .getRetryStrategy()
                    .retry(task)) {
                log.info("任务{}已提交重试", task.getId());
                TaskEventManager
                        .getInstance()
                        .publishTaskEventSync(TaskEventFactory.newTaskRetryEvent(task, task
                                .getTrigger()
                                .getCurrentRepeatTimes()
                                .get(), config.getNextRetryTime(), config.getRetryCount()), TaskRetryEvent.class, true);
            } else {
                log.warn("任务{}重试异常，已强制结束", task.getId());
                TaskEventManager
                        .getInstance()
                        .publishTaskEvent(TaskEventFactory.newErrorEvent(task), TaskErrorEvent.class, true);
                TaskEventManager
                        .getInstance()
                        .publishTaskEvent(TaskEventFactory.newFinishedEvent(task), TaskFinishedEvent.class, true);
                return false;
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static class InstanceHolder {
        private static final AutoJobRetryHandler HANDLER = new AutoJobRetryHandler();
    }
}
