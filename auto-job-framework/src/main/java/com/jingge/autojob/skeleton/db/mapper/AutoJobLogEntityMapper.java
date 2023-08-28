package com.jingge.autojob.skeleton.db.mapper;

import com.jingge.autojob.skeleton.db.entity.AutoJobLogEntity;
import com.jingge.autojob.skeleton.framework.config.DBTableConstant;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 任务日志持久层对象mapper
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/17 16:47
 */
public class AutoJobLogEntityMapper extends BaseMapper<AutoJobLogEntity> {
    /**
     * 所有列
     */
    public static final String ALL_COLUMNS = "id, scheduling_id, task_id, write_timestamp,write_time, log_level, message, del_flag";


    public AutoJobLogEntityMapper() {
        super(AutoJobLogEntity.class);
    }

    public List<AutoJobLogEntity> selectLogByTaskId(long taskId) {
        String condition = " where del_flag = 0 and task_id = ?";
        return queryList(getSelectExpression() + condition, taskId);
    }

    public int deleteByTaskId(long taskId) {
        String condition = " where del_flag = 0";
        return updateOne(getLogicDeleteExpression() + condition);
    }

    public List<AutoJobLogEntity> selectByTaskIdBetween(Date startTime, Date endTime, long taskId) {
        String condition = " where task_id = ? AND del_flag = 0 AND write_timestamp >= ? AND write_timestamp <= ?";
        return queryList(getSelectExpression() + condition, taskId, startTime.getTime(), endTime.getTime());
    }

    public List<AutoJobLogEntity> selectBySchedulingId(Long schedulingId) {
        if (schedulingId == null) {
            return Collections.emptyList();
        }
        String condition = " where scheduling_id = ? and del_flag = 0";
        return queryList(getSelectExpression() + condition, schedulingId);
    }


    @Override
    public String getAllColumns() {
        return ALL_COLUMNS;
    }

    @Override
    public String getTableName() {
        return DBTableConstant.JOB_LOG_TABLE_NAME;
    }
}
