package com.jingge.autojob.skeleton.model.scheduler;

import com.jingge.autojob.skeleton.db.entity.*;
import com.jingge.autojob.skeleton.framework.config.AutoJobConstant;
import com.jingge.autojob.skeleton.framework.task.AutoJobContext;
import com.jingge.autojob.skeleton.lang.WithDaemonThread;
import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;
import com.jingge.autojob.skeleton.framework.config.AutoJobConfigHolder;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.model.executor.AutoJobTaskExecutorPool;
import com.jingge.autojob.skeleton.model.register.AutoJobRegisterRefusedException;
import com.jingge.autojob.skeleton.model.register.IAutoJobRegister;
import com.jingge.autojob.util.thread.ScheduleTaskUtil;
import com.jingge.autojob.skeleton.db.entity.AutoJobMethodTaskEntity;
import com.jingge.autojob.skeleton.db.entity.AutoJobScriptTaskEntity;
import com.jingge.autojob.skeleton.db.entity.EntityConvertor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * DB task调度器
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/20 18:19
 */
@Slf4j
public class AutoJobDBTaskScheduler extends AbstractScheduler implements WithDaemonThread {
    private final ScheduleTaskUtil dbScheduleThread;

    /**
     * 调度器的通用构造方法，框架自动注册调度器时会执行该构造方法
     *
     * @param executorPool 执行器池
     * @param register     注册器
     * @param configHolder 配置源
     * @author Huang Yongxiang
     * @date 2022/8/19 15:18
     */
    public AutoJobDBTaskScheduler(AutoJobTaskExecutorPool executorPool, IAutoJobRegister register, AutoJobConfigHolder configHolder) {
        super(executorPool, register, configHolder);
        this.dbScheduleThread = ScheduleTaskUtil.build(false, "dbScheduleThread");
    }


    @Override
    public void startWork() {
        //log.warn("DB调度器已启动");
        ScheduleTaskUtil
                .build(true, "DBTaskScheduler")
                .EFixedRateTask(() -> {
                    try {
                        List<AutoJobMethodTaskEntity> taskEntities = AutoJobMapperHolder.METHOD_TASK_ENTITY_MAPPER.selectNearTask(AutoJobConstant.beforeSchedulingInQueue, TimeUnit.MILLISECONDS);
                        List<AutoJobScriptTaskEntity> scriptTaskEntities = AutoJobMapperHolder.SCRIPT_TASK_ENTITY_MAPPER.selectNearTask(AutoJobConstant.beforeSchedulingInQueue, TimeUnit.MILLISECONDS);
                        //log.warn("执行DB调度器");
                        if (AutoJobConfigHolder
                                .getInstance()
                                .isDebugEnable()) {
                            log.warn("查找到{}个DB任务", taskEntities.size() + scriptTaskEntities.size());
                        }
                        List<AutoJobTask> tasks = new ArrayList<>();
                        if (taskEntities != null && taskEntities.size() > 0) {
                            tasks.addAll(taskEntities
                                    .stream()
                                    .map(EntityConvertor::entity2Task)
                                    .collect(Collectors.toList()));
                        }
                        if (scriptTaskEntities != null && scriptTaskEntities.size() > 0) {
                            tasks.addAll(scriptTaskEntities
                                    .stream()
                                    .map(EntityConvertor::entity2Task)
                                    .collect(Collectors.toList()));
                        }
                        for (AutoJobTask task : tasks) {
                            //long near = task
                            //        .getTrigger()
                            //        .getTriggeringTime() - System.currentTimeMillis();
                            //if (near < AutoJobConstant.beforeSchedulingInTimeWheel) {
                            //    if (AutoJobConfigHolder
                            //            .getInstance()
                            //            .isDebugEnable()) {
                            //        log.warn("任务{}触发时间小于当前系统时间或者过度接近触发时间，不予放入调度队列", task.getId());
                            //    }
                            //    continue;
                            //}
                            if (task.getTrigger() != null && !task
                                    .getTrigger()
                                    .getIsPause()) {
                                try {
                                    boolean flag = register.registerTask(task);
                                    if (AutoJobConfigHolder
                                            .getInstance()
                                            .isDebugEnable()) {
                                        if (flag) {
                                            log.warn("注册DB任务：{}", task.getId());
                                        } else {
                                            log.warn("注册DB任务失败：{}", task.getId());
                                        }
                                    }
                                } catch (AutoJobRegisterRefusedException e) {
                                    log.warn("任务{}被禁止注册", task.getId());
                                    //AutoJobMapperHolder.TRIGGER_ENTITY_MAPPER.pauseTaskById(task.getId());
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, 0, AutoJobConstant.dbSchedulerRate, TimeUnit.MILLISECONDS);
    }

    @Override
    public void execute() {
        startWork();
    }

    @Override
    public void destroy() {
        dbScheduleThread.shutdownNow();
    }
}
