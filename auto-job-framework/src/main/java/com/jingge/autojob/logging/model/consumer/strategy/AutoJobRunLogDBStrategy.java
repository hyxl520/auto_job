package com.jingge.autojob.logging.model.consumer.strategy;

import com.jingge.autojob.logging.domain.AutoJobRunLog;
import com.jingge.autojob.logging.model.consumer.IAutoJobLogSaveStrategy;
import com.jingge.autojob.skeleton.db.entity.AutoJobRunLogEntity;
import com.jingge.autojob.skeleton.db.entity.EntityConvertor;
import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 运行日志的DB保存策略
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/26 11:53
 */
public class AutoJobRunLogDBStrategy implements IAutoJobLogSaveStrategy<AutoJobRunLog> {
    @Override
    public void doHandle(String taskPath, List<AutoJobRunLog> logList) {
        if (logList == null || logList.size() == 0) {
            return;
        }
        List<AutoJobRunLogEntity> entities = logList.stream().map(EntityConvertor::runLog2RunLogEntity).collect(Collectors.toList());
        AutoJobMapperHolder.RUN_LOG_ENTITY_MAPPER.insertList(entities);
    }
}
