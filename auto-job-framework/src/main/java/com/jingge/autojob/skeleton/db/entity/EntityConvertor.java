package com.jingge.autojob.skeleton.db.entity;

import com.jingge.autojob.logging.domain.AutoJobLog;
import com.jingge.autojob.logging.domain.AutoJobRunLog;
import com.jingge.autojob.logging.domain.AutoJobSchedulingRecord;
import com.jingge.autojob.skeleton.framework.config.ConfigDeserializer;
import com.jingge.autojob.skeleton.framework.config.StorableConfig;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.framework.task.AutoJobTrigger;
import com.jingge.autojob.skeleton.model.task.method.MethodTask;
import com.jingge.autojob.skeleton.model.task.script.ScriptTask;
import com.jingge.autojob.util.bean.ObjectUtil;
import com.jingge.autojob.util.convert.DateUtils;
import com.jingge.autojob.util.convert.DefaultValueUtil;
import com.jingge.autojob.util.convert.StringUtils;
import com.jingge.autojob.util.id.IdGenerator;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 实体对象转化
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/20 17:38
 */
public class EntityConvertor {
    private static final MethodTask2EntityConvertor METHOD_TASK_2_ENTITY_CONVERTOR = new MethodTask2EntityConvertor();
    private static final ScriptTask2EntityConvertor SCRIPT_TASK_2_ENTITY_CONVERTOR = new ScriptTask2EntityConvertor();
    private static final MethodEntity2TaskConvertor METHOD_ENTITY_2_TASK_CONVERTOR = new MethodEntity2TaskConvertor();
    private static final ScriptEntity2TaskConvertor SCRIPT_ENTITY_2_TASK_CONVERTOR = new ScriptEntity2TaskConvertor();

    public static AutoJobTaskBaseEntity task2Entity(AutoJobTask task) {
        if (task instanceof MethodTask) {
            return METHOD_TASK_2_ENTITY_CONVERTOR.convert(task);
        } else if (task instanceof ScriptTask) {
            return SCRIPT_TASK_2_ENTITY_CONVERTOR.convert(task);
        }
        return null;
    }

    public static AutoJobTask entity2Task(AutoJobTaskBaseEntity entity) {
        if (entity instanceof AutoJobMethodTaskEntity) {
            return METHOD_ENTITY_2_TASK_CONVERTOR.convert((AutoJobMethodTaskEntity) entity);
        } else if (entity instanceof AutoJobScriptTaskEntity) {
            return SCRIPT_ENTITY_2_TASK_CONVERTOR.convert((AutoJobScriptTaskEntity) entity);
        }
        return null;
    }


    public static AutoJobTrigger triggerEntity2Trigger(AutoJobTriggerEntity entity) {
        if (ObjectUtil.isNull(entity)) {
            return null;
        }
        AutoJobTrigger trigger = new AutoJobTrigger();
        if (!StringUtils.isEmpty(entity.getChildTasksId())) {
            trigger.setChildTask(Arrays
                    .stream(entity
                            .getChildTasksId()
                            .split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toList()));
        }
        trigger.setCurrentRepeatTimes(new AtomicInteger(entity.getCurrentRepeatTimes()));
        trigger.setTriggerId(entity.getId());
        trigger.setIsPause(DefaultValueUtil.defaultValue(entity.getIsPause(), 0) == 1);
        trigger.setIsRunning(DefaultValueUtil.defaultValue(entity.getIsRun(), 0) == 1);
        trigger.setMaximumExecutionTime(entity.getMaximumExecutionTime());
        trigger.setLastTriggeringTime(entity.getLastTriggeringTime());
        trigger.setTriggeringTime(entity.getNextTriggeringTime());
        trigger.setCronExpression(entity.getCronExpression());
        trigger.setIsLastSuccess(entity.getIsLastSuccess() == 1);
        trigger.setCycle(entity.getCycle());
        trigger.setTaskId(entity.getTaskId());
        trigger.setLastRunTime(entity.getLastRunTime());
        trigger.setRepeatTimes(entity.getRepeatTimes());
        trigger.setFinishedTimes(entity.getFinishedTimes());
        return trigger;
    }

