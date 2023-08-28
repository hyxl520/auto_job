package com.jingge.autojob.skeleton.db.entity;

import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.model.task.script.ScriptTask;
import com.jingge.autojob.util.convert.StringUtils;

import java.sql.Timestamp;

/**
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-03-15 16:05
 * @email 1158055613@qq.com
 */
public class ScriptTask2EntityConvertor implements Task2EntityConvertor<AutoJobScriptTaskEntity> {
    @Override
    public AutoJobScriptTaskEntity convert(AutoJobTask source) {
        AutoJobScriptTaskEntity entity = new AutoJobScriptTaskEntity();
        entity.setId(source.getId());
        entity.setType(1);
        ScriptTask scriptTask = (ScriptTask) source;
        if (!StringUtils.isEmpty(scriptTask.getScriptFilename())) {
            if (scriptTask
                    .getScriptFilename()
                    .lastIndexOf(".") > 0) {
                entity.setScriptFileName(scriptTask.getScriptFilename());
            } else {
                entity.setScriptFileName(scriptTask
                        .getScriptFilename()
                        .concat(scriptTask.getScriptFileSuffix()));
            }
        }
        if (source.getExecutableMachines() != null && source
                .getExecutableMachines()
                .size() > 0) {
            entity.setExecutableMachines(String.join(",", source.getExecutableMachines()));
        }
        entity.setScriptCmd(scriptTask.getCmd());
        entity.setScriptContent(scriptTask.getScriptContent());
        entity.setScriptPath(scriptTask.getScriptPath());
        if (source.getVersionId() != null) {
            AutoJobScriptTaskEntity latestVersion = AutoJobMapperHolder.SCRIPT_TASK_ENTITY_MAPPER.selectLatestAnnotationTask(source.getVersionId());
            if (latestVersion != null && latestVersion.getVersion() != null) {
                entity.setVersion(latestVersion.getVersion() + 1);
            } else {
                entity.setVersion(0L);
            }
            entity.setVersionId(source.getVersionId());
        }
        entity.setBelongTo(source.getBelongTo());
        entity.setTriggerId(source
                .getTrigger()
                .getTriggerId());
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
