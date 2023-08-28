package com.jingge.autojob.skeleton.db.mapper;

import com.jingge.autojob.api.task.params.MethodTaskEditParams;
import com.jingge.autojob.api.task.params.TaskEditParams;
import com.jingge.autojob.skeleton.db.StorageChain;
import com.jingge.autojob.skeleton.db.TransactionManager;
import com.jingge.autojob.skeleton.db.entity.AutoJobMethodTaskEntity;
import com.jingge.autojob.skeleton.db.task.*;
import com.jingge.autojob.skeleton.db.task.*;
import com.jingge.autojob.skeleton.enumerate.SaveStrategy;
import com.jingge.autojob.skeleton.framework.config.AutoJobConfigHolder;
import com.jingge.autojob.skeleton.framework.config.DBTableConstant;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.util.id.IdGenerator;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 方法型任务的Mapper
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-03-15 15:28
 * @email 1158055613@qq.com
 */
@Slf4j
public class AutoJobMethodTaskEntityMapper extends AutoJobTaskEntityBaseMapper<AutoJobMethodTaskEntity> {
    public static final String ALL_COLUMNS = "content, method_class_name, method_name, method_object_factory";


    public AutoJobMethodTaskEntityMapper() {
        super(AutoJobMethodTaskEntity.class);
    }

    @Override
    public boolean saveTasks(List<AutoJobTask> tasks) {
        List<Long> ids = filterExistTask(SaveStrategy.SAVE_IF_ABSENT.filterTasks(tasks));
        tasks.removeIf(item -> ids.contains(item.getVersionId()));
        TransactionManager.openTransaction(dataSourceHolder, Connection.TRANSACTION_SERIALIZABLE);
        deleteExistTaskByVersionID(SaveStrategy.UPDATE.filterTasks(tasks));
        boolean flag = new StorageChain.Builder<AutoJobTask>()
                .addNode(new TriggerStorageNode())
                .addNode(new TryConfigStorageNode())
                .addNode(new MailConfigStorageNode())
                .addNode(new ShardingConfigStorageNode())
                .addNode(new MethodTaskStorageNode())
                .build()
                .store(tasks, Connection.TRANSACTION_SERIALIZABLE);
        if (flag) {
            List<AutoJobMapperHolder.TaskTypeMatcher> matchers = tasks
                    .stream()
                    .map(task -> {
                        AutoJobMapperHolder.TaskTypeMatcher taskTypeMatcher = new AutoJobMapperHolder.TaskTypeMatcher();
                        taskTypeMatcher.taskType = 0;
                        taskTypeMatcher.delFlag = 0;
                        taskTypeMatcher.id = IdGenerator.getNextIdAsLong();
                        taskTypeMatcher.status = 1;
                        taskTypeMatcher.taskID = task.getId();
                        return taskTypeMatcher;
                    })
                    .collect(Collectors.toList());
            AutoJobMapperHolder.TASK_TYPE_MATCHER_MAPPER.insertList(matchers);
        }
        return flag;
    }

    @Override
    public int updateById(TaskEditParams editParams, long taskId) {
        if (editParams == null) {
            return -1;
        }
        AutoJobMethodTaskEntity entity = new AutoJobMethodTaskEntity();
        entity.setAlias(editParams.getAlias());
        entity.setBelongTo(editParams.getBelongTo());
        entity.setTaskLevel(editParams.getTaskLevel());
        if (editParams instanceof MethodTaskEditParams) {
            MethodTaskEditParams methodTaskEditParams = (MethodTaskEditParams) editParams;
            entity.setMethodObjectFactory(methodTaskEditParams.getMethodObjectFactory());
            entity.setParams(methodTaskEditParams.getParamsString());
        }
        return updateEntity(entity, "id = ?", taskId);
    }


    @Override
    public String getAllColumns() {
        return BASE_COLUMNS + "," + ALL_COLUMNS;
    }

    @Override
    public String getTableName() {
        return DBTableConstant.METHOD_TASK_TABLE_NAME;
    }
}
