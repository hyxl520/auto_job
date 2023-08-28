package com.jingge.autojob.logging.model.consumer.strategy;

import com.jingge.autojob.logging.domain.AutoJobRunLog;
import com.jingge.autojob.logging.model.consumer.IAutoJobLogSaveStrategy;
import com.jingge.autojob.logging.model.memory.AutoJobRunLogCache;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;

import java.util.List;

/**
 * 运行日志的内存Cache保存策略
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/26 11:00
 */
public class AutoJobRunLogMemoryStrategy implements IAutoJobLogSaveStrategy<AutoJobRunLog> {
    private final AutoJobRunLogCache runLogCache = AutoJobApplication.getInstance().getLogContext().getRunLogCache();

    @Override
    public void doHandle(String taskPath, List<AutoJobRunLog> logList) {
        if (logList == null || logList.size() == 0) {
            return;
        }
        runLogCache.insertAll(taskPath, logList);
    }
}
