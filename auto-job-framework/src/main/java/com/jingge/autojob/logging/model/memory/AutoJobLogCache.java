package com.jingge.autojob.logging.model.memory;

import com.jingge.autojob.skeleton.framework.config.AutoJobLogConfig;
import com.jingge.autojob.logging.domain.AutoJobLog;
import com.jingge.autojob.util.cache.LocalCacheManager;
import com.jingge.autojob.util.convert.StringUtils;
import net.jodah.expiringmap.ExpirationPolicy;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 任务日志内存缓存，内存Cache推荐仅做测试用，实际生成日志应该放到数据库里
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/12 15:40
 */
public class AutoJobLogCache implements IAutoJobLogCache<AutoJobLog> {
    private LocalCacheManager<String, List<AutoJobLog>> taskLogCache;

    public AutoJobLogCache(AutoJobLogConfig config) {
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
    public boolean insert(String taskPath, AutoJobLog log) {
        return insertAll(taskPath, Collections.singletonList(log));
    }

    @Override
    public boolean insertAll(String taskPath, List<AutoJobLog> autoJobLogs) {
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
    public List<AutoJobLog> get(String taskPath) {
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
