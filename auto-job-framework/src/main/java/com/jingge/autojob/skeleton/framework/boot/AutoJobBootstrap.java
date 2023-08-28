package com.jingge.autojob.skeleton.framework.boot;

import com.jingge.autojob.api.log.AutoJobLogDBAPI;
import com.jingge.autojob.api.task.DBTaskAPI;
import com.jingge.autojob.api.task.MemoryTaskAPI;
import com.jingge.autojob.logging.domain.AutoJobLog;
import com.jingge.autojob.logging.domain.AutoJobRunLog;
import com.jingge.autojob.logging.model.AutoJobLogContainer;
import com.jingge.autojob.logging.model.AutoJobLogContext;
import com.jingge.autojob.logging.model.consumer.AutoJobLogConsumer;
import com.jingge.autojob.logging.model.consumer.ILogSaveStrategyDelegate;
import com.jingge.autojob.logging.model.memory.AutoJobLogCache;
import com.jingge.autojob.logging.model.memory.AutoJobRunLogCache;
import com.jingge.autojob.logging.model.producer.AutoJobLogHelper;
import com.jingge.autojob.skeleton.annotation.AutoJobProcessorScan;
import com.jingge.autojob.skeleton.cluster.model.AutoJobClusterManager;
import com.jingge.autojob.skeleton.cluster.model.AutoJobTaskShardingManager;
import com.jingge.autojob.skeleton.cluster.model.AutoJobTaskTransferManager;
import com.jingge.autojob.skeleton.db.DataSourceHolder;
import com.jingge.autojob.skeleton.framework.config.AutoJobClusterConfig;
import com.jingge.autojob.skeleton.framework.config.AutoJobConfig;
import com.jingge.autojob.skeleton.framework.config.AutoJobConfigHolder;
import com.jingge.autojob.skeleton.framework.config.AutoJobExecutorPoolConfig;
import com.jingge.autojob.skeleton.framework.container.MemoryTaskContainer;
import com.jingge.autojob.skeleton.framework.mail.MailClientFactory;
import com.jingge.autojob.skeleton.framework.mq.ExpirationListenerPolicy;
import com.jingge.autojob.skeleton.framework.mq.MessageQueueContext;
import com.jingge.autojob.skeleton.framework.network.AutoJobNetWorkManager;
import com.jingge.autojob.skeleton.framework.pool.DefaultRefuseHandler;
import com.jingge.autojob.skeleton.framework.processor.*;
import com.jingge.autojob.skeleton.framework.task.AutoJobContext;
import com.jingge.autojob.skeleton.lifecycle.TaskEventHandlerLoader;
import com.jingge.autojob.skeleton.lifecycle.listener.TaskListenerLoader;
import com.jingge.autojob.skeleton.model.alert.AlertEventHandlerLoader;
import com.jingge.autojob.skeleton.model.executor.AutoJobTaskExecutorPool;
import com.jingge.autojob.skeleton.model.executor.MethodObjectCache;
import com.jingge.autojob.skeleton.model.handler.DefaultEndProcessor;
import com.jingge.autojob.skeleton.model.register.*;
import com.jingge.autojob.skeleton.model.scheduler.*;
import com.jingge.autojob.skeleton.model.register.*;
import com.jingge.autojob.skeleton.model.scheduler.*;
import com.jingge.autojob.skeleton.model.tq.AutoJobTaskQueue;
import com.jingge.autojob.util.bean.ObjectUtil;
import com.jingge.autojob.util.convert.RegexUtil;
import com.jingge.autojob.util.thread.FlowThreadPoolExecutorHelper;
import com.jingge.autojob.util.thread.TimerThreadPoolExecutorHelper;
import com.jingge.autojob.skeleton.framework.processor.*;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * AutoJob应用启动引导类，用于构建一个全局AutoJobApplication
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/12 16:29
 */
@Slf4j
public class AutoJobBootstrap {
    /**
     * 配置源
     */
    private final AutoJobConfigHolder configHolder;
    /**
     * 运行上下文
     */
    private final AutoJobApplication runningContext;
    /**
     * 处理器扫描器
     */
    private final AutoJobProcessorScanner processorScanner;
    /**
     * 是否自动扫描处理器
     */
    private boolean isAutoScanProcessor;
    /**
     * 是否关闭内存任务调度器
     */
    private boolean isCloseMemoryScheduler;
    /**
     * 是否关闭DB任务调度器
     */
    private boolean isCloseDBScheduler;

