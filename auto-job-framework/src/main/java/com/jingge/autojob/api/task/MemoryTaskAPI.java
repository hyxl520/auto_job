package com.jingge.autojob.api.task;

import com.jingge.autojob.api.task.params.MethodTaskEditParams;
import com.jingge.autojob.api.task.params.ScriptTaskEditParams;
import com.jingge.autojob.api.task.params.TaskEditParams;
import com.jingge.autojob.api.task.params.TriggerEditParams;
import com.jingge.autojob.skeleton.annotation.AutoJobRPCService;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.container.MemoryTaskContainer;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.framework.task.AutoJobTrigger;
import com.jingge.autojob.skeleton.model.builder.AutoJobTriggerFactory;
import com.jingge.autojob.skeleton.model.executor.IMethodObjectFactory;
import com.jingge.autojob.skeleton.model.interpreter.AutoJobAttributeContext;
import com.jingge.autojob.skeleton.model.task.method.MethodTask;
import com.jingge.autojob.skeleton.model.task.script.ScriptTask;
import com.jingge.autojob.util.bean.ObjectUtil;
import com.jingge.autojob.util.convert.DateUtils;
import com.jingge.autojob.util.convert.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Memory任务一站式API，该类能被框架内置RPC客户端调用
 *
 * @Author Huang Yongxiang
 * @Date 2022/10/17 15:40
 */
@AutoJobRPCService("MemoryTaskAPI")
@Slf4j
public class MemoryTaskAPI implements AutoJobAPI {
    private final MemoryTaskContainer container = AutoJobApplication
            .getInstance()
            .getMemoryTaskContainer();

    @Override
    public List<MethodTask> pageMethodTask(int pageNum, int size) {
        List<MethodTask> tasks = container
                .list()
                .stream()
                .filter(task -> task instanceof MethodTask)
                .map(task -> (MethodTask) task)
                .collect(Collectors.toList());
        ;
        if (tasks.size() == 0) {
            return Collections.emptyList();
        }
        int skip = (pageNum - 1) * size;
        int startIndex = Math.min(tasks.size(), skip);
        return new ArrayList<>(tasks.subList(startIndex, Math.min(tasks.size(), startIndex + size)));
    }

    @Override
    public List<ScriptTask> pageScriptTask(int pageNum, int size) {
        List<ScriptTask> tasks = container
                .list()
                .stream()
                .filter(task -> task instanceof ScriptTask)
                .map(task -> (ScriptTask) task)
                .collect(Collectors.toList());
        ;
        if (tasks.size() == 0) {
            return Collections.emptyList();
        }
        int skip = (pageNum - 1) * size;
        int startIndex = Math.min(tasks.size(), skip);
        return new ArrayList<>(tasks.subList(startIndex, Math.min(tasks.size(), startIndex + size)));
    }

    @Override
    public int count() {
        return container.size();
    }

