package com.jingge.autojob.logging.model.memory;

import com.jingge.autojob.skeleton.framework.config.AutoJobLogConfig;
import com.jingge.autojob.logging.domain.AutoJobRunLog;
import com.jingge.autojob.util.cache.LocalCacheManager;
import com.jingge.autojob.util.convert.StringUtils;
import net.jodah.expiringmap.ExpirationPolicy;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 运行日志缓存
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/12 16:16
 */
public class AutoJobRunLogCache implements IAutoJobLogCache<AutoJobRunLog> {
    private LocalCacheManager<String, List<AutoJobRunLog>> taskLogCache;

    public AutoJobRunLogCache(AutoJobLogConfig config) {
        if (config.getEnableMemory()) {
            taskLogCache = LocalCacheManager
                    .builder()
                    .setEntriesExpiration(false)
                    .setExpiringTime((long) (config.getMemoryDefaultExpireTime() * 60 * 1000), TimeUnit.MILLISECONDS)
                    .setMaxLength(config.getMemoryLength())
                    .setPolicy(ExpirationPolicy.CREATED)
                    .build();
        }
    }

    @Override
    public boolean insert(String taskPath, AutoJobRunLog log) {
        return insertAll(taskPath, Collections.singletonList(log));
    }

    @Override
    public boolean insertAll(String taskPath, List<AutoJobRunLog> autoJobLogs) {
        if (StringUtils.isEmpty(taskPath) || autoJobLogs == null) {
            return false;
        }
        if (taskLogCache != null) {
            try {
                if (!taskLogCache.exist(taskPath)) {
                    taskLogCache.set(taskPath, new LinkedList<>());
                }
                taskLogCache
                        .get(taskPath)
                        .addAll(autoJobLogs);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean exist(String taskPath) {
        if (StringUtils.isEmpty(taskPath) || taskLogCache == null) {
            return false;
        }
        return taskLogCache.exist(taskPath);
    }

    @Override
    public List<AutoJobRunLog> get(String taskPath) {
        if (StringUtils.isEmpty(taskPath) || taskLogCache == null || !taskLogCache.exist(taskPath)) {
            return Collections.emptyList();
        }
        return taskLogCache.get(taskPath);
    }

    @Override
    public boolean remove(String taskPath) {
        if (StringUtils.isEmpty(taskPath) || taskLogCache == null) {
            return false;
        }
        return taskLogCache.remove(taskPath);
    }
}