    /**
     * 是否关闭时间轮调度器
     */
    private boolean isCloseTimeWheelScheduler;

    private ILogSaveStrategyDelegate<AutoJobLog> logSaveStrategyDelegate;

    private ILogSaveStrategyDelegate<AutoJobRunLog> runLogSaveStrategyDelegate;


    /**
     * 使用给定配置文件的输入流来创建一个引导者实例，创建后应用会自动关闭输入流
     *
     * @param applicationEntrance    应用入口
     * @param configFileInputStreams 配置文件的输入流
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/7/24 14:20
     */
    public AutoJobBootstrap(Class<?> applicationEntrance, List<InputStream> configFileInputStreams, String[] args) {
        this(new AutoJobConfigHolder(args, configFileInputStreams), applicationEntrance);
    }


    /**
     * 创建一个默认的引导者实例，默认使用类路径下的auto-job.yaml或auto-job.properties文件作为配置源
     *
     * @param applicationEntrance 应用入口
     * @author Huang Yongxiang
     * @date 2022/11/2 16:47
     */
    public AutoJobBootstrap(Class<?> applicationEntrance, String[] args) {
        this(new AutoJobConfigHolder(args, "auto-job.yml", "auto-job.properties"), applicationEntrance);
    }

    /**
     * 创建一个引导类实例
     *
     * @param applicationEntrance 应用入口
     * @param classpathResources  类路径下的配置文件名
     * @author Huang Yongxiang
     * @date 2022/11/2 16:45
     */
    public AutoJobBootstrap(Class<?> applicationEntrance, String[] args, String... classpathResources) {
        this(new AutoJobConfigHolder(args, classpathResources), applicationEntrance);
    }

    /**
     * 创建一个引导类实例，引导类对所需要的组件进行了首次初始化
     *
     * @param configHolder        配置源
     * @param applicationEntrance 应用入口，一般是main方法所在的类
     * @author Huang Yongxiang
     * @date 2022/8/12 17:34
     */
    private AutoJobBootstrap(AutoJobConfigHolder configHolder, Class<?> applicationEntrance) {
        if (configHolder == null) {
            throw new NullPointerException();
        }
        this.isAutoScanProcessor = false;
        this.configHolder = configHolder;
        AutoJobConfig config = configHolder.getAutoJobConfig();
        this.runningContext = AutoJobApplication.getInstance();
        synchronized (AutoJobBootstrap.class) {
            if (this.runningContext.isNoCreated()) {
                this.runningContext.setStatus(AutoJobApplication.CREATING);
            } else {
                throw new IllegalStateException("已经有一个线程正在创建或已创建好AutoJob应用");
            }
        }
        this.runningContext.setConfigHolder(configHolder);
        this.runningContext.setApplication(applicationEntrance);

        /*=================调度组件配置=================>*/
        this.runningContext.setMemoryTaskContainer(MemoryTaskContainer
                .builder()
                .setCleanStrategy(config.getCleanStrategy())
                .setLimitSize(config.getMemoryContainerLength())
                .build());
        this.runningContext.setTaskQueue(new AutoJobTaskQueue(config.getSchedulingQueueLength(), true));
        this.runningContext.setExecutorPool(createDefaultExecutorPool());
        this.runningContext.setRegister(new AutoJobRegister(this.runningContext.getTaskQueue()));
        /*=======================Finished======================<*/

        /*=================注解扫描器配置=================>*/
        AutoJobProcessorScan processorScan = applicationEntrance.getAnnotation(AutoJobProcessorScan.class);
        if (processorScan == null) {
            this.processorScanner = new AutoJobProcessorScanner();
        } else {
            this.processorScanner = new AutoJobProcessorScanner(processorScan.value());
        }
        /*=======================Finished======================<*/

        /*=================邮件配置=================>*/
        this.runningContext.setMailClient(MailClientFactory.createMailClient(config.getMailConfig()));
        /*=======================Finished======================<*/

        /*=================API配置=================>*/
        this.runningContext.setDbTaskAPI(new DBTaskAPI());
        this.runningContext.setMemoryTaskAPI(new MemoryTaskAPI());
        this.runningContext.setLogDbAPI(new AutoJobLogDBAPI());
        /*=======================Finished======================<*/

        this.runningContext.setSchedulers(new LinkedList<>());
        this.runningContext.setLoaders(new LinkedList<>());
        this.runningContext.setEnds(new LinkedList<>());
        this.runningContext.setMethodObjectCache(new MethodObjectCache(1000, 24, TimeUnit.HOURS));

        AutoJobClusterConfig clusterConfig = configHolder.getClusterConfig();
        this.runningContext.setNetWorkManager(new AutoJobNetWorkManager(clusterConfig));
        if (config.getEnableCluster()) {
            this.runningContext
                    .getNetWorkManager()
                    .startRPCServer();
        }
        /*=================集群配置=================>*/
        if (config.getEnableCluster()) {
            log.warn("系统已开启集群模式，将禁用注解式内存任务（不包含子任务），防止多个节点执行多个任务实例");
            AutoJobClusterManager clusterManager = new AutoJobClusterManager();
            addProcessor(clusterManager);
            this.runningContext.setClusterManager(clusterManager);
            this.runningContext.setTransferManager(new AutoJobTaskTransferManager(configHolder, clusterManager));
            this.runningContext.setShardingManager(new AutoJobTaskShardingManager(configHolder, clusterManager));
        }
        /*=======================Finished======================<*/
    }

