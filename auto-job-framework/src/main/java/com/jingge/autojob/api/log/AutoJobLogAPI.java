package com.jingge.autojob.api.log;

import com.jingge.autojob.logging.domain.AutoJobLog;
import com.jingge.autojob.logging.domain.AutoJobRunLog;
import com.jingge.autojob.logging.domain.AutoJobSchedulingRecord;

import java.util.Date;
import java.util.List;

/**
 * 日志API接口
 *
 * @Author Huang Yongxiang
 * @Date 2022/11/02 14:51
 * @Email 1158055613@qq.com
 */
public interface AutoJobLogAPI {
    /**
     * 分页查询指定任务的调度记录
     *
     * @param pageCount 页数
     * @param pageSize  每页条数
     * @param taskId    任务ID
     * @return java.util.List<com.jingge.autojob.logging.domain.AutoJobSchedulingRecord>
     * @author Huang Yongxiang
     * @date 2022/12/27 15:14
     */
    List<AutoJobSchedulingRecord> page(Integer pageCount, Integer pageSize, Long taskId);

    /**
     * 获取指定任务的调度记录总数目
     *
     * @param taskId 任务ID
     * @return java.lang.Integer
     * @author Huang Yongxiang
     * @date 2022/12/27 15:37
     */
    Integer count(Long taskId);

    /**
     * 查询指定任务指定时间区间的调度记录
     *
     * @param taskId 任务ID
     * @param start  起时间
     * @param end    止时间
     * @return java.util.List<com.jingge.autojob.logging.domain.AutoJobSchedulingRecord>
     * @author Huang Yongxiang
     * @date 2022/12/27 15:35
     */
    List<AutoJobSchedulingRecord> findSchedulingRecordsBetween(Long taskId, Date start, Date end);

    /**
     * 通过调度ID查询指定调度的运行日志，该接口返回的日志是实时的
     *
     * @param schedulingId 调度ID
     * @return java.util.List<com.jingge.autojob.logging.domain.AutoJobLog>
     * @author Huang Yongxiang
     * @date 2022/11/7 14:47
     */
    List<AutoJobLog> findLogsBySchedulingId(Long schedulingId);

    /**
     * 通过调度ID查询指定调度的执行日志，该接口返回的日志是实时的
     *
     * @param schedulingId 调度ID
     * @return java.util.List<com.jingge.autojob.logging.domain.AutoJobRunLog>
     * @author Huang Yongxiang
     * @date 2022/11/7 14:49
     */
    List<AutoJobRunLog> findRunLogsBySchedulingId(Long schedulingId);

    /**
     * 查询指定任务指定时间区间的运行日志
     *
     * @param taskId 任务ID
     * @param start  起时间
     * @param end    止时间
     * @return java.util.List<com.jingge.autojob.logging.domain.AutoJobLog>
     * @author Huang Yongxiang
     * @date 2022/12/27 15:20
     */
    List<AutoJobLog> findLogsByTaskIdBetween(Long taskId, Date start, Date end);

    /**
     * 查询指定任务指定时间区间的执行日志
     *
     * @param taskId 任务ID
     * @param start  起时间
     * @param end    止时间
     * @return java.util.List<com.jingge.autojob.logging.domain.AutoJobRunLog>
     * @author Huang Yongxiang
     * @date 2022/12/27 15:21
     */
    List<AutoJobRunLog> findRunLogsByTaskIdBetween(Long taskId, Date start, Date end);
}
