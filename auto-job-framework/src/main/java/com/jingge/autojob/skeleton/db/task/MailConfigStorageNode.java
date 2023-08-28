package com.jingge.autojob.skeleton.db.task;

import com.jingge.autojob.skeleton.db.StorageNode;
import com.jingge.autojob.skeleton.framework.config.ConfigRepository;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Huang Yongxiang
 * @date 2023-01-11 11:34
 * @email 1158055613@qq.com
 */
public class MailConfigStorageNode implements StorageNode<AutoJobTask> {
    @Override
    public int store(List<AutoJobTask> entities) {
        return ConfigRepository.storageConfigs(entities
                .stream()
                .map(AutoJobTask::getMailConfig)
                .collect(Collectors.toList()));
    }
}