    /**
     * 添加一个调度器，调度器需要依赖执行器池和注册器，请在调用该方法前请确认已配置，否则使用默认实现
     *
     * @param scheduler 调度器
     * @return com.example.autojob.skeleton.framework.launcher.AutoJobLauncherBuilder
     * @author Huang Yongxiang
     * @date 2022/8/13 11:19
     */
    public <T extends AbstractScheduler> AutoJobBootstrap addScheduler(Class<T> scheduler) {
        return addScheduler(createScheduler(scheduler));
    }

    public AutoJobBootstrap setEnv(String env) {
        if (RegexUtil.isMatch(env, ".*\\$\\{.*\\}.*")) {
            this.runningContext.setEnv(configHolder
                    .getPropertiesHolder()
                    .getProperty(env
                            .replaceAll("\\$\\{", "")
                            .replaceAll("\\}", "")));
        } else {
            this.runningContext.setEnv(env);
        }
        return this;
    }

    public AutoJobBootstrap setMethodObjectCache(MethodObjectCache cache) {
        this.runningContext.setMethodObjectCache(cache);
        return this;
    }

    /**
     * 额外添加一个调度器，如果你没有对应用进行拓展请忽略此方法，应用会自动将默认的调度器注册进应用
     *
     * @param scheduler 额外添加的调度器
     * @return com.example.autojob.skeleton.framework.launcher.AutoJobLauncherBuilder
     * @author Huang Yongxiang
     * @date 2022/8/15 18:15
     */
    public AutoJobBootstrap addScheduler(AbstractScheduler scheduler) {
        if (scheduler == null) {
            return this;
        }
        if (isCloseDBScheduler && scheduler instanceof AutoJobDBTaskScheduler) {
            log.warn("DB任务调度器已被关闭！");
            return this;
        }
        if (isCloseMemoryScheduler && scheduler instanceof AutoJobMemoryTaskScheduler) {
            log.warn("Memory任务调度器已被关闭！");
            return this;
        }
        if (isCloseTimeWheelScheduler && scheduler instanceof AutoJobTimeWheelScheduler) {
            log.warn("时间轮调度器已被关闭！");
            return this;
        }
        if (scheduler instanceof IAutoJobProcessor) {
            this.addProcessor((IAutoJobProcessor) scheduler);
        }
        this.runningContext
                .getSchedulers()
                .add(scheduler);
        return this;
    }

