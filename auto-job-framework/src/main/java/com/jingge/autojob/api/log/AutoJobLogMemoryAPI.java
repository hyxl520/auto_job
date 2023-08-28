package com.jingge.autojob.api.log;

import com.jingge.autojob.logging.domain.AutoJobLog;
import com.jingge.autojob.logging.domain.AutoJobRunLog;
import com.jingge.autojob.logging.domain.AutoJobSchedulingRecord;

import java.util.Date;
import java.util.List;

/**
 * 内存日志API，暂不支持
 *
 * @Author Huang Yongxiang
 * @Date 2022/11/07 14:56
 * @Email 1158055613@qq.com
 */
public class AutoJobLogMemoryAPI implements AutoJobLogAPI {
    @Override
    public List<AutoJobSchedulingRecord> page(Integer pageCount, Integer pageSize, Long taskId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integer count(Long taskId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutoJobSchedulingRecord> findSchedulingRecordsBetween(Long taskId, Date start, Date end) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutoJobLog> findLogsBySchedulingId(Long schedulingId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutoJobRunLog> findRunLogsBySchedulingId(Long schedulingId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutoJobLog> findLogsByTaskIdBetween(Long taskId, Date start, Date end) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutoJobRunLog> findRunLogsByTaskIdBetween(Long taskId, Date start, Date end) {
        throw new UnsupportedOperationException();
    }
}
