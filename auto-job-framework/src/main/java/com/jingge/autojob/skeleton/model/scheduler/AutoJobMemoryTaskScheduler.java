package com.jingge.autojob.skeleton.model.scheduler;

import com.jingge.autojob.skeleton.framework.config.AutoJobConfigHolder;
import com.jingge.autojob.skeleton.framework.config.AutoJobConstant;
import com.jingge.autojob.skeleton.framework.container.MemoryTaskContainer;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.model.executor.AutoJobTaskExecutorPool;
import com.jingge.autojob.skeleton.model.register.AutoJobRegisterRefusedException;
import com.jingge.autojob.skeleton.model.register.IAutoJobRegister;
import com.jingge.autojob.util.thread.ScheduleTaskUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author Huang Yongxiang
 * @Date 2022/10/14 15:55
 */
@Slf4j
public class AutoJobMemoryTaskScheduler extends AbstractScheduler {
    private ScheduleTaskUtil scheduleTaskUtil;

    /**
     * 调度器的通用构造方法，框架自动注册调度器时会执行该构造方法
     *
     * @param executorPool 执行器池
     * @param register     注册器
     * @param configHolder 配置源
     * @author Huang Yongxiang
     * @date 2022/8/19 15:18
     */
    public AutoJobMemoryTaskScheduler(AutoJobTaskExecutorPool executorPool, IAutoJobRegister register, AutoJobConfigHolder configHolder) {
        super(executorPool, register, configHolder);
    }

    @Override
    public void execute() {
        log.debug("内存任务调度器启动");
        MemoryTaskContainer memoryTaskContainer = AutoJobApplication
                .getInstance()
                .getMemoryTaskContainer();
        scheduleTaskUtil = ScheduleTaskUtil.build(true, "memoryTaskScheduler");
        scheduleTaskUtil.EFixedRateTask(() -> {
            try {
                List<AutoJobTask> tasks = memoryTaskContainer.getFutureRun(AutoJobConstant.beforeSchedulingInQueue, TimeUnit.MILLISECONDS);
                //if(AutoJobConfigHolder.getInstance()
                //                      .isDebugEnable()){
                //    log.warn("查找到{}个内存任务", tasks.size());
                //}
                if (tasks.size() > 0) {
                    for (AutoJobTask task : tasks) {
                        AutoJobTask newOne = AutoJobTask.deepCopyFrom(task);
                        if (newOne.getTrigger() != null && !newOne
                                .getTrigger()
                                .getIsPause()) {
                            try {
                                boolean flag = register.registerTask(newOne);
                                if (AutoJobConfigHolder
                                        .getInstance()
                                        .isDebugEnable()) {
                                    if (flag) {
                                        log.warn("注册Memory任务：{}", newOne.getId());
                                    } else {
                                        log.warn("注册Memory任务失败：{}", newOne.getId());
                                    }
                                }
                            } catch (AutoJobRegisterRefusedException e) {
                                log.warn("任务{}被禁止注册", task.getId());
                                //memoryTaskContainer.removeById(newOne.getId());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, AutoJobConstant.memorySchedulerRate, TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy() {
        scheduleTaskUtil.shutdown();
    }

}
