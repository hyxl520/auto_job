package com.jingge.autojob.skeleton.db.mapper;

import com.jingge.autojob.skeleton.db.entity.AutoJobTaskBaseEntity;
import com.jingge.autojob.skeleton.enumerate.TaskOperationType;
import com.jingge.autojob.skeleton.framework.config.DBTableConstant;
import lombok.Getter;
import lombok.Setter;

/**
 * Mapper持有者
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/19 15:27
 */
public class AutoJobMapperHolder {

    public static final AutoJobMethodTaskEntityMapper METHOD_TASK_ENTITY_MAPPER = new AutoJobMethodTaskEntityMapper();

    public static final AutoJobScriptTaskEntityMapper SCRIPT_TASK_ENTITY_MAPPER = new AutoJobScriptTaskEntityMapper();

    public static final AutoJobRunLogEntityMapper RUN_LOG_ENTITY_MAPPER = new AutoJobRunLogEntityMapper();

    public static final AutoJobLogEntityMapper LOG_ENTITY_MAPPER = new AutoJobLogEntityMapper();

    public static final AutoJobTriggerEntityMapper TRIGGER_ENTITY_MAPPER = new AutoJobTriggerEntityMapper();

    public static final AutoJobSchedulingRecordEntityMapper SCHEDULING_RECORD_ENTITY_MAPPER = new AutoJobSchedulingRecordEntityMapper();

    public static final AutoJobConfigEntityMapper CONFIG_ENTITY_MAPPER = new AutoJobConfigEntityMapper();

    static final TaskTypeMatcherMapper TASK_TYPE_MATCHER_MAPPER = new TaskTypeMatcherMapper();

    public static AutoJobTaskEntityBaseMapper<? extends AutoJobTaskBaseEntity> getMatchTaskMapper(long taskID) {
        TaskOperationType taskType = TASK_TYPE_MATCHER_MAPPER.selectTaskTypeByTaskId(taskID);
        if (taskType == null) {
            return METHOD_TASK_ENTITY_MAPPER;
        }
        switch (taskType) {
            case SCRIPT_TASK:
                return SCRIPT_TASK_ENTITY_MAPPER;
            default:
                return METHOD_TASK_ENTITY_MAPPER;
        }
    }

    @Getter
    @Setter
    public static class TaskTypeMatcher {
        Long id;

        Long taskID;

        Integer taskType;

        Integer status;

        Integer delFlag;
    }

    static class TaskTypeMatcherMapper extends BaseMapper<TaskTypeMatcher> {

        public TaskTypeMatcherMapper() {
            super(TaskTypeMatcher.class);
        }

        TaskOperationType selectTaskTypeByTaskId(long taskId) {
            String sql = getSelectExpression() + " where task_id = ? and status = 1 and del_flag = 0";
            TaskTypeMatcher matcher = queryOne(sql, taskId);
            if (matcher == null || matcher.getTaskType() == null) {
                return null;
            }
            switch (matcher.taskType) {
                case 0:
                    return TaskOperationType.METHOD_TASK;
                case 1:
                    return TaskOperationType.SCRIPT_TASK;
                default:
                    return null;
            }
        }

        @Override
        public String getAllColumns() {
            return "id, task_id, task_type, status, del_flag";
        }

        @Override
        public String getTableName() {
            return DBTableConstant.JOB_TYPE_TABLE_MATCHER;
        }
    }
}
