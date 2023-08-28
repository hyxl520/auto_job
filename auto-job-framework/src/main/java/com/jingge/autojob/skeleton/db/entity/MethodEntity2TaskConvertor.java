package com.jingge.autojob.skeleton.db.entity;

import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;
import com.jingge.autojob.skeleton.framework.task.AutoJobRunningStatus;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.model.executor.IMethodObjectFactory;
import com.jingge.autojob.skeleton.model.interpreter.AutoJobAttributeContext;
import com.jingge.autojob.skeleton.model.task.method.MethodTask;
import com.jingge.autojob.util.bean.ObjectUtil;
import com.jingge.autojob.util.convert.StringUtils;

import java.util.Arrays;

/**
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-03-15 16:27
 * @email 1158055613@qq.com
 */
public class MethodEntity2TaskConvertor implements Entity2TaskConvertor<AutoJobMethodTaskEntity> {
    @Override
    public AutoJobTask convert(AutoJobMethodTaskEntity source) {
        if (ObjectUtil.isNull(source)) {
            return null;
        }
        AutoJobTriggerEntity triggerEntity = AutoJobMapperHolder.TRIGGER_ENTITY_MAPPER.selectOneByTaskId(source.getId());
        if (source.getType() == 0) {
            MethodTask task = new MethodTask();
            task.setId(source.getId());
            task.setTrigger(EntityConvertor.triggerEntity2Trigger(triggerEntity));
            task.setTaskLevel(source.getTaskLevel());
            task.setType(AutoJobTask.TaskType.DB_TASK);
            task.setIsShardingTask(source.getIsShardingTask() != null && source.getIsShardingTask() == 1);
            task.setMethodClassName(source.getMethodClassName());
            task.setMethodClass(ObjectUtil.classPath2Class(source.getMethodClassName()));
            task.setMethodName(source.getMethodName());
            if (!StringUtils.isEmpty(source.getExecutableMachines())) {
                String[] machine = source
                        .getExecutableMachines()
                        .split(",");
                task.setExecutableMachines(Arrays.asList(machine));
            }
            if (!StringUtils.isEmpty(source.getMethodObjectFactory())) {
                task.setMethodObjectFactory((IMethodObjectFactory) ObjectUtil.getClassInstance(ObjectUtil.classPath2Class(source.getMethodObjectFactory())));
            }
            task.setParamsString(source.getParams());
            if (!StringUtils.isEmpty(source.getParams())) {
                task.setParams(new AutoJobAttributeContext(task).getAttributeEntity());
            }
            if (source.getRunningStatus() != null) {
                task.setRunningStatus(AutoJobRunningStatus.findByFlag(source.getRunningStatus()));
                if (task.getTrigger() != null) {
                    task
                            .getTrigger()
                            .setIsRetrying(task.getRunningStatus() == AutoJobRunningStatus.RETRYING);
                }
            }
            task.setBelongTo(source.getBelongTo());
            task.setVersionId(source.getVersionId());
            task.setIsChildTask(source.getIsChildTask() != null && source.getIsChildTask() == 1);
            task.setAlias(source.getAlias());
            task.setIsAllowRegister(true);
            task.setIsFinished(false);
            return task;
        }
        return null;
    }
}
