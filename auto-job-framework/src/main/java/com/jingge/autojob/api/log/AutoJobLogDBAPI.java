package com.jingge.autojob.api.log;

import com.jingge.autojob.logging.domain.AutoJobLog;
import com.jingge.autojob.logging.domain.AutoJobRunLog;
import com.jingge.autojob.logging.domain.AutoJobSchedulingRecord;
import com.jingge.autojob.skeleton.db.entity.EntityConvertor;
import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;
import com.jingge.autojob.util.convert.DefaultValueUtil;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于数据库存储的日志接口实现
 *
 * @Author Huang Yongxiang
 * @Date 2022/11/07 11:39
 * @Email 1158055613@qq.com
 */
public class AutoJobLogDBAPI implements AutoJobLogAPI {
    @Override
    public List<AutoJobSchedulingRecord> page(Integer pageCount, Integer pageSize, Long taskId) {
        if (pageCount == null || pageSize == null || taskId == null) {
            throw new NullPointerException();
        }

        return AutoJobMapperHolder.SCHEDULING_RECORD_ENTITY_MAPPER
                .pageByTaskId(pageCount, pageSize, taskId)
                .stream()
                .map(EntityConvertor::entity2schedulingRecord)
                .collect(Collectors.toList());
    }

    @Override
    public Integer count(Long taskId) {
        if (taskId == null) {
            return 0;
        }
        return AutoJobMapperHolder.SCHEDULING_RECORD_ENTITY_MAPPER.countByTaskId(taskId);
    }

    @Override
    public List<AutoJobSchedulingRecord> findSchedulingRecordsBetween(Long taskId, Date start, Date end) {
        if (taskId == null || start == null) {
            throw new NullPointerException();
        }
        end = DefaultValueUtil.defaultValue(end, new Date());
        return AutoJobMapperHolder.SCHEDULING_RECORD_ENTITY_MAPPER
                .listBetween(taskId, start, end)
                .stream()
                .map(EntityConvertor::entity2schedulingRecord)
                .collect(Collectors.toList());
    }

    @Override
    public List<AutoJobLog> findLogsBySchedulingId(Long schedulingId) {
        return AutoJobMapperHolder.LOG_ENTITY_MAPPER
                .selectBySchedulingId(schedulingId)
                .stream()
                .map(EntityConvertor::logEntity2Log)
                .collect(Collectors.toList());
    }

    @Override
    public List<AutoJobRunLog> findRunLogsBySchedulingId(Long schedulingId) {
        return AutoJobMapperHolder.RUN_LOG_ENTITY_MAPPER
                .selectBySchedulingId(schedulingId)
                .stream()
                .map(EntityConvertor::runLogEntity2RunLog)
                .collect(Collectors.toList());
    }

    @Override
    public List<AutoJobLog> findLogsByTaskIdBetween(Long taskId, Date start, Date end) {
        return AutoJobMapperHolder.LOG_ENTITY_MAPPER
                .selectByTaskIdBetween(start, end, taskId)
                .stream()
                .map(EntityConvertor::logEntity2Log)
                .collect(Collectors.toList());
    }

    @Override
    public List<AutoJobRunLog> findRunLogsByTaskIdBetween(Long taskId, Date start, Date end) {
        return AutoJobMapperHolder.RUN_LOG_ENTITY_MAPPER
                .selectByTaskIdBetween(start, end, taskId)
                .stream()
                .map(EntityConvertor::runLogEntity2RunLog)
                .collect(Collectors.toList());
    }
}
