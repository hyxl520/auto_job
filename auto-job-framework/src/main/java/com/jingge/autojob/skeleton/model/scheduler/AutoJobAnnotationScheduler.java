package com.jingge.autojob.skeleton.model.scheduler;

import com.jingge.autojob.skeleton.annotation.*;
import com.jingge.autojob.skeleton.framework.config.AutoJobConfigHolder;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.model.executor.AutoJobTaskExecutorPool;
import com.jingge.autojob.skeleton.model.handler.*;
import com.jingge.autojob.skeleton.model.register.IAutoJobRegister;
import com.jingge.autojob.skeleton.annotation.*;
import com.jingge.autojob.skeleton.model.handler.*;
import lombok.extern.slf4j.Slf4j;

/**
 * 注解调度器
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/15 15:50
 */
@Slf4j
public class AutoJobAnnotationScheduler extends AbstractScheduler {
    public AutoJobAnnotationScheduler(AutoJobTaskExecutorPool executorPool, IAutoJobRegister register, AutoJobConfigHolder configHolder) {
        super(executorPool, register, configHolder);
    }

    private int handleAnnotationTask() {
        AbstractAnnotationTaskHandler autoJobHandler = new AutoJobAnnotationTaskHandler(new AutoJobAnnotationWrapper(), AutoJob.class);
        AbstractAnnotationTaskHandler factoryAutoJobHandler = new AutoJobAnnotationTaskHandler(new FactoryAutoJobAnnotationWrapper(), FactoryAutoJob.class);
        AbstractAnnotationTaskHandler templateAutoJobHandler = new AutoJobAnnotationTaskHandler(new TemplateAutoJobAnnotationWrapper(), TemplateAutoJob.class);
        AbstractAnnotationTaskHandler scriptAutoJobHandler = new AutoJobAnnotationTaskHandler(new ScriptAutoJobWrapper(), ScriptJob.class);
        Class<?> application = AutoJobApplication
                .getInstance()
                .getApplication();
        AutoJobScan autoJobScan = application.getAnnotation(AutoJobScan.class);
        if (autoJobScan != null) {
            return autoJobHandler.handle(autoJobScan.value()) + factoryAutoJobHandler.handle(autoJobScan.value()) + templateAutoJobHandler.handle(autoJobScan.value()) + scriptAutoJobHandler.handle(autoJobScan.value());
        }
        return autoJobHandler.handle() + factoryAutoJobHandler.handle() + templateAutoJobHandler.handle() + scriptAutoJobHandler.handle();
    }

    @Override
    public int getSchedulerLevel() {
        return Integer.MIN_VALUE;
    }

    @Override
    public void execute() {
        if (!configHolder
                .getAutoJobConfig()
                .getEnableAnnotation()) {
            return;
        }
        int count = 0;
        count += handleAnnotationTask();
        log.info("注解调度器加载{}个即将执行的任务到任务调度队列", count);
    }


}
