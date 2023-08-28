package com.jingge.autojob.skeleton.db.entity;

import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;
import com.jingge.autojob.skeleton.framework.task.AutoJobRunningStatus;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.model.task.script.ScriptTask;
import com.jingge.autojob.util.bean.ObjectUtil;
import com.jingge.autojob.util.convert.StringUtils;

import java.util.Arrays;

/**
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-03-15 16:33
 * @email 1158055613@qq.com
 */
public class ScriptEntity2TaskConvertor implements Entity2TaskConvertor<AutoJobScriptTaskEntity> {
    @Override
    public AutoJobTask convert(AutoJobScriptTaskEntity source) {
        if (ObjectUtil.isNull(source)) {
            return null;
        }
        AutoJobTriggerEntity triggerEntity = AutoJobMapperHolder.TRIGGER_ENTITY_MAPPER.selectOneByTaskId(source.getId());
        if (source.getType() == 1) {
            ScriptTask task = new ScriptTask();
            task.setId(source.getId());
            task.setTrigger(EntityConvertor.triggerEntity2Trigger(triggerEntity));
            task.setTaskLevel(source.getTaskLevel());
            task.setIsChildTask(source.getIsChildTask() != null && source.getIsChildTask() == 1);
            task.setBelongTo(source.getBelongTo());
            task.setIsShardingTask(source.getIsShardingTask() != null && source.getIsShardingTask() == 1);
            task.setVersionId(source.getVersionId());
            task.setType(AutoJobTask.TaskType.DB_TASK);
            task.setIsAllowRegister(true);
            task.setAlias(source.getAlias());
            task.setIsFinished(false);
            if (!StringUtils.isEmpty(source.getExecutableMachines())) {
                String[] machine = source
                        .getExecutableMachines()
                        .split(",");
                task.setExecutableMachines(Arrays.asList(machine));
            }
            if (source.getRunningStatus() != null) {
                task.setRunningStatus(AutoJobRunningStatus.findByFlag(source.getRunningStatus()));
                if (task.getTrigger() != null) {
                    task
                            .getTrigger()
                            .setIsRetrying(task.getRunningStatus() == AutoJobRunningStatus.RETRYING);
                }
            }
            task.setCmd(source.getScriptCmd());
            if (!StringUtils.isEmpty(source.getScriptFileName())) {
                task.setScriptFilename(source.getScriptFileName());
                task.setScriptFileSuffix(source
                        .getScriptFileName()
                        .substring(source
                                .getScriptFileName()
                                .lastIndexOf(".")));
                task.setScriptFile(true);
                task.setNeedWrite(false);
            } else {
                task.setScriptFile(false);
                task.setNeedWrite(false);
                task.setIsCmd(true);
            }
            task.setScriptContent(source.getScriptContent());
            task.setScriptPath(source.getScriptPath());
            return task;
        }
        return null;
    }
}
