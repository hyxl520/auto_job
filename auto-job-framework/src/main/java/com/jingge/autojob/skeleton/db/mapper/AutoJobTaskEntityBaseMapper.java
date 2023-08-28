package com.jingge.autojob.skeleton.db.mapper;

import com.jingge.autojob.api.task.params.TaskEditParams;
import com.jingge.autojob.skeleton.db.DataSourceHolder;
import com.jingge.autojob.skeleton.db.PageManager;
import com.jingge.autojob.skeleton.db.entity.AutoJobTaskBaseEntity;
import com.jingge.autojob.skeleton.db.entity.AutoJobTriggerEntity;
import com.jingge.autojob.skeleton.enumerate.DatabaseType;
import com.jingge.autojob.skeleton.framework.config.DBTableConstant;
import com.jingge.autojob.skeleton.framework.task.AutoJobRunningStatus;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.util.bean.ObjectUtil;
import com.jingge.autojob.util.convert.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-03-15 14:57
 * @email 1158055613@qq.com
 */
public abstract class AutoJobTaskEntityBaseMapper<T extends AutoJobTaskBaseEntity> extends BaseMapper<T> {
    /**
     * 所有列
     */
    public static final String BASE_COLUMNS = "id, alias, version_id, params, trigger_id, type, is_child_task, " + "is_sharding_task, run_lock, task_level, version, running_status, belong_to, status, executable_machines,create_time, del_flag";

    public AutoJobTaskEntityBaseMapper(DataSourceHolder dataSourceHolder, Class<T> type) {
        super(dataSourceHolder, type);
    }

    public AutoJobTaskEntityBaseMapper(Class<T> type) {
        super(type);
    }

    public abstract boolean saveTasks(List<AutoJobTask> tasks);

    @Override
    public int insertList(List<T> entities) {
        if (entities == null || entities.size() == 0) {
            return 0;
        }
        int length = ObjectUtil.getObjectFields(entities.get(0)).length;
        String slot = StringUtils.repeat("?", ",", length);
        String sql = "insert into " + getTableName() + "(" + getAllColumns() + ") values" + " (" + slot + ")";
        Object[][] params = new Object[entities.size()][];
        for (int i = 0; i < entities.size(); i++) {
            params[i] = parseEntityFieldValue(entities.get(i), getAllColumns()
                    .replace(" ", "")
                    .split(","));
        }
        return updateBatch(sql, params);
    }

    public boolean isLatestVersion(long annotationID, long taskID) {
        String sql = getSelectExpression() + " where version_id = ? and id > ? and del_flag = 0 and STATUS = 1";
        T entity = queryOne(sql, annotationID, taskID);
        return entity == null || entity.getTriggerId() == null;
    }

    /**
     * 尝试对任务进行上锁
     *
     * @param taskId 要上锁的任务Id
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/8/18 11:04
     */
    public boolean lock(long taskId) {

        AutoJobTaskBaseEntity entity = selectById(taskId);
        AutoJobTriggerEntity triggerEntity = AutoJobMapperHolder.TRIGGER_ENTITY_MAPPER.selectOneByTaskId(taskId);

        if (entity == null || ObjectUtil.isNull(entity)) {
            return false;
        }
        if (entity.getRunLock() == 1 || (triggerEntity != null && triggerEntity.getIsPause() == 1) || entity.getStatus() == 0) {
            return false;
        }
        int count = updateOne(getUpdateExpression() + " set run_lock = 1 where del_flag = 0 and id = ? and run_lock = 0", taskId);
        return count == 1;
    }

    public boolean unLock(long taskId) {
        return updateOne(getUpdateExpression() + " set run_lock = 0 where del_flag = 0 and id = ? and run_lock = 1", taskId) == 1;
    }


    /**
     * 查询未来时间内会执行的任务
     *
     * @param nearTime 未来时间段
     * @param unit     时间单位
     * @return java.util.List<com.example.autojob.skeleton.db.entity.AutoJobTaskEntity>
     * @author Huang Yongxiang
     * @date 2022/8/26 9:50
     */
    public List<T> selectNearTask(long nearTime, TimeUnit unit) {
        String command = "SELECT j.*  FROM %s j LEFT JOIN %s t ON j.id = t.task_id  WHERE j.id IN ( SELECT max( id ) " + "FROM %s WHERE del_flag = 0 AND STATUS = 1 GROUP BY version_id ORDER BY version DESC )  AND " + "next_triggering_time >= ?  AND next_triggering_time <= ? AND t.del_flag = 0  AND t.is_pause = 0 and " + "run_lock = 0 and (j.running_status = ? or j.running_status = ?)";
        String sql = String.format(command, getTableName(), DBTableConstant.TRIGGER_TABLE_NAME, getTableName());
        return queryList(sql, System.currentTimeMillis(), System.currentTimeMillis() + unit.toMillis(nearTime), AutoJobRunningStatus.SCHEDULING.getFlag(), AutoJobRunningStatus.RETRYING.getFlag());
    }


