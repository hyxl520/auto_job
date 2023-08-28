package com.jingge.autojob.skeleton.model.scheduler;

import com.jingge.autojob.skeleton.db.entity.EntityConvertor;
import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.config.*;
import com.jingge.autojob.skeleton.framework.task.AutoJobContext;
import com.jingge.autojob.skeleton.framework.task.AutoJobRunningStatus;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lang.WithDaemonThread;
import com.jingge.autojob.skeleton.lifecycle.TaskEventFactory;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskMissFireEvent;
import com.jingge.autojob.skeleton.lifecycle.manager.TaskEventManager;
import com.jingge.autojob.skeleton.model.executor.AutoJobTaskExecutorPool;
import com.jingge.autojob.skeleton.model.register.IAutoJobRegister;
import com.jingge.autojob.skeleton.model.tq.AutoJobTimeWheel;
import com.jingge.autojob.util.convert.DateUtils;
import com.jingge.autojob.util.thread.ScheduleTaskUtil;
import com.jingge.autojob.skeleton.framework.config.AutoJobConfigHolder;
import com.jingge.autojob.skeleton.framework.config.AutoJobConstant;
import com.jingge.autojob.skeleton.framework.config.AutoJobShardingConfig;
import com.jingge.autojob.skeleton.framework.config.ConfigJsonSerializerAndDeserializer;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 时间轮调度器
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/07 17:51
 */
@Slf4j
public class AutoJobTimeWheelScheduler extends AbstractScheduler implements WithDaemonThread {
    private final AutoJobTimeWheel timeWheel;

    private final ScheduleTaskUtil startSchedulerThread;

    private final ScheduleTaskUtil transferSchedulerThread;

    private static final long ADVANCE_TIME = AutoJobConstant.beforeSchedulingInTimeWheel;


    public AutoJobTimeWheelScheduler(AutoJobTaskExecutorPool executorPool, IAutoJobRegister register, AutoJobConfigHolder configHolder) {
        super(executorPool, register, configHolder);
        this.startSchedulerThread = ScheduleTaskUtil.build(false, "startSchedulerThread");
        this.transferSchedulerThread = ScheduleTaskUtil.build(false, "transferSchedulerThread");
        this.timeWheel = new AutoJobTimeWheel();
    }


