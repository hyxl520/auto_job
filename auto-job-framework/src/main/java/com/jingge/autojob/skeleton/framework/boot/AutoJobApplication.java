package com.jingge.autojob.skeleton.framework.boot;

import com.jingge.autojob.api.log.AutoJobLogDBAPI;
import com.jingge.autojob.api.task.AutoJobAPI;
import com.jingge.autojob.api.task.DBTaskAPI;
import com.jingge.autojob.api.task.MemoryTaskAPI;
import com.jingge.autojob.logging.model.AutoJobLogContext;
import com.jingge.autojob.logging.model.producer.AutoJobLogHelper;
import com.jingge.autojob.skeleton.annotation.ProcessorLevel;
import com.jingge.autojob.skeleton.cluster.model.AutoJobClusterManager;
import com.jingge.autojob.skeleton.cluster.model.AutoJobTaskShardingManager;
import com.jingge.autojob.skeleton.cluster.model.AutoJobTaskTransferManager;
import com.jingge.autojob.skeleton.db.DataSourceHolder;
import com.jingge.autojob.skeleton.framework.config.AutoJobConfigHolder;
import com.jingge.autojob.skeleton.framework.container.MemoryTaskContainer;
import com.jingge.autojob.skeleton.framework.mail.IMailClient;
import com.jingge.autojob.skeleton.framework.network.AutoJobNetWorkManager;
import com.jingge.autojob.skeleton.framework.processor.IAutoJobEnd;
import com.jingge.autojob.skeleton.framework.processor.IAutoJobLoader;
import com.jingge.autojob.skeleton.framework.processor.IAutoJobProcessor;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.framework.task.AutoJobContext;
import com.jingge.autojob.skeleton.model.executor.AutoJobTaskExecutorPool;
import com.jingge.autojob.skeleton.model.executor.MethodObjectCache;
import com.jingge.autojob.skeleton.model.register.IAutoJobRegister;
import com.jingge.autojob.skeleton.model.scheduler.AbstractScheduler;
import com.jingge.autojob.skeleton.model.tq.AutoJobTaskQueue;
import com.jingge.autojob.util.thread.ScheduleTaskUtil;
import com.jingge.autojob.util.thread.SyncHelper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * AutoJob程序，包含AutoJob运行的上下文，该程序为全局单例，通过{@link AutoJobBootstrap}构建该应用，构建后你可以通过{@link AutoJobApplication#getInstance()}来获取单例，如果你要使用该应用内的某个组件，请务必保证应用状态{@link AutoJobApplication#status}为RUNNING时再调用
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/13 10:41
 */
@Setter(AccessLevel.PACKAGE)
@Getter
@Slf4j
public class AutoJobApplication implements Closeable {
    String env;
    /**
     * 应用入口
     */
    private Class<?> application;
    /**
     * 内存任务容器
     */
    private MemoryTaskContainer memoryTaskContainer;
    /**
     * 任务调度队列
     */
    private AutoJobTaskQueue taskQueue;
    /**
     * 任务执行器池
     */
    private AutoJobTaskExecutorPool executorPool;
    /**
     * 任务注册器
     */
    private IAutoJobRegister register;
    /**
     * 加载器
     */
    private List<IAutoJobLoader> loaders;
    /**
     * 关闭处理器
     */
    private List<IAutoJobEnd> ends;
    /**
     * 所有的调度器
     */
    private List<AbstractScheduler> schedulers;
    /**
     * DB任务API服务类
     */
    private DBTaskAPI dbTaskAPI;
    /**
     * 内存任务API服务类
     */
    private MemoryTaskAPI memoryTaskAPI;
    /**
     * DB存储日志API
     */
    private AutoJobLogDBAPI logDbAPI;
    /**
     * 任务转移管理器
     */
    private AutoJobTaskTransferManager transferManager;
    /**
     * 任务分片管理器
     */
    private AutoJobTaskShardingManager shardingManager;
    /**
     * 集群管理器
     */
    private AutoJobClusterManager clusterManager;
    /**
     * 任务运行上下文
     */
    private AutoJobContext autoJobContext;
    /**
     * 日志上下文
     */
    private AutoJobLogContext logContext;
    /**
     * 配置源
     */
    private AutoJobConfigHolder configHolder;
    /**
     * 通信管理器
     */
    private AutoJobNetWorkManager netWorkManager;
    /**
     * 全局邮件处理器
     */
    private IMailClient mailClient;
    /**
     * 连接池
     */
    private DataSourceHolder dataSourceHolder;
    /**
     * Method对象缓存
     */
    private MethodObjectCache methodObjectCache;
    public static final int NO_CREATE = 0;
    public static final int CREATING = 1;
    public static final int CREATED = 2;
    public static final int RUNNING = 3;
    public static final int CLOSE = 4;
    private volatile int status = NO_CREATE;


    public synchronized void run() {
        if (status == NO_CREATE) {
            throw new IllegalStateException("应用还未初始化，请先初始化应用");
        }
        if (status == RUNNING) {
            return;
        }
        printLogo();
        if (configHolder.isDebugEnable()) {
            log.info("AutoJob已打开调试模式，调试日志将通过slf4j的WARN级别记录");
        }
        loaders = loaders
                .stream()
                .sorted(new ProcessorComparator())
                .collect(Collectors.toList());
        ends = ends
                .stream()
                .sorted(new ProcessorComparator())
                .collect(Collectors.toList());
        schedulers = schedulers
                .stream()
                .sorted(new SchedulerComparator())
                .collect(Collectors.toList());
        ScheduleTaskUtil
                .build(false, "AutoJobRunThread")
                .EOneTimeTask(() -> {
                    try {
                        log.info("==================================>AutoJob starting");
                        long start = System.currentTimeMillis();
                        //执行所有启动器
                        for (IAutoJobLoader loader : loaders) {
                            loader.load();
                            log.debug("加载器：{}加载完成", loader
                                    .getClass()
                                    .getName());
                        }
                        //启动所有调度器
                        for (AbstractScheduler scheduler : schedulers) {
                            scheduler.beforeExecute();
                            try {
                                scheduler.execute();
                            } catch (Exception e) {
                                scheduler.executeError(e);
                                throw e;
                            } finally {
                                scheduler.afterExecute();
                            }
                            log.debug("调度器：{}加载完成", scheduler
                                    .getClass()
                                    .getName());
                        }
                        log.info("AutoJob环境变量：{}", env);
                        log.info("AutoJob成功执行：{}个加载器，成功加载{}个调度器，共计用时：{}ms", loaders.size(), schedulers.size(), System.currentTimeMillis() - start);
                        status = RUNNING;
                        log.info("==================================>AutoJob started in {} ms", System.currentTimeMillis() - start);
                    } catch (Exception e) {
                        log.error("AutoJob start failed:{}", e.getMessage());
                        e.printStackTrace();
                    }
                    return 0;
                }, 0, TimeUnit.MILLISECONDS);
        SyncHelper.aWaitQuietly(() -> status == RUNNING);
    }

    private AutoJobApplication() {
        Thread overThread = new Thread(() -> {
            try {
                close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        overThread.setDaemon(false);
        overThread.setName("AutoJobCloseThread");
        Runtime
                .getRuntime()
                .addShutdownHook(overThread);
    }

    public boolean isCreating() {
        return status == CREATING;
    }

    public boolean isRunning() {
        return status == RUNNING;
    }

    public boolean isClosed() {
        return status == CLOSE;
    }

    public boolean isCreated() {
        return status == CREATED;
    }

    public boolean isNoCreated() {
        return status == NO_CREATE;
    }

    public static AutoJobApplication getInstance() {
        return InstanceHolder.CONTEXT;
    }

    @Override
    public void close() throws IOException {
        if (status != RUNNING) {
            throw new IllegalStateException("应用还未启动或已关闭");
        }
        try {
            //关闭时slf4j的log对象已经摧毁
            AutoJobLogHelper logger = AutoJobLogHelper.getInstance();
            logger.info("==================================>AutoJob ending");
            long start = System.currentTimeMillis();
            //执行所有的关闭处理器
            for (IAutoJobEnd end : ends) {
                end.end();
                log.debug("关闭处理器：{}执行完成", end
                        .getClass()
                        .getName());
            }
            //摧毁所有的调度器
            for (AbstractScheduler scheduler : schedulers) {
                scheduler.beforeDestroy();
                try {
                    scheduler.destroy();
                } catch (Exception e) {
                    scheduler.destroyError(e);
                    throw e;
                } finally {
                    scheduler.afterDestroy();
                }

                log.debug("调度器：{}已摧毁", scheduler
                        .getClass()
                        .getName());
            }
            logger.info("AutoJob成功执行：{}个关闭处理器，摧毁：{}个调度器，共计用时：{}ms", ends.size(), schedulers.size(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        } finally {
            status = CLOSE;
        }
    }

    /**
     * 通过任务ID获取一个匹配的API
     *
     * @param taskId 任务ID
     * @return com.example.autojob.api.task.AutoJobAPI
     * @author Huang Yongxiang
     * @date 2022/12/26 11:08
     */
    public AutoJobAPI getMatchedAPI(long taskId) {
        AutoJobTask.TaskType taskType = memoryTaskAPI.getTaskType(taskId);
        if (taskType == null) {
            return null;
        }
        return taskType == AutoJobTask.TaskType.MEMORY_TASk ? memoryTaskAPI : dbTaskAPI;
    }

    public void openDebugModel() {
        configHolder
                .getAutoJobConfig()
                .setEnableDebug(true);
    }

    private static class InstanceHolder {
        private static final AutoJobApplication CONTEXT = new AutoJobApplication();
    }

    /**
     * 处理器优先级比较器，子类应该按照使用该比较器对所有加载器有序加载
     */
    protected static class ProcessorComparator implements Comparator<IAutoJobProcessor> {
        @Override
        public int compare(IAutoJobProcessor o1, IAutoJobProcessor o2) {
            ProcessorLevel o1ProcessorLevel = o1
                    .getClass()
                    .getAnnotation(ProcessorLevel.class);
            ProcessorLevel o2ProcessorLevel = o2
                    .getClass()
                    .getAnnotation(ProcessorLevel.class);
            if (o1ProcessorLevel == null && o2ProcessorLevel != null) {
                return Integer.compare(o2ProcessorLevel.value(), 0);
            } else if (o1ProcessorLevel != null && o2ProcessorLevel == null) {
                return Integer.compare(0, o1ProcessorLevel.value());
            } else if (o1ProcessorLevel == null) {
                return 0;
            } else {
                return Integer.compare(o2ProcessorLevel.value(), o1ProcessorLevel.value());
            }
        }
    }

    public static class SchedulerComparator implements Comparator<AbstractScheduler> {

        @Override
        public int compare(AbstractScheduler o1, AbstractScheduler o2) {
            return Integer.compare(o2.getSchedulerLevel(), o1.getSchedulerLevel());
        }
    }

    private void printLogo() {
        System.out.println("   _____          __              ____.     ___.     ");
        System.out.println("  /  _  \\  __ ___/  |_  ____     |    | ____\\_ |__  ");
        System.out.println(" /  /_\\  \\|  |  \\   __\\/  _ \\    |    |/  _ \\| __ \\ ");
        System.out.println("/    |    \\  |  /|  | (  <_> )\\__|    (  <_> ) \\_\\ \\");
        System.out.println("\\____|__  /____/ |__|  \\____/\\________|\\____/|___  /");
        System.out.println("        \\/                                       \\/ ");
        System.out.println("https://gitee.com/hyxl-520/auto-job => 0.9.6");
    }

}
