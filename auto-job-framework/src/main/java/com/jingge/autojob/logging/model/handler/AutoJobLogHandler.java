package com.jingge.autojob.logging.model.handler;

import com.jingge.autojob.logging.domain.AutoJobLog;
import com.jingge.autojob.logging.domain.AutoJobRunLog;
import com.jingge.autojob.logging.domain.AutoJobSchedulingRecord;
import com.jingge.autojob.logging.model.consumer.DefaultLogSaveStrategyDelegate;
import com.jingge.autojob.logging.model.consumer.DefaultRunLogSaveStrategyDelegate;
import com.jingge.autojob.logging.model.consumer.ILogSaveStrategyDelegate;
import com.jingge.autojob.skeleton.db.entity.EntityConvertor;
import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.task.AutoJobContext;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.util.convert.DefaultValueUtil;
import com.jingge.autojob.util.servlet.InetUtil;
import com.jingge.autojob.util.thread.SyncHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 一个任务一次完整的调度日志
 *
 * @Author Huang Yongxiang
 * @Date 2022/10/21 14:30
 */
@Slf4j
public class AutoJobLogHandler {
    /**
     * 调度ID
     */
    private long schedulingId;
    /**
     * 任务ID
     */
    private final long taskId;
    /**
     * 调度记录
     */
    private AutoJobSchedulingRecord record;
    /**
     * 运行日志
     */
    private final List<AutoJobRunLog> runLogs = new ArrayList<>();
    /**
     * 处理的任务
     */
    private AutoJobTask handleTask;
    /**
     * 任务日志
     */
    private final List<AutoJobLog> logs = new ArrayList<>();
    private final ILogSaveStrategyDelegate<AutoJobLog> logSaveStrategyDelegate;
    private final ILogSaveStrategyDelegate<AutoJobRunLog> runLogSaveStrategyDelegate;

    /**
     * 保存周期，一定周期保存一次日志
     */
    private long saveCycle;

    /**
     * 最大缓冲长度，日志达到该长度后自动保存
     */
    private int maxBufferLength;

    private final AtomicLong lastSaveTime = new AtomicLong(System.currentTimeMillis());

    private volatile boolean isFinished = false;

    public AutoJobLogHandler(AutoJobTask task) {
        this(task, null, null);
    }

    public AutoJobLogHandler(AutoJobTask task, ILogSaveStrategyDelegate<AutoJobLog> logSaveStrategyDelegate, ILogSaveStrategyDelegate<AutoJobRunLog> runLogSaveStrategyDelegate) {
        schedulingId = task
                .getTrigger()
                .getSchedulingRecordID();
        record = new AutoJobSchedulingRecord(task);
        record.setSchedulingId(schedulingId);
        taskId = task.getId();
        saveCycle = 5000;
        maxBufferLength = 10;
        handleTask = task;
        this.logSaveStrategyDelegate = DefaultValueUtil.defaultValue(logSaveStrategyDelegate, new DefaultLogSaveStrategyDelegate());
        this.runLogSaveStrategyDelegate = DefaultValueUtil.defaultValue(runLogSaveStrategyDelegate, new DefaultRunLogSaveStrategyDelegate());
    }

    public AutoJobLogHandler setHandleTask(AutoJobTask handleTask) {
        this.handleTask = handleTask;
        return this;
    }

    public boolean startScheduling() {
        return AutoJobMapperHolder.SCHEDULING_RECORD_ENTITY_MAPPER.insertList(Collections.singletonList(EntityConvertor.schedulingRecord2Entity(record))) == 1;
    }

    public void addRunLog(AutoJobRunLog runLog) {
        if (runLog == null) {
            return;
        }
        runLog.setSchedulingId(schedulingId);
        runLogs.add(runLog);
    }

    public void addAllRunLogs(List<AutoJobRunLog> runLogs) {
        this.runLogs.addAll(runLogs
                .stream()
                .filter(Objects::nonNull)
                .peek(log -> log.setSchedulingId(schedulingId))
                .collect(Collectors.toList()));
    }

    public void addLog(AutoJobLog log) {
        if (log == null) {
            return;
        }
        log.setSchedulingId(schedulingId);
        logs.add(log);
    }

    public void addAllLogs(List<AutoJobLog> logs) {
        this.logs.addAll(logs
                .stream()
                .filter(Objects::nonNull)
                .peek(log -> log.setSchedulingId(schedulingId))
                .collect(Collectors.toList()));
    }

    public synchronized void saveRunLogs() {
        runLogSaveStrategyDelegate
                .doDelegate(AutoJobApplication
                        .getInstance()
                        .getConfigHolder(), AutoJobRunLog.class)
                .doHandle(schedulingId + "", runLogs);
        runLogs.clear();
    }

    public synchronized void saveLogs() {
        logSaveStrategyDelegate
                .doDelegate(AutoJobApplication
                        .getInstance()
                        .getConfigHolder(), AutoJobLog.class)
                .doHandle(schedulingId + "", logs);
        logs.clear();
    }

    public void finishScheduling(boolean isSuccess, String result, long executingTime) {
        AutoJobMapperHolder.SCHEDULING_RECORD_ENTITY_MAPPER.updateResult(schedulingId, isSuccess, result, executingTime, InetUtil.getTCPAddress());
    }

    public boolean isFinished() {
        return isFinished;
    }

    public AutoJobLogHandler setSaveCycle(long saveCycle, TimeUnit unit) {
        this.saveCycle = unit.toMillis(saveCycle);
        return this;
    }

    public AutoJobLogHandler setMaxBufferLength(int maxBufferLength) {
        this.maxBufferLength = maxBufferLength;
        return this;
    }

    public void stop() {
        save();
        isFinished = true;
    }

    public void refresh(AutoJobTask task) {
        this.handleTask = task;
        schedulingId = handleTask
                .getTrigger()
                .getSchedulingRecordID();
        record = new AutoJobSchedulingRecord(handleTask);
        record.setSchedulingId(schedulingId);
    }

    boolean reachSavingTime() {
        return !isFinished && (System.currentTimeMillis() - lastSaveTime.get() >= saveCycle || logs.size() >= maxBufferLength);
    }

    void save() {
        SyncHelper.sleepQuietly(1, TimeUnit.SECONDS);
        saveLogs();
        lastSaveTime.set(System.currentTimeMillis());
        if (runLogs.size() > 0) {
            saveRunLogs();
        }
    }

}