    public static AutoJobTriggerEntity trigger2TriggerEntity(AutoJobTrigger trigger) {
        AutoJobTriggerEntity entity = new AutoJobTriggerEntity();
        entity.setId(trigger.getTriggerId() == null ? IdGenerator.getNextIdAsLong() : trigger.getTriggerId());
        if (trigger.hasChildTask()) {
            StringBuilder children = new StringBuilder();
            trigger
                    .getChildTask()
                    .forEach(id -> {
                        children
                                .append(id)
                                .append(",");
                    });
            children.deleteCharAt(children.length() - 1);
            entity.setChildTasksId(children.toString());
        }
        entity.setCurrentRepeatTimes(trigger
                .getCurrentRepeatTimes()
                .get());
        entity.setIsRun(DefaultValueUtil.defaultValue(trigger.getIsRunning(), false) ? 1 : 0);
        entity.setMaximumExecutionTime(trigger.getMaximumExecutionTime());
        entity.setCreateTime(new Timestamp(System.currentTimeMillis()));
        entity.setCronExpression(trigger.getCronExpression());
        entity.setCycle(trigger.getCycle());
        entity.setIsPause(trigger.getIsPause() != null && trigger.getIsPause() ? 1 : 0);
        entity.setLastRunTime(trigger.getLastRunTime());
        entity.setFinishedTimes(trigger.getFinishedTimes());
        entity.setIsLastSuccess(trigger.getIsLastSuccess() != null && trigger.getIsLastSuccess() ? 1 : 0);
        entity.setNextTriggeringTime(trigger.getTriggeringTime());
        entity.setLastTriggeringTime(trigger.getLastTriggeringTime());
        entity.setRepeatTimes(trigger.getRepeatTimes());
        entity.setTaskId(trigger.getTaskId());
        return entity;
    }

    public static AutoJobLog logEntity2Log(AutoJobLogEntity entity) {
        if (ObjectUtil.isNull(entity)) {
            return null;
        }
        AutoJobLog log = new AutoJobLog();
        log.setId(entity.getId() == null ? -1L : entity.getId());
        log.setMessage(entity.getMessage());
        log.setLevel(entity.getLogLevel());
        log.setSchedulingId(entity.getSchedulingId());
        log.setInputTime(DateUtils.formatDateTime(new Date(entity
                .getWriteTime()
                .getTime())));
        log.setTaskId(entity.getTaskId());
        return log;
    }

