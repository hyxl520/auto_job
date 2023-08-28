package com.jingge.autojob.skeleton.db.mapper;

import com.jingge.autojob.api.task.params.TriggerEditParams;
import com.jingge.autojob.skeleton.db.entity.AutoJobTriggerEntity;
import com.jingge.autojob.skeleton.framework.config.DBTableConstant;
import com.jingge.autojob.util.bean.ObjectUtil;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 任务触发器持久层对象mapper
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/17 16:48
 */
public class AutoJobTriggerEntityMapper extends BaseMapper<AutoJobTriggerEntity> {
    /**
     * 所有列
     */
    public static final String ALL_COLUMNS = "id,cron_expression, last_run_time, last_triggering_time, " + "next_triggering_time, is_last_success, repeat_times,finished_times, current_repeat_times, cycle,task_id," + " child_tasks_id, maximum_execution_time, is_run, is_pause, create_time, del_flag";


    public AutoJobTriggerEntityMapper() {
        super(AutoJobTriggerEntity.class);
    }

    public AutoJobTriggerEntity selectOneByTaskId(long taskId) {
        String sql = "select " + getAllColumns() + " from " + getTableName() + " where task_id = ? and del_flag = 0";
        return queryOne(sql, taskId);
    }

    /**
     * 暂停某个任务
     *
     * @param taskId 任务Id
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/8/26 9:39
     */
    public boolean pauseTaskById(long taskId) {
        String sql = getUpdateExpression() + " set is_pause = 1 where del_flag = 0 and task_id = ?";
        try {
            updateOne(sql, taskId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 取消对某个任务的暂停
     *
     * @param taskId 任务id
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/8/26 9:51
     */
    public boolean unpauseTaskById(long taskId) {
        String sql = getUpdateExpression() + " set is_pause = 0 where del_flag = 0 and task_id = ?";
        try {
            updateOne(sql, taskId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateOperatingStatus(boolean isRunning, long taskId) {
        String condition = String.format(" set is_run = %d where task_id = ?", isRunning ? 1 : 0);
        return updateOne(getUpdateExpression() + condition, taskId) == 1;
    }

    public int updateOperatingStatuses(boolean isRunning, List<Long> taskId) {
        String condition = String.format(" set is_run = %d where task_id in (" + idRepeat(taskId) + ") and del_flag = 0", isRunning ? 1 : 0);
        return updateOne(getUpdateExpression() + condition);
    }

    /**
     * 更新触发器状态
     *
     * @param finishedTimes      已完成次数
     * @param lastTriggeringTime 上次触发时间
     * @param nextTriggeringTime 下次触发时间
     * @param isLastSuccess      上次是否执行成功
     * @param taskId             任务Id
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/8/21 9:28
     */
    public boolean updateStatus(int finishedTimes, int currentRepeatTimes, long lastTriggeringTime, long nextTriggeringTime, long lastRunTime, boolean isLastSuccess, long taskId) {
        String sql = " set finished_times = ? ,current_repeat_times = ?,next_triggering_time = ? ,last_triggering_time = ? ,is_last_success = ? ,last_run_time = ? where task_id = ? and del_flag = 0";
        return updateOne(getUpdateExpression() + sql, finishedTimes, currentRepeatTimes, nextTriggeringTime, lastTriggeringTime, isLastSuccess ? 1 : 0, lastRunTime, taskId) == 1;
    }

    public boolean updateTriggeringTime(long taskId, long triggeringTime) {
        String sql = " set next_triggering_time = ? where task_id  = ? and del_flag = 0";
        return updateOne(getUpdateExpression() + sql, triggeringTime, taskId) == 1;
    }

    public int deleteByTaskIds(List<Long> taskIds) {
        String condition = " where task_id in (" + idRepeat(taskIds) + ") and del_flag = 0";
        return updateBatch(getDeleteExpression() + condition, new Object[][]{});
    }

    public int updateByTaskId(TriggerEditParams triggerEditParams, long taskId) {
        if (triggerEditParams == null || ObjectUtil.isNull(triggerEditParams)) {
            return 0;
        }
        AutoJobTriggerEntity updateEntity = new AutoJobTriggerEntity();
        //以下字段支持修改
        updateEntity.setCronExpression(triggerEditParams.getCronExpression());
        updateEntity.setRepeatTimes(triggerEditParams.getRepeatTimes());
        updateEntity.setCycle(triggerEditParams.getCycle());
        updateEntity.setChildTasksId(triggerEditParams.getChildTasksId());
        updateEntity.setMaximumExecutionTime(triggerEditParams.getMaximumExecutionTime());
        return updateEntity(updateEntity, "task_id = ?", taskId);
    }

    /**
     * 查询未来会执行的触发器
     *
     * @param nearTime 未来的时间
     * @param unit     单位
     * @return java.util.List<com.example.autojob.skeleton.db.entity.AutoJobTriggerEntity>
     * @author Huang Yongxiang
     * @date 2022/8/26 18:44
     */
    public List<AutoJobTriggerEntity> selectNearTrigger(long nearTime, TimeUnit unit) {
        String condition = "  where next_triggering_time > ? and next_triggering_time < ? and finished_times< repeat_times and del_flag = 0 and is_pause = 0";
        return queryList(getSelectExpression() + condition, System.currentTimeMillis(), System.currentTimeMillis() + unit.toMillis(nearTime));
    }


    @Override
    public String getAllColumns() {
        return ALL_COLUMNS;
    }

    @Override
    public String getTableName() {
        return DBTableConstant.TRIGGER_TABLE_NAME;
    }


}