    public abstract int updateById(TaskEditParams editParams, long taskId);

    public boolean bindingTrigger(long triggerId, long taskId) {
        String sql = getUpdateExpression() + " set trigger_id = ? where id = ? and del_flag = 0";
        return updateOne(sql, triggerId, taskId) == 1;
    }

    /**
     * 查找最新版本的注解任务
     *
     * @param annotationId 注解上给定的id
     * @return com.example.autojob.skeleton.db.entity.AutoJobTaskEntity
     * @author Huang Yongxiang
     * @date 2022/8/26 9:51
     */
    public T selectLatestAnnotationTask(long annotationId) {
        String sql = getSelectExpression() + " where id = (select max(id) from " + getTableName() + " where version_id = ? and del_flag = 0 and status = 1)";
        return queryOne(sql, annotationId);
    }

    public boolean isNewTask(AutoJobTask task) {
        if (task.getId() != null && task.getVersionId() != null) {
            String sql = "select count(*) from " + getTableName() + " where version_id = ? and del_flag = 0 and status = 1";
            return conditionalCount(sql) == 0;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public List<Long> filterExistTask(List<AutoJobTask> tasks) {
        if (tasks == null || tasks.size() == 0) {
            return Collections.emptyList();
        }
        String sql = getSelectExpression() + " where version_id in (" + idRepeat(tasks
                .stream()
                .map(AutoJobTask::getVersionId)
                .collect(Collectors.toList())) + ") and del_flag = 0 and status = 1";
        List<AutoJobTaskBaseEntity> exist = (List<AutoJobTaskBaseEntity>) queryList(sql);
        List<Long> existIDs = exist
                .stream()
                .map(AutoJobTaskBaseEntity::getVersionId)
                .collect(Collectors.toList());
        tasks.removeIf(item -> existIDs.contains(item.getVersionId()));
        exist.clear();
        return existIDs;
    }

    public int deleteExistTaskByVersionID(List<AutoJobTask> tasks) {
        if (tasks == null || tasks.size() == 0) {
            return 0;
        }
        String versionIds = idRepeat(tasks
                .stream()
                .map(AutoJobTask::getVersionId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList()));
        String sql = getDeleteExpression() + "where version_id in (" + versionIds + ") and del_flag = 0";
        String triggerSQL = "delete from " + DBTableConstant.TRIGGER_TABLE_NAME + " where task_id in (select id from " + "aj_method_job where version_id in" + " (" + versionIds + ") and del_flag = 0) and del_flag = 0";
        String matcherSQL = "delete from " + DBTableConstant.JOB_TYPE_TABLE_MATCHER + " where task_id in (select id from " + "aj_method_job where version_id in" + " (" + versionIds + ") and del_flag = 0) and del_flag = 0";
        updateOne(triggerSQL);
        updateOne(matcherSQL);
        return updateOne(sql);
    }

    /**
     * 批量查询子任务
     *
     * @param ids 版本ID或任务ID
     * @return java.util.List<com.example.autojob.skeleton.db.entity.AutoJobTaskEntity>
     * @author Huang Yongxiang
     * @date 2022/12/27 10:48
     */
    public List<T> selectChildTasks(List<Long> ids) {
        String sql = getSelectExpression() + String.format(" where id in ( select max(id) from %s where (id in (%s) or version_id in (%s)) and is_child_task = 1 and del_flag = 0 and status = 1)", getTableName(), idRepeat(ids), idRepeat(ids));
        return queryList(sql);
    }

    @Override
    public int count() {
        String sql = "SELECT count(*) FROM " + getTableName() + " WHERE id in (SELECT max( id ) FROM " + getTableName() + " WHERE del_flag = 0 GROUP BY version_id)";
        return conditionalCount(sql);
    }

    @Override
    public List<T> page(int pageNum, int size) {
        String sql = new PageManager(DatabaseType.getCurrentDatabaseType()).getPageSql(getSelectExpression() + " " + "where id in ( SELECT max( id ) FROM " + getTableName() + " WHERE del_flag = 0 GROUP BY version_id )", pageNum, size);
        return queryList(sql);
    }

    public void updateRunningStatus(AutoJobRunningStatus status, long taskID) {
        String sql = getUpdateExpression() + " set running_status = " + status.getFlag() + " where id = ? and del_flag = 0 and status = 1";
        updateOne(sql, taskID);
    }

}
