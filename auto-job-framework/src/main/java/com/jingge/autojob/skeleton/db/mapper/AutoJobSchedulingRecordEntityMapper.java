package com.jingge.autojob.skeleton.db.mapper;

import com.jingge.autojob.skeleton.db.entity.AutoJobSchedulingRecordEntity;
import com.jingge.autojob.skeleton.framework.config.DBTableConstant;

import java.util.Date;
import java.util.List;

/**
 * 调度记录mapper
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/29 16:27
 */
public class AutoJobSchedulingRecordEntityMapper extends BaseMapper<AutoJobSchedulingRecordEntity> {
    private static final String ALL_COLUMNS = "id, write_timestamp, scheduling_time, task_alias, task_id, is_success," + " is_run, scheduling_type, sharding_id, executing_machine, result, execution_time, del_flag";

    public AutoJobSchedulingRecordEntityMapper() {
        super(AutoJobSchedulingRecordEntity.class);
    }

    public int updateResult(long id, boolean isSuccess, String result, long executionTime, String executingMachine) {
        String sql = getUpdateExpression() + "set is_success = ?, result = ?,execution_time = ?, is_run = 0, executing_machine = ? where del_flag = 0 and id = ?";
        return updateOne(sql, isSuccess ? 1 : 0, result, executionTime, executingMachine, id);
    }

    public List<AutoJobSchedulingRecordEntity> pageByTaskId(int pageNum, int size, long taskId) {
        int skip = (pageNum - 1) * size;
        String sql = getSelectExpression() + String.format(" where task_id = ? and del_flag = 0 limit %d, %d", skip, size);
        return queryList(sql, taskId);
    }

    public int countByTaskId(long taskId) {
        String sql = " select count(*) from " + getTableName() + " where task_id = ? and del_flag = 0";
        return conditionalCount(sql, taskId);
    }

    public List<AutoJobSchedulingRecordEntity> listBetween(long taskId, Date from, Date to) {
        String sql = getSelectExpression() + " where task_id = ? AND del_flag = 0 AND write_timestamp >= ? AND write_timestamp <= ?";
        return queryList(sql, taskId, from.getTime(), to.getTime());
    }

    @Override
    public String getAllColumns() {
        return ALL_COLUMNS;
    }

    @Override
    public String getTableName() {
        return DBTableConstant.SCHEDULING_RECORD_TABLE_NAME;
    }
}
