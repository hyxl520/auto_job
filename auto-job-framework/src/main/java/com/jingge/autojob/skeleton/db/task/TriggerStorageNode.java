package com.jingge.autojob.skeleton.db.task;

import com.jingge.autojob.skeleton.db.StorageNode;
import com.jingge.autojob.skeleton.db.entity.AutoJobTriggerEntity;
import com.jingge.autojob.skeleton.db.entity.EntityConvertor;
import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 触发器保存节点
 *
 * @author Huang Yongxiang
 * @date 2023-01-05 11:02
 * @email 1158055613@qq.com
 */
public class TriggerStorageNode implements StorageNode<AutoJobTask> {
    @Override
    public int store(List<AutoJobTask> entities) {
        List<AutoJobTriggerEntity> triggerEntities = entities
                .stream()
                .map(task -> {
                    if (task.getTrigger() != null) {
                        AutoJobTriggerEntity entity = EntityConvertor.trigger2TriggerEntity(task.getTrigger());
                        task
                                .getTrigger()
                                .setTriggerId(entity.getId());
                        return entity;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return AutoJobMapperHolder.TRIGGER_ENTITY_MAPPER.insertList(triggerEntities);
    }
}
