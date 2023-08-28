package com.jingge.autojob.api.task;

import com.jingge.autojob.api.task.params.TaskEditParams;
import com.jingge.autojob.api.task.params.TriggerEditParams;
import com.jingge.autojob.skeleton.annotation.AutoJobRPCService;
import com.jingge.autojob.skeleton.db.AutoJobSQLException;
import com.jingge.autojob.skeleton.db.entity.AutoJobTriggerEntity;
import com.jingge.autojob.skeleton.db.entity.EntityConvertor;
import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;
import com.jingge.autojob.skeleton.db.mapper.TransactionEntry;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.config.AutoJobConstant;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.framework.task.AutoJobTrigger;
import com.jingge.autojob.skeleton.model.builder.AutoJobTriggerFactory;
import com.jingge.autojob.skeleton.model.register.IAutoJobRegister;
import com.jingge.autojob.skeleton.model.task.method.MethodTask;
import com.jingge.autojob.skeleton.model.task.script.ScriptTask;
import com.jingge.autojob.util.bean.ObjectUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 提供DB任务的一站式API
 *
 * @Author Huang Yongxiang
 * @Date 2022/10/17 13:51
 */
@AutoJobRPCService("DBTaskAPI")
@Slf4j
public class DBTaskAPI implements AutoJobAPI {
    private final IAutoJobRegister register = AutoJobApplication
            .getInstance()
            .getRegister();

    @Override
    public List<MethodTask> pageMethodTask(int pageNum, int size) {
        return AutoJobMapperHolder.METHOD_TASK_ENTITY_MAPPER
                .page(pageNum, size)
                .stream()
                .map(task -> (MethodTask) EntityConvertor.entity2Task(task))
                .collect(Collectors.toList());
    }

    @Override
    public List<ScriptTask> pageScriptTask(int pageNum, int size) {
        return AutoJobMapperHolder.SCRIPT_TASK_ENTITY_MAPPER
                .page(pageNum, size)
                .stream()
                .map(task -> (ScriptTask) EntityConvertor.entity2Task(task))
                .collect(Collectors.toList());
    }

    @Override
    public int count() {
        return AutoJobMapperHolder.METHOD_TASK_ENTITY_MAPPER.count() + AutoJobMapperHolder.SCRIPT_TASK_ENTITY_MAPPER.count();
    }

    @Override
    public boolean registerTask(AutoJobTask task) {
        if (ObjectUtil.isNull(task)) {
            return false;
        }
        if (task.getTrigger() == null || task.getType() != AutoJobTask.TaskType.DB_TASK) {
            return false;
        }
        if (task
                .getTrigger()
                .isNearTriggeringTime(2 * AutoJobConstant.dbSchedulerRate)) {
            register.registerTask(task);
        }
        return AutoJobMapperHolder
                .getMatchTaskMapper(task.getId())
                .saveTasks(Collections.singletonList(task));
    }


    @Override
    public boolean runTaskNow(AutoJobTask task) {
        if (ObjectUtil.isNull(task)) {
            return false;
        }
        if (task.getType() != AutoJobTask.TaskType.DB_TASK) {
            return false;
        }
        AutoJobTrigger trigger = AutoJobTriggerFactory.newDelayTrigger(5, TimeUnit.SECONDS);
        task.setTrigger(trigger);
        registerTask(task);
        trigger.refresh();
        return register.registerTask(task);
    }

    @Override
    public AutoJobTask find(long taskId) {
        return EntityConvertor.entity2Task(AutoJobMapperHolder
                .getMatchTaskMapper(taskId)
                .selectById(taskId));
    }

    @Override
    public boolean editTrigger(long taskId, TriggerEditParams triggerEditParams) {
        if (ObjectUtil.isNull(triggerEditParams)) {
            return false;
        }
        if (pause(taskId)) {
            boolean flag = false;
            try {
                flag = AutoJobMapperHolder.TRIGGER_ENTITY_MAPPER.updateByTaskId(triggerEditParams, taskId) >= 0;
            } finally {
                unpause(taskId);
            }
            return flag;
        }
        return false;
    }

