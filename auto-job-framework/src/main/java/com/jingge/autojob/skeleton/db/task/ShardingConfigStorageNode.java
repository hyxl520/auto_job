package com.jingge.autojob.skeleton.db.task;

import com.jingge.autojob.skeleton.db.StorageNode;
import com.jingge.autojob.skeleton.framework.config.ConfigRepository;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-07-28 15:47
 * @email 1158055613@qq.com
 */
public class ShardingConfigStorageNode implements StorageNode<AutoJobTask> {
    @Override
    public int store(List<AutoJobTask> entities) {
        return ConfigRepository.storageConfigs(entities
                .stream()
                .map(AutoJobTask::getShardingConfig)
                .collect(Collectors.toList()));
    }
}