    /**
     * 额外添加一个处理器，果你没有对应用进行拓展请忽略此方法，应用会自动将默认的处理器注册进应用
     *
     * @param processor 要添加的默认处理器
     * @return com.example.autojob.skeleton.framework.launcher.AutoJobLauncherBuilder
     * @author Huang Yongxiang
     * @date 2022/8/16 9:10
     */
    public AutoJobBootstrap addProcessor(IAutoJobProcessor processor) {
        if (processor == null) {
            return this;
        }
        if (AutoJobProcessorContext
                .getInstance()
                .containsProcessor(processor.getClass())) {
            return this;
        }
        if (processor instanceof IAutoJobLoader) {
            this.runningContext
                    .getLoaders()
                    .add((IAutoJobLoader) processor);
        }
        if (processor instanceof IAutoJobEnd) {
            this.runningContext
                    .getEnds()
                    .add((IAutoJobEnd) processor);
        }
        AutoJobProcessorContext
                .getInstance()
                .addProcessor(processor);
        return this;
    }

    /**
     * 使用动态处理器扫描，扫描到的处理器会调用其无参构造方法创建处理器，该方法仅会扫描非默认的处理器
     *
     * @return com.example.autojob.skeleton.framework.launcher.AutoJobLauncherBuilder
     * @author Huang Yongxiang
     * @date 2022/8/16 9:12
     */
    public AutoJobBootstrap withAutoScanProcessor() {
        isAutoScanProcessor = true;
        return this;
    }

    /**
     * 设置一个连接池，默认使用Druid连接池
     *
     * @param dataSource 连接池
     * @return com.example.autojob.skeleton.framework.launcher.AutoJobLauncherBuilder
     * @author Huang Yongxiang
     * @date 2022/8/25 15:38
     */
    public AutoJobBootstrap setDataSource(DataSource dataSource) {
        if (dataSource == null) {
            throw new NullPointerException();
        }
        this.runningContext.setDataSourceHolder(new DataSourceHolder(dataSource));
        return this;
    }

    /**
     * 设置注册器
     *
     * @param register 注册器
     * @return com.example.autojob.skeleton.framework.launcher.AutoJobLauncherBuilder
     * @author Huang Yongxiang
     * @date 2022/8/16 9:13
     */
    public AutoJobBootstrap setRegister(IAutoJobRegister register) {
        if (register == null) {
            throw new NullPointerException();
        }
        this.runningContext.setRegister(register);
        return this;
    }

    /**
     * 设置注册器
     *
     * @param register 注册器
     * @param handler  注册处理器
     * @param filter   注册过滤器
     * @return com.example.autojob.skeleton.framework.launcher.AutoJobLauncherBuilder
     * @author Huang Yongxiang
     * @date 2022/8/16 9:13
     */
    public AutoJobBootstrap setRegister(IAutoJobRegister register, AbstractRegisterHandler handler, AbstractRegisterFilter filter) {
        if (register == null) {
            throw new NullPointerException();
        }
        register.setFilter(filter);
        register.setHandler(handler);
        this.runningContext.setRegister(register);
        return this;
    }

    /**
     * 设置执行池，默认使用的是根据流量调整的动态线程池，为了适应业务，你可以考虑使用基于时间调整的动态线程池，参照{@link TimerThreadPoolExecutorHelper}
     *
     * @param executorPool 执行池
     * @return com.example.autojob.skeleton.framework.launcher.AutoJobBootstrap
     * @author Huang Yongxiang
     * @date 2022/12/7 16:03
     */
    public AutoJobBootstrap setExecutorPool(AutoJobTaskExecutorPool executorPool) {
        if (executorPool == null) {
            throw new NullPointerException();
        }
        this.runningContext.setExecutorPool(executorPool);
        return this;
    }

    public AutoJobBootstrap setTaskQueue(AutoJobTaskQueue taskQueue) {
        if (taskQueue == null) {
            throw new NullPointerException();
        }
        this.runningContext.setTaskQueue(taskQueue);
        return this;
    }

    /**
     * 关闭内存任务调度器
     *
     * @return com.example.autojob.skeleton.framework.launcher.AutoJobLauncherBuilder
     * @author Huang Yongxiang
     * @date 2022/11/2 16:32
     */
    public AutoJobBootstrap closeMemoryTaskScheduler() {
        isCloseMemoryScheduler = true;
        return this;
    }

    /**
     * 关闭DB任务调度器
     *
     * @return com.example.autojob.skeleton.framework.launcher.AutoJobLauncherBuilder
     * @author Huang Yongxiang
     * @date 2022/11/2 16:33
     */
    public AutoJobBootstrap closeDBTaskScheduler() {
        isCloseDBScheduler = true;
        return this;
    }

