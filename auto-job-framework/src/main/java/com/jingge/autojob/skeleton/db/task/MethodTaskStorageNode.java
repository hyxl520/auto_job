package com.jingge.autojob.skeleton.db.task;

import com.jingge.autojob.skeleton.db.StorageNode;
import com.jingge.autojob.skeleton.db.entity.AutoJobMethodTaskEntity;
import com.jingge.autojob.skeleton.db.entity.EntityConvertor;
import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-03-15 15:45
 * @email 1158055613@qq.com
 */
public class MethodTaskStorageNode implements StorageNode<AutoJobTask> {
    @Override
    public int store(List<AutoJobTask> entities) {
        return AutoJobMapperHolder.METHOD_TASK_ENTITY_MAPPER.insertList(entities
                .stream()
                .map(task -> (AutoJobMethodTaskEntity) EntityConvertor.task2Entity(task))
                .collect(Collectors.toList()));
    }
}