    @Override
    public boolean registerTask(AutoJobTask task) {
        if (ObjectUtil.isNull(task)) {
            return false;
        }
        if (task.getTrigger() == null || task.getType() != AutoJobTask.TaskType.MEMORY_TASk) {
            return false;
        }
        try {
            container.insert(task);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean runTaskNow(AutoJobTask task) {
        if (ObjectUtil.isNull(task)) {
            return false;
        }
        if (task.getType() != AutoJobTask.TaskType.MEMORY_TASk) {
            return false;
        }
        AutoJobTrigger trigger = AutoJobTriggerFactory.newDelayTrigger(5, TimeUnit.SECONDS);
        task.setTrigger(trigger);
        try {
            container.insert(task);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public AutoJobTask find(long taskId) {
        return container.getById(taskId);
    }

    /**
     * 修改内存任务的触发器参数，注意该方法只允许修改cron-like表达式、重复次数、周期、触发时间、子任务、最大运行时长以及暂停调度
     *
     * @param taskId            要修改的任务
     * @param triggerEditParams 修改的信息
     * @return java.lang.boolean
     * @author Huang Yongxiang
     * @date 2022/10/27 16:50
     */
    @Override
    public boolean editTrigger(long taskId, TriggerEditParams triggerEditParams) {
        if (ObjectUtil.isNull(triggerEditParams)) {
            return false;
        }
        AutoJobTask task = container.getByIdDirect(taskId);
        if (task == null || task.getTrigger() == null) {
            return false;
        }
        if (pause(taskId)) {
            //log.warn("任务开始修改");
            try {
                AutoJobTrigger editParams = new AutoJobTrigger();
                editParams.setCronExpression(triggerEditParams.getCronExpression());
                editParams.setRepeatTimes(triggerEditParams.getRepeatTimes());
                editParams.setCycle(triggerEditParams.getCycle());
                if (!StringUtils.isEmpty(triggerEditParams.getChildTasksId())) {
                    String[] idArray = triggerEditParams
                            .getChildTasksId()
                            .split(",");
                    editParams.setChildTask(Arrays
                            .stream(idArray)
                            .map(Long::parseLong)
                            .collect(Collectors.toList()));
                }
                editParams.setMaximumExecutionTime(triggerEditParams.getMaximumExecutionTime());
                ObjectUtil.mergeObject(editParams, task.getTrigger());
            } finally {
                unpause(taskId);
            }
            //log.warn("任务修改完成");
            return true;
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
                AutoJobTask task = container.getByIdDirect(taskId);
                if (task == null) {
                    return false;
                }
                trigger.setTaskId(taskId);
                task.setTrigger(trigger);
                if (trigger.isNearTriggeringTime(5000)) {
                    AutoJobApplication
                            .getInstance()
                            .getRegister()
                            .registerTask(task);
                }
                log.info("任务{}将在{}执行", taskId, DateUtils.formatDateTime(new Date(trigger.getTriggeringTime())));
                return true;
            }
        } finally {
            unpause(taskId);
        }
        return false;
    }

    /**
     * 修改内存任务信息，任务ID，版本ID和任务类型不允许修改
     *
     * @param taskId         要修改的任务ID
     * @param taskEditParams 修改的内容
     * @return java.lang.boolean
     * @author Huang Yongxiang
     * @date 2022/10/27 17:52
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean editTask(long taskId, TaskEditParams taskEditParams) {
        AutoJobTask edit = container.getByIdDirect(taskId);
        if (edit == null || ObjectUtil.isNull(taskEditParams)) {
            return false;
        }
        if (!pause(taskId)) {
            return false;
        }
        try {
            if (taskEditParams instanceof MethodTaskEditParams) {
                MethodTask task = new MethodTask();
                task.setAlias(taskEditParams.getAlias());
                task.setBelongTo(taskEditParams.getBelongTo());
                task.setTaskLevel(taskEditParams.getTaskLevel());
                if (!StringUtils.isEmpty(((MethodTaskEditParams) taskEditParams).getParamsString())) {
                    task.setParamsString(((MethodTaskEditParams) taskEditParams).getParamsString());
                    task.setParams(new AutoJobAttributeContext(task.getParamsString()).getAttributeEntity());
                }

                if (!StringUtils.isEmpty(((MethodTaskEditParams) taskEditParams).getMethodObjectFactory())) {
                    Class<IMethodObjectFactory> factoryClass = (Class<IMethodObjectFactory>) ObjectUtil.classPath2Class(((MethodTaskEditParams) taskEditParams).getMethodObjectFactory());
                    if (factoryClass == null) {
                        return false;
                    }
                    task.setMethodObjectFactory(ObjectUtil.getClassInstance(factoryClass));
                }
                ObjectUtil.mergeObject(task, edit);
                return true;
            } else if (taskEditParams instanceof ScriptTaskEditParams) {
                ScriptTask task = new ScriptTask();
                task.setAlias(taskEditParams.getAlias());
                task.setBelongTo(taskEditParams.getBelongTo());
                task.setTaskLevel(taskEditParams.getTaskLevel());
                task.setParams(((ScriptTaskEditParams) taskEditParams)
                        .getAttributes()
                        .toArray());
                ObjectUtil.mergeObject(task, edit);
                return true;
            }
        } finally {
            unpause(taskId);
        }
        return false;
    }

    @Override
    public boolean pause(long taskId) {
        AutoJobTask task = container.getByIdDirect(taskId);
        if (task == null) {
            return false;
        }
        if (task.getTrigger() == null) {
            return true;
        }
        task
                .getTrigger()
                .setIsPause(true);
        AutoJobApplication
                .getInstance()
                .getRegister()
                .removeTask(taskId);
        return true;
    }

    @Override
    public boolean unpause(long taskId) {
        AutoJobTask task = container.getByIdDirect(taskId);
        if (task == null || task.getTrigger() == null) {
            return false;
        }
        task
                .getTrigger()
                .setIsPause(false);
        if (task
                .getTrigger()
                .getTriggeringTime() == null || task
                .getTrigger()
                .getTriggeringTime() < System.currentTimeMillis()) {
            task
                    .getTrigger()
                    .refresh();
        }
        return true;
    }

    @Override
    public boolean delete(long taskId) {
        return pause(taskId) && container.removeById(taskId) != null;
    }

    @Override
    public boolean isExist(long taskId) {
        return container.getById(taskId) != null;
    }
}