    public AutoJobBootstrap closeTimeWheelScheduler() {
        isCloseTimeWheelScheduler = true;
        return this;
    }

    protected void createLogContext() {

        /*=================添加日志消息队列=================>*/
        AutoJobConfig config = configHolder.getAutoJobConfig();
        AutoJobLogContainer
                .getInstance()
                .addMessageQueueContext(AutoJobLog.class, MessageQueueContext
                        .builder()
                        .setListenerPolicy(ExpirationListenerPolicy.SINGLE_THREAD)
                        .setAllowSetEntryExpired(true)
                        .setDefaultExpiringTime(24, TimeUnit.HOURS)
                        .setAllowMaxTopicCount(config
                                .getExecutorPoolConfig()
                                .getFastPoolMaxThreadCount() + config
                                .getExecutorPoolConfig()
                                .getSlowPoolMaxCoreThreadCount())
                        .setAllowMaxMessageCountPerQueue(Integer.MAX_VALUE)
                        .build());
        /*=======================Finished======================<*/

        /*=================创建日志上下文=================>*/
        AutoJobLogContext
                .getInstance()
                .setLogContainer(AutoJobLogContainer.getInstance())
                .setLogManager(new AutoJobLogConsumer(logSaveStrategyDelegate, runLogSaveStrategyDelegate, configHolder.getLogConfig()))
                .setLogCache(new AutoJobLogCache(configHolder.getLogConfig()))
                .setRunLogCache(new AutoJobRunLogCache(configHolder.getLogConfig()));
        runningContext.setLogContext(AutoJobLogContext.getInstance());
        /*=======================Finished======================<*/
    }

    protected void createDefaultScheduler() {
        this
                .addScheduler(createScheduler(AutoJobAnnotationScheduler.class))
                .addScheduler(createScheduler(AutoJobRunSuccessScheduler.class))
                .addScheduler(createScheduler(AutoJobRunErrorScheduler.class))
                .addScheduler(createScheduler(AutoJobDBTaskScheduler.class))
                .addScheduler(createScheduler(AutoJobTimeWheelScheduler.class))
                .addScheduler(createScheduler(AutoJobMemoryTaskScheduler.class));
        AutoJobContext context = new AutoJobContext(runningContext.getExecutorPool(), runningContext.getRegister(), runningContext.getConfigHolder(), new DBOptimisticLock());
        runningContext.setAutoJobContext(context);
        this.addScheduler(context);
    }

    protected void createDefaultProcessor() {
        this
                .addProcessor(new AlertEventHandlerLoader())
                .addProcessor(new TaskEventHandlerLoader())
                .addProcessor(new TaskListenerLoader())
                .addProcessor(new AutoJobRegisterLoader())
                .addProcessor(new DefaultEndProcessor());
    }

    protected void createDefaultDataSource() {
        if (this.runningContext.getDataSourceHolder() == null) {
            this.runningContext.setDataSourceHolder(new DataSourceHolder());
        }
    }

    public AutoJobBootstrap setLogSaveStrategyDelegate(ILogSaveStrategyDelegate<AutoJobLog> logSaveStrategyDelegate) {
        this.logSaveStrategyDelegate = logSaveStrategyDelegate;
        return this;
    }

    public AutoJobBootstrap setRunLogSaveStrategyDelegate(ILogSaveStrategyDelegate<AutoJobRunLog> runLogSaveStrategyDelegate) {
        this.runLogSaveStrategyDelegate = runLogSaveStrategyDelegate;
        return this;
    }

