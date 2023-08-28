package com.jingge.autojob.skeleton.db.entity;

import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.model.task.method.MethodTask;

import java.sql.Timestamp;

/**
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-03-15 16:16
 * @email 1158055613@qq.com
 */
public class MethodTask2EntityConvertor implements Task2EntityConvertor<AutoJobMethodTaskEntity> {
    @Override
    public AutoJobMethodTaskEntity convert(AutoJobTask source) {
        AutoJobMethodTaskEntity entity = new AutoJobMethodTaskEntity();
        entity.setId(source.getId());
        if (source instanceof MethodTask) {
            entity.setMethodClassName(((MethodTask) source).getMethodClass() == null ? "" : ((MethodTask) source)
                    .getMethodClass()
                    .getName());
            entity.setMethodName(((MethodTask) source).getMethodName());
            entity.setMethodObjectFactory(((MethodTask) source)
                    .getMethodObjectFactory()
                    .getClass()
                    .getName());
        }
        entity.setType(0);
        if (source.getExecutableMachines() != null && source
                .getExecutableMachines()
                .size() > 0) {
            entity.setExecutableMachines(String.join(",", source.getExecutableMachines()));
        }
        if (source.getVersionId() != null) {
            AutoJobMethodTaskEntity latestVersion = AutoJobMapperHolder.METHOD_TASK_ENTITY_MAPPER.selectLatestAnnotationTask(source.getVersionId());
            if (latestVersion != null && latestVersion.getVersion() != null) {
                entity.setVersion(latestVersion.getVersion() + 1);
            } else {
                entity.setVersion(0L);
            }
            entity.setVersionId(source.getVersionId());
        }
        entity.setBelongTo(source.getBelongTo());
        if (source.getTrigger() != null) {
            entity.setTriggerId(source
                    .getTrigger()
                    .getTriggerId());
        }
        entity.setRunningStatus(source
                .getRunningStatus()
                .getFlag());
        entity.setRunLock(0);
        entity.setStatus(1);
        entity.setCreateTime(new Timestamp(System.currentTimeMillis()));
        entity.setAlias(source.getAlias());
        entity.setIsChildTask(source.getIsChildTask() != null && source.getIsChildTask() ? 1 : 0);
        entity.setIsShardingTask(source.getIsShardingTask() != null && source.getIsShardingTask() ? 1 : 0);
        entity.setTaskLevel(source.getTaskLevel());
        entity.setParams(source.getParamsString());
        return entity;
    }
}
