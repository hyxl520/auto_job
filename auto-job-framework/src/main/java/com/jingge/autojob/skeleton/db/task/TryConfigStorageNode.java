package com.jingge.autojob.skeleton.db.task;

import com.jingge.autojob.skeleton.db.StorageNode;
import com.jingge.autojob.skeleton.framework.config.ConfigRepository;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 重试配置存储节点
 *
 * @author Huang Yongxiang
 * @date 2023-01-05 11:06
 * @email 1158055613@qq.com
 */
public class TryConfigStorageNode implements StorageNode<AutoJobTask> {
    @Override
    public int store(List<AutoJobTask> entities) {
        return ConfigRepository.storageConfigs(entities
                .stream()
                .map(AutoJobTask::getRetryConfig)
                .collect(Collectors.toList()));
    }
}