    /**
     * 创建一个默认的执行器池实现，具体参数来源于配置源
     *
     * @return com.example.autojob.skeleton.model.executor.AutoJobTaskExecutorPool
     * @author Huang Yongxiang
     * @date 2022/8/25 15:41
     */
    protected AutoJobTaskExecutorPool createDefaultExecutorPool() {
        AutoJobExecutorPoolConfig config = configHolder
                .getAutoJobConfig()
                .getExecutorPoolConfig();
        /*=================fast pool自动装配=================>*/
        FlowThreadPoolExecutorHelper fastPool = FlowThreadPoolExecutorHelper
                .classicBuilder()
                .setAllowUpdate(config.getEnableFastPoolUpdate())
                .setAllowMaxCoreThreadCount(config.getFastPoolMaxCoreThreadCount())
                .setAllowMinCoreThreadCount(config.getFastPoolMinCoreThreadCount())
                .setAllowMaxResponseTime(config.getFastPoolAllowMaximumResponseTime())
                .setCoreThreadCount(config.getFastPoolInitialCoreThreadCount())
                .setMaxThreadCount(config.getFastPoolInitialThreadCount())
                .setAllowMaxThreadCount(config.getFastPoolMaxThreadCount())
                .setQueueLength(config.getFastPoolQueueLength())
                .setAllowMinThreadCount(config.getFastPoolMinThreadCount())
                .setTrafficListenerCycle((config.getFastPoolTrafficUpdateCycle()).longValue())
                .setUpdateType(FlowThreadPoolExecutorHelper.UpdateType.USE_FLOW)
                .setUpdateThreshold(config.getFastPoolAdjustedThreshold())
                .build();
        /*=======================Finished======================<*/

        /*=================end pool自动装配=================>*/
        FlowThreadPoolExecutorHelper slowPool = FlowThreadPoolExecutorHelper
                .classicBuilder()
                .setAllowUpdate(config.getEnableSlowPoolUpdate())
                .setAllowMaxCoreThreadCount(config.getSlowPoolMaxCoreThreadCount())
                .setAllowMinCoreThreadCount(config.getSlowPoolMinCoreThreadCount())
                .setAllowMaxResponseTime(config.getSlowPoolAllowMaximumResponseTime())
                .setCoreThreadCount(config.getSlowPoolInitialCoreThreadCount())
                .setMaxThreadCount(config.getSlowPoolInitialThreadCount())
                .setQueueLength(config.getSlowPoolQueueLength())
                .setAllowMaxThreadCount(config.getSlowPoolMaxThreadCount())
                .setAllowMinThreadCount(config.getSlowPoolMinThreadCount())
                .setTrafficListenerCycle((config.getSlowPoolTrafficUpdateCycle()).longValue())
                .setUpdateType(FlowThreadPoolExecutorHelper.UpdateType.USE_FLOW)
                .setUpdateThreshold(config.getSlowPoolAdjustedThreshold())
                .build();
        /*=======================Finished======================<*/
        return new AutoJobTaskExecutorPool(new DefaultRefuseHandler(), fastPool, slowPool);
    }

    /**
     * 如果设置了处理器自动扫描，该方法会自动扫描处理器相关的类并且调用无参构造方法创建对象
     *
     * @return void
     * @author Huang Yongxiang
     * @date 2022/8/15 18:11
     */
    protected void scanProcessor() {
        if (isAutoScanProcessor) {
            Set<Class<? extends IAutoJobProcessor>> processors = processorScanner.scanClass();
            for (Class<? extends IAutoJobProcessor> clazz : processors) {
                if (clazz == AutoJobClusterManager.class) {
                    continue;
                }
                IAutoJobProcessor processor = ObjectUtil.getClassInstance(clazz);
                if (processor != null) {
                    this.addProcessor(processor);
                }
            }
            log.debug("自动扫描到：{}个处理器", processors.size());
        }
    }

    /**
     * 创建一个应用实例，注意该实例是全局单例
     *
     * @return com.example.autojob.skeleton.framework.launcher.AutoJobApplication
     * @author Huang Yongxiang
     * @date 2022/8/17 15:55
     */
    public AutoJobApplication build() {
        this.createDefaultScheduler();
        this.createDefaultProcessor();
        this.scanProcessor();
        this.createDefaultDataSource();
        this.createLogContext();
        this.runningContext.setStatus(AutoJobApplication.CREATED);
        return runningContext;
    }

    protected <T extends AbstractScheduler> T createScheduler(Class<T> clazz) {
        try {
            Constructor<T> constructor = clazz.getConstructor(AutoJobTaskExecutorPool.class, IAutoJobRegister.class, AutoJobConfigHolder.class);
            return constructor.newInstance(this.runningContext.getExecutorPool(), this.runningContext.getRegister(), configHolder);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