    @Override
    public boolean bindingTrigger(long taskId, AutoJobTrigger trigger) {
        if (trigger == null) {
            return false;
        }
        try {
            if (pause(taskId)) {
                trigger.setTaskId(taskId);
                AutoJobTriggerEntity entity = EntityConvertor.trigger2TriggerEntity(trigger);
                TransactionEntry insertTrigger = connection -> AutoJobMapperHolder.TRIGGER_ENTITY_MAPPER.insertList(Collections.singletonList(entity));
                TransactionEntry bindingTrigger = connection -> {
                    boolean flag = AutoJobMapperHolder
                            .getMatchTaskMapper(taskId)
                            .bindingTrigger(entity.getId(), taskId);
                    //绑定失败回滚
                    if (!flag) {
                        throw new AutoJobSQLException();

                    }
                    return 1;
                };
                return AutoJobMapperHolder.METHOD_TASK_ENTITY_MAPPER.doTransaction(new TransactionEntry[]{insertTrigger, bindingTrigger});
            }
        } finally {
            unpause(taskId);
        }
        return false;
    }

    /**
     * 修改DB任务信息，任务ID，版本ID和任务类型不允许修改
     *
     * @param taskId         任务ID
     * @param taskEditParams 要修改的内容
     * @return java.lang.boolean
     * @author Huang Yongxiang
     * @date 2022/10/27 17:52
     */
    @Override
    public boolean editTask(long taskId, TaskEditParams taskEditParams) {
        if (pause(taskId) && !ObjectUtil.isNull(taskEditParams)) {
            boolean flag = false;
            try {
                flag = AutoJobMapperHolder
                        .getMatchTaskMapper(taskId)
                        .updateById(taskEditParams, taskId) >= 0;
            } finally {
                unpause(taskId);
            }
            return flag;
        }
        return false;
    }

    @Override
    public boolean pause(long taskId) {
        register.removeTask(taskId);
        return AutoJobMapperHolder.TRIGGER_ENTITY_MAPPER.pauseTaskById(taskId);
    }

    @Override
    public boolean unpause(long taskId) {
        AutoJobTask task = EntityConvertor.entity2Task(AutoJobMapperHolder
                .getMatchTaskMapper(taskId)
                .selectById(taskId));
        if (task == null || task.getTrigger() == null) {
            return false;
        }
        task
                .getTrigger()
                .setIsPause(false);
        TransactionEntry updateTriggeringTime = connection -> {
            if (task
                    .getTrigger()
                    .getTriggeringTime() != null && task
                    .getTrigger()
                    .getTriggeringTime() < System.currentTimeMillis() && task
                    .getTrigger()
                    .refresh()) {
                AutoJobMapperHolder.TRIGGER_ENTITY_MAPPER.updateTriggeringTime(taskId, task
                        .getTrigger()
                        .getTriggeringTime());
            }
            return 1;
        };
        TransactionEntry unpause = connection -> {
            AutoJobMapperHolder.TRIGGER_ENTITY_MAPPER.unpauseTaskById(taskId);
            return 1;
        };
        return AutoJobMapperHolder.TRIGGER_ENTITY_MAPPER.doTransaction(new TransactionEntry[]{updateTriggeringTime, unpause});
    }

    @Override
    public boolean delete(long taskId) {
        TransactionEntry deleteTask = (connection) -> AutoJobMapperHolder
                .getMatchTaskMapper(taskId)
                .deleteById(taskId) ? 1 : 0;
        TransactionEntry deleteTrigger = connection -> AutoJobMapperHolder.TRIGGER_ENTITY_MAPPER.deleteByTaskIds(Collections.singletonList(taskId));
        return AutoJobMapperHolder.TRIGGER_ENTITY_MAPPER.doTransaction(new TransactionEntry[]{deleteTask, deleteTrigger});
    }

    @Override
    public boolean isExist(long taskId) {
        return !ObjectUtil.isNull(find(taskId));
    }
}