    @Override
    public void startWork() {
        Runnable roll = () -> {
            try {
                int second = (int) ((System.currentTimeMillis() / 1000) % 60);
                //log.info("时间轮下标：{}", second);
                List<AutoJobTask> tasks = timeWheel.getSecondTasks(second);
                //当前时间前3S的任务也会被执行，防止任务周期过短导致的missFire
                //for (int i = 1; i <= 3; i++) {
                //    tasks.addAll(timeWheel.getSecondTasks((int) (((System.currentTimeMillis() - 1000 * i) / 1000) % 60)));
                //}
                if (tasks.size() > 0) {
                    tasks.forEach(item -> {
                        submitTask(item);
                        if (AutoJobConfigHolder
                                .getInstance()
                                .isDebugEnable()) {
                            log.warn("任务：{}-{}时间轮触发成功", item.getId(), DateUtils.formatDateTime(item
                                    .getTrigger()
                                    .getTriggeringTime()));
                        }
                    });
                }
                //获取时间轮是否有残留任务
                int mid = (int) (60 - ADVANCE_TIME / 1000);
                int scanStart = second < mid ? 0 : second - mid + 1;
                for (int i = scanStart; i <= second; i++) {
                    List<AutoJobTask> leftTasks = timeWheel.getSecondTasks(i);
                    if (leftTasks != null && leftTasks.size() > 0) {
                        int finalI = i;
                        leftTasks.forEach(task -> {
                            if (AutoJobConfigHolder
                                    .getInstance()
                                    .isDebugEnable()) {
                                log.warn("任务{} miss fire on second: {}", task.getId(), finalI);
                            }
                            //miss fire的事件处理采用异步处理，避免阻塞时间轮的调度
                            TaskEventManager
                                    .getInstance()
                                    .publishTaskEvent(TaskEventFactory.newTaskMissFireEvent(task), TaskMissFireEvent.class, true);
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        Runnable timeWheelSchedule = () -> {
            AutoJobTask headTask = register.readTask();
            if (headTask == null || headTask.getTrigger() == null) {
                return;
            }
            //任务如果已经暂停移出调度队列不作处理
            if (headTask
                    .getTrigger()
                    .getIsPause()) {
                log.warn("任务{}已被暂停", headTask.getId());
                register.removeAndGetTaskByScheduleQueueID(headTask.getScheduleQueueId());
                return;
            }

            //任务missFire
            if (headTask
                    .getTrigger()
                    .getTriggeringTime() - 1000 < System.currentTimeMillis()) {
                if (AutoJobConfigHolder
                        .getInstance()
                        .isDebugEnable()) {
                    log.warn("任务{} miss fire", headTask.getScheduleQueueId());
                }
                TaskEventManager
                        .getInstance()
                        .publishTaskEvent(TaskEventFactory.newTaskMissFireEvent(headTask), TaskMissFireEvent.class, true);
                register.removeAndGetTaskByScheduleQueueID(headTask.getScheduleQueueId());
                return;
            }

            //判断执行时间
            if (!headTask
                    .getTrigger()
                    .isNearTriggeringTime(ADVANCE_TIME)) {
                return;
            }

            try {
                //任务必须要为调度状态或者重试状态才能参与调度，解决Memory任务重新执行的问题
                if (headTask.getRunningStatus() != AutoJobRunningStatus.SCHEDULING && headTask.getRunningStatus() != AutoJobRunningStatus.RETRYING) {
                    if (AutoJobConfigHolder
                            .getInstance()
                            .isDebugEnable()) {
                        log.warn("任务{}的状态{}不符合调度要求", headTask.getId(), headTask.getRunningStatus());
                    }
                    return;
                }
                if (headTask.isSharding() && timeWheel.joinTask(headTask)) {
                    if (AutoJobConfigHolder
                            .getInstance()
                            .isDebugEnable()) {
                        log.warn("任务{}的分片{}已经调度进时间轮", headTask.getId(), headTask.getShardingId());
                    }
                    return;
                }
                //如果是分片任务直接放入时间轮
                if (headTask.getIsShardingTask() && headTask.getIsAlreadyBroadcastSharding() && timeWheel.joinTask(headTask)) {
                    if (AutoJobConfigHolder
                            .getInstance()
                            .isDebugEnable()) {
                        log.warn("任务{}的分片{}已经调度进时间轮", headTask.getId(), headTask.getShardingId());
                    }
                    return;
                }

                //先获取锁
                boolean isSchedulable = headTask.getType() == AutoJobTask.TaskType.MEMORY_TASk || headTask.isSharding() || (headTask.getType() == AutoJobTask.TaskType.DB_TASK && isLatest(headTask) && lock(headTask.getId()));
                if (!isSchedulable) {
                    if (AutoJobConfigHolder
                            .getInstance()
                            .isDebugEnable()) {
                        log.warn("任务{}不符合调度条件，移出调度队列", headTask.getId());
                    }
                    return;
                }
                if (headTask.getIsShardingTask() && !headTask.getIsAlreadyBroadcastSharding()) {
                    if (headTask.getType() == AutoJobTask.TaskType.DB_TASK && headTask.getShardingConfig() == null) {
                        AutoJobShardingConfig shardingConfig = (AutoJobShardingConfig) EntityConvertor.entity2StorableConfig(AutoJobMapperHolder.CONFIG_ENTITY_MAPPER.selectByTaskIdAndType(AutoJobShardingConfig.class.getName(), headTask.getId()), new ConfigJsonSerializerAndDeserializer());
                        if (shardingConfig == null) {
                            log.warn("任务{}分片配置为null", headTask.getId());
                            return;
                        }
                        headTask.setShardingConfig(shardingConfig);
                    }
                    if (headTask
                            .getShardingConfig()
                            .isEnable()) {
                        AutoJobApplication
                                .getInstance()
                                .getShardingManager()
                                .addTask(headTask);
                    } else if (timeWheel.joinTask(headTask)) {
                        if (AutoJobConfigHolder
                                .getInstance()
                                .isDebugEnable()) {
                            log.warn("任务{}已经调度进时间轮", headTask.getId());
                        }
                    }
                } else if (timeWheel.joinTask(headTask)) {
                    if (AutoJobConfigHolder
                            .getInstance()
                            .isDebugEnable()) {
                        log.warn("任务{}已经调度进时间轮", headTask.getId());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                register.removeAndGetTaskByScheduleQueueID(headTask.getScheduleQueueId());
            }
        };
        //时间轮滚动
        startSchedulerThread.EFixedRateTask(roll, 1000 - System.currentTimeMillis() % 1000, 1000, TimeUnit.MILLISECONDS);
        //调度队列消费
        transferSchedulerThread.EFixedRateTask(timeWheelSchedule, 1000 - System.currentTimeMillis() % 1000, 10, TimeUnit.MILLISECONDS);
    }


    @Override
    public void execute() {
        startWork();
    }

    @Override
    public void destroy() {
        startSchedulerThread.shutdown();
        transferSchedulerThread.shutdown();
    }
}