    public static AutoJobLogEntity log2LogEntity(AutoJobLog log) {
        AutoJobLogEntity entity = new AutoJobLogEntity();
        entity.setId(IdGenerator.getNextIdAsLong());
        entity.setLogLevel(log.getLevel());
        entity.setTaskId(log.getTaskId());
        entity.setSchedulingId(log.getSchedulingId());
        entity.setMessage(log.getMessage());
        Timestamp timestamp = null;
        if (StringUtils.isEmpty(log.getInputTime())) {
            timestamp = new Timestamp(System.currentTimeMillis());
        } else {
            try {
                timestamp = new Timestamp(DateUtils
                        .parseDate(log.getInputTime(), "yyyy-MM-dd HH:mm:ss")
                        .getTime());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        entity.setWriteTime(timestamp);
        entity.setWriteTimestamp(timestamp == null ? System.currentTimeMillis() : timestamp.getTime());
        return entity;
    }

    public static AutoJobRunLog runLogEntity2RunLog(AutoJobRunLogEntity entity) {
        if (ObjectUtil.isNull(entity)) {
            return null;
        }
        AutoJobRunLog runLog = new AutoJobRunLog();
        runLog.setId(entity.getId());
        runLog.setErrorStack(entity.getErrorStack());
        runLog.setMessage(entity.getMessage());
        runLog.setSchedulingId(entity.getSchedulingId());
        runLog.setRunStatus(entity.getRunStatus());
        runLog.setTaskId(entity.getTaskId());
        runLog.setTaskType(entity.getTaskType());
        runLog.setWriteTime(DateUtils.formatDateTime(new Date(entity
                .getWriteTime()
                .getTime())));
        return runLog;
    }

    public static AutoJobRunLogEntity runLog2RunLogEntity(AutoJobRunLog runLog) {
        AutoJobRunLogEntity entity = new AutoJobRunLogEntity();
        entity.setId(IdGenerator.getNextIdAsLong());
        entity.setErrorStack(runLog.getErrorStack());
        entity.setTaskId(runLog.getTaskId());
        entity.setSchedulingId(runLog.getSchedulingId());
        entity.setTaskType(runLog.getTaskType());
        entity.setMessage(runLog.getMessage());
        entity.setResult(runLog.getRunResult());
        Timestamp timestamp = null;
        if (StringUtils.isEmpty(runLog.getWriteTime())) {
            timestamp = new Timestamp(System.currentTimeMillis());
        } else {
            try {
                timestamp = new Timestamp(DateUtils
                        .parseDate(runLog.getWriteTime(), "yyyy-MM-dd HH:mm:ss")
                        .getTime());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        entity.setWriteTime(timestamp);
        entity.setWriteTimestamp(timestamp == null ? System.currentTimeMillis() : timestamp.getTime());
        entity.setRunStatus(runLog.getRunStatus());
        return entity;
    }

    public static AutoJobSchedulingRecordEntity schedulingRecord2Entity(AutoJobSchedulingRecord schedulingRecord) {
        AutoJobSchedulingRecordEntity entity = new AutoJobSchedulingRecordEntity();
        entity.setId(schedulingRecord.getSchedulingId());
        entity.setExecutionTime(schedulingRecord.getExecutionTime());
        entity.setSchedulingTime(new Timestamp(schedulingRecord
                .getSchedulingTime()
                .getTime()));
        entity.setWriteTimestamp(schedulingRecord
                .getSchedulingTime()
                .getTime());
        entity.setTaskAlias(schedulingRecord.getTaskAlias());
        entity.setSchedulingType(schedulingRecord.getSchedulingType());
        entity.setShardingId(schedulingRecord.getShardingId());
        if (!schedulingRecord.isRun()) {
            entity.setIsRun(0);
            entity.setIsSuccess(schedulingRecord.isSuccess() ? 1 : 0);
            entity.setResult(schedulingRecord.getResult());
        } else {
            entity.setIsRun(1);
        }
        entity.setTaskId(schedulingRecord.getTaskId());
        return entity;
    }

    public static AutoJobSchedulingRecord entity2schedulingRecord(AutoJobSchedulingRecordEntity entity) {
        AutoJobSchedulingRecord record = new AutoJobSchedulingRecord();
        record.setSchedulingId(entity.getId());
        record.setExecutionTime(entity.getExecutionTime());
        record.setSchedulingId(entity.getShardingId());
        record.setSuccess(entity.getIsSuccess() != null && entity.getIsSuccess() == 1);
        record.setTaskAlias(entity.getTaskAlias());
        record.setSchedulingType(entity.getSchedulingType());
        record.setResult(entity.getResult());
        record.setTaskId(entity.getTaskId());
        record.setSchedulingTime(new Date(entity
                .getSchedulingTime()
                .getTime()));
        return record;
    }

    public static AutoJobConfigEntity storableConfig2Entity(StorableConfig config) {
        if (config == null) {
            return null;
        }
        try {
            config.beforeSerialize();
            return config
                    .getSerializer()
                    .serialize(config);
        } finally {
            config.afterSerialize();
        }
    }

    public static StorableConfig entity2StorableConfig(AutoJobConfigEntity entity, ConfigDeserializer deserializer) {
        if (entity == null) {
            return null;
        }
        return deserializer.deserialize(entity);
    }
}
