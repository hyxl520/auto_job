package com.jingge.autojob.skeleton.model.builder;

import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;
import com.jingge.autojob.skeleton.enumerate.SaveStrategy;
import com.jingge.autojob.skeleton.enumerate.SchedulingStrategy;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.config.AutoJobMailConfig;
import com.jingge.autojob.skeleton.framework.config.AutoJobRetryConfig;
import com.jingge.autojob.skeleton.framework.config.AutoJobShardingConfig;
import com.jingge.autojob.skeleton.framework.config.TimeConstant;
import com.jingge.autojob.skeleton.framework.mail.IMailClient;
import com.jingge.autojob.skeleton.framework.mail.MailClientFactory;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.framework.task.AutoJobTrigger;
import com.jingge.autojob.skeleton.framework.task.NumericalShardingStrategy;
import com.jingge.autojob.skeleton.framework.task.ShardingStrategy;
import com.jingge.autojob.skeleton.model.executor.DefaultMethodObjectFactory;
import com.jingge.autojob.skeleton.model.executor.IMethodObjectFactory;
import com.jingge.autojob.skeleton.model.interpreter.AutoJobAttributeContext;
import com.jingge.autojob.skeleton.model.task.method.MethodTask;
import com.jingge.autojob.util.convert.DefaultValueUtil;
import com.jingge.autojob.util.convert.StringUtils;
import com.jingge.autojob.util.id.IdGenerator;
import com.jingge.autojob.util.servlet.InetUtil;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 任务构建类，构建一个完整的方法型任务
 *
 * @Author Huang Yongxiang
 * @Date 2022/10/27 9:28
 * @Email 1158055613@qq.com
 */
public class AutoJobMethodTaskBuilder {
    private final AttributesBuilder ATTRIBUTES_BUILDER = new AttributesBuilder();
    /**
     * 任务ID，该ID将会作为调度的唯一键
     */
    private long taskId;

    private Long versionID;

    /**
     * 任务所在的类
     */
    private final Class<?> taskClass;

    /**
     * 任务方法名
     */
    private final String methodName;

    /**
     * 参数字符串
     */
    private String paramsString;

    /**
     * 任务类型，默认是内存型任务
     */
    private AutoJobTask.TaskType taskType;

    /**
     * 保存策略
     */
    private SaveStrategy saveStrategy;

    /**
     * 任务别名
     */
    private String taskAlias;

    /**
     * 任务级别
     */
    private int taskLevel;

    /**
     * 最长运行时长
     */
    private long maximumExecutionTime;

    /**
     * 分片策略
     */
    private ShardingStrategy shardingStrategy;

    /**
     * 分片配置
     */
    private AutoJobShardingConfig shardingConfig;

    /**
     * 任务方法类构建工厂
     */
    private IMethodObjectFactory methodObjectFactory;

    /**
     * 触发器
     */
    private AutoJobTrigger trigger;

    /**
     * 调度策略
     */
    private SchedulingStrategy schedulingStrategy;

    /**
     * 重试配置
     */
    private AutoJobRetryConfig retryConfig;

    /**
     * 邮件配置
     */
    private AutoJobMailConfig mailConfig;

    private IMailClient mailClient;

    /**
     * 所属
     */
    private Long belongTo;

    private Object[] params;

    private boolean isChildTask;

    private final List<String> executableMachines = new LinkedList<>();

    /**
     * 是否自动保存
     */
    private boolean isSave;

    public AutoJobMethodTaskBuilder(Class<?> taskClass, String methodName) {
        this.taskClass = taskClass;
        this.methodName = methodName;
        this.taskId = IdGenerator.getNextIdAsLong();
        this.taskLevel = 0;
        saveStrategy = SaveStrategy.SAVE_IF_ABSENT;
        this.taskAlias = String.format("%s.%s", taskClass.getName(), methodName);
        this.taskType = AutoJobTask.TaskType.MEMORY_TASk;
        this.methodObjectFactory = new DefaultMethodObjectFactory();
        this.maximumExecutionTime = TimeConstant.A_DAY;
    }

    public AutoJobMethodTaskBuilder(Method method) {
        this(method.getDeclaringClass(), method.getName());
    }

    public AutoJobMethodTaskBuilder setTaskId(long taskId) {
        this.taskId = taskId;
        return this;
    }

    public AutoJobMethodTaskBuilder setTaskType(AutoJobTask.TaskType taskType) {
        this.taskType = taskType;
        return this;
    }

    public AutoJobMethodTaskBuilder setTaskAlias(String taskAlias) {
        this.taskAlias = taskAlias;
        return this;
    }

    public AutoJobMethodTaskBuilder setTaskLevel(int taskLevel) {
        this.taskLevel = taskLevel;
        return this;
    }

    public AutoJobMethodTaskBuilder setMaximumExecutionTime(long maximumExecutionTime, TimeUnit unit) {
        this.maximumExecutionTime = unit.toMillis(maximumExecutionTime);
        return this;
    }

    public AutoJobMethodTaskBuilder setMethodObjectFactory(IMethodObjectFactory methodObjectFactory) {
        this.methodObjectFactory = methodObjectFactory;
        return this;
    }

    public AutoJobMethodTaskBuilder setRetryConfig(AutoJobRetryConfig retryConfig) {
        this.retryConfig = retryConfig;
        return this;
    }

    public AutoJobMethodTaskBuilder setSchedulingStrategy(SchedulingStrategy schedulingStrategy) {
        this.schedulingStrategy = schedulingStrategy;
        return this;
    }

    public AutoJobMethodTaskBuilder setSaveStrategy(SaveStrategy saveStrategy) {
        this.saveStrategy = saveStrategy;
        return this;
    }

    public AutoJobMethodTaskBuilder addExecutableMachine(String machineAddress) {
        if ("localhost".equalsIgnoreCase(machineAddress.trim()) || "local".equalsIgnoreCase(machineAddress.trim())) {
            executableMachines.add(InetUtil.getLocalhostIp());
        } else {
            executableMachines.add(machineAddress);
        }
        return this;
    }

    public AutoJobMethodTaskBuilder addAllExecutableMachine(List<String> machineAddress) {
        for (String s : machineAddress) {
            if ("localhost".equalsIgnoreCase(s.trim()) || "local".equalsIgnoreCase(s.trim())) {
                executableMachines.add(InetUtil.getLocalhostIp());
            } else {
                executableMachines.add(s);
            }
        }
        return this;
    }

    public AutoJobMethodTaskBuilder setBelongTo(Long belongTo) {
        this.belongTo = belongTo;
        return this;
    }

    public AutoJobMethodTaskBuilder isSave(boolean save) {
        isSave = save;
        return this;
    }

    public AutoJobMethodTaskBuilder setShardingStrategy(ShardingStrategy shardingStrategy) {
        this.shardingStrategy = shardingStrategy;
        return this;
    }

    public AutoJobMethodTaskBuilder setShardingConfig(boolean enable, Object total) {
        this.shardingConfig = new AutoJobShardingConfig(null, enable, total);
        return this;
    }

    public AutoJobMethodTaskBuilder setShardingConfig(AutoJobShardingConfig shardingConfig) {
        this.shardingConfig = shardingConfig;
        return this;
    }

    public AutoJobMethodTaskBuilder setMailConfig(AutoJobMailConfig mailConfig) {
        this.mailConfig = mailConfig;
        if (mailConfig != null) {
            mailClient = MailClientFactory.createMailClient(mailConfig);
        }
        return this;
    }

    public AutoJobMethodTaskBuilder setVersionID(Long versionID) {
        this.versionID = versionID;
        return this;
    }

    public AutoJobMethodTaskBuilder setChildTask(boolean childTask) {
        isChildTask = childTask;
        return this;
    }

    /**
     * 添加一个任务参数，参数类型为枚举中的类型，注意参数顺序将会按照添加顺序注入
     *
     * @param type  参数类型
     * @param value 参数值
     * @return com.example.autojob.skeleton.model.builder.AutoJobMethodTaskBuilder
     * @author Huang Yongxiang
     * @date 2022/10/27 9:53
     */
    public AutoJobMethodTaskBuilder addParam(AttributesBuilder.AttributesType type, Object value) {
        ATTRIBUTES_BUILDER.addParams(type, value);
        return this;
    }

    /**
     * 添加一个参数，该参数类型为可被Json序列化/反序列化的对象类型，注意参数顺序将会按照添加顺序注入
     *
     * @param paramType 参数类型
     * @param value     参数值
     * @return com.example.autojob.skeleton.model.builder.AutoJobMethodTaskBuilder
     * @author Huang Yongxiang
     * @date 2022/10/27 9:54
     */
    public AutoJobMethodTaskBuilder addParam(Class<?> paramType, Object value) {
        ATTRIBUTES_BUILDER.addParams(paramType, value);
        return this;
    }

    public AutoJobMethodTaskBuilder addParams(Object[] params) {
        for (Object param : params) {
            if (param == null) {
                continue;
            }
            addParam(param.getClass(), param);
        }
        return this;
    }

    public AutoJobMethodTaskBuilder setParams(String paramsString) {
        if (StringUtils.isEmpty(paramsString)) {
            return this;
        }
        AutoJobAttributeContext context = new AutoJobAttributeContext(paramsString);
        this.params = context.getAttributeEntity();
        this.paramsString = paramsString;
        return this;
    }

    /**
     * 添加一个简单触发器，添加多个触发器时前者将被后者覆盖
     *
     * @param firstTriggeringTime 首次触发时间
     * @param repeatTimes         重复次数，任务总触发次数=1+repeatTimes，-1表示无限次触发
     * @param cycle               周期
     * @param unit                时间单位
     * @return com.example.autojob.skeleton.model.builder.AutoJobMethodTaskBuilder
     * @author Huang Yongxiang
     * @date 2022/10/27 9:59
     */
    public AutoJobMethodTaskBuilder addASimpleTrigger(long firstTriggeringTime, int repeatTimes, long cycle, TimeUnit unit) {
        this.trigger = AutoJobTriggerFactory.newSimpleTrigger(firstTriggeringTime, repeatTimes, cycle, unit);
        return this;
    }

    /**
     * 添加一个cron-like表达式的触发器，添加多个触发器时前者将被后者覆盖
     *
     * @param cronExpression cron-like表达式
     * @param repeatTimes    重复次数，任务总触发次数=1+repeatTimes，-1表示无限次触发
     * @return com.example.autojob.skeleton.model.builder.AutoJobMethodTaskBuilder
     * @author Huang Yongxiang
     * @date 2022/10/27 10:03
     */
    public AutoJobMethodTaskBuilder addACronExpressionTrigger(String cronExpression, int repeatTimes) {
        this.trigger = AutoJobTriggerFactory.newCronExpressionTrigger(cronExpression, repeatTimes);
        return this;
    }

    /**
     * 添加一个子任务触发器，该任务将会作为一个子任务参与调度，添加多个触发器时前者将被后者覆盖
     *
     * @return com.example.autojob.skeleton.model.builder.AutoJobMethodTaskBuilder
     * @author Huang Yongxiang
     * @date 2022/10/27 10:09
     */
    public AutoJobMethodTaskBuilder addAChildTaskTrigger() {
        this.trigger = AutoJobTriggerFactory.newChildTrigger();
        isChildTask = true;
        return this;
    }

    /**
     * 添加一个延迟触发器，任务将会在给定延迟后触发一次，添加多个触发器时前者将被后者覆盖
     *
     * @param delay 距离现在延迟执行的时间
     * @param unit  时间单位
     * @return com.example.autojob.skeleton.model.builder.AutoJobMethodTaskBuilder
     * @author Huang Yongxiang
     * @date 2022/10/27 10:12
     */
    public AutoJobMethodTaskBuilder addADelayTrigger(long delay, TimeUnit unit) {
        this.trigger = AutoJobTriggerFactory.newDelayTrigger(delay, unit);
        return this;
    }

    public AutoJobMethodTaskBuilder setTrigger(AutoJobTrigger trigger) {
        this.trigger = trigger;
        return this;
    }

    public MethodTask build() {
        MethodTask methodTask = new MethodTask();
        methodTask.setId(taskId);
        methodTask.setBelongTo(belongTo);
        methodTask.setAlias(taskAlias);
        methodTask.setTrigger(trigger);
        if (trigger != null) {
            trigger.setTaskId(taskId);
        }
        methodTask.setIsChildTask(isChildTask);
        methodTask.setMailConfig(mailConfig);
        if (mailConfig != null && mailConfig.getTaskId() == null) {
            mailConfig.setTaskId(taskId);
        }
        //methodTask.setRetryConfig(DefaultValueUtil.defaultValue(retryConfig, new AutoJobRetryConfig().setTaskId(methodTask.getId())));
        //methodTask
        //        .getRetryConfig()
        //        .setTaskId(taskId);
        methodTask.setMailClient(DefaultValueUtil.defaultValue(mailClient, AutoJobApplication
                .getInstance()
                .getMailClient()));
        if (shardingConfig != null) {
            if (shardingConfig.getTaskId() == null) {
                shardingConfig.setTaskID(taskId);
            }
            methodTask.setShardingStrategy(DefaultValueUtil.defaultValue(shardingStrategy, new NumericalShardingStrategy()));
            methodTask.setShardingConfig(shardingConfig);
        }
        if (retryConfig != null && retryConfig.getTaskId() == null) {
            retryConfig.setTaskId(taskId);
        }
        methodTask.setSaveStrategy(saveStrategy);
        methodTask.setRetryConfig(retryConfig);
        methodTask.setVersionId(versionID);
        methodTask.setSchedulingStrategy(schedulingStrategy);
        methodTask.setParamsString(paramsString);
        methodTask.setExecutableMachines(executableMachines);
        methodTask.setMethodObjectFactory(methodObjectFactory);
        if (StringUtils.isEmpty(methodTask.getParamsString())) {
            methodTask.setParamsString(ATTRIBUTES_BUILDER.getAttributesString());
        }
        methodTask.setType(taskType);
        methodTask.setMethodName(methodName);
        methodTask.setMethodClass(taskClass);
        //methodTask.setAnnotationId(IdGenerator.getNextIdAsLong());
        if (methodTask.getTrigger() != null && methodTask
                .getTrigger()
                .getMaximumExecutionTime() == null) {
            methodTask
                    .getTrigger()
                    .setMaximumExecutionTime(maximumExecutionTime);
        }
        methodTask.setTaskLevel(taskLevel);
        methodTask.setMethodClassName(taskClass.getName());
        if (params == null) {
            AutoJobAttributeContext context = new AutoJobAttributeContext(methodTask);
            methodTask.setParams(context.getAttributeEntity());
        } else {
            methodTask.setParams(this.params);
        }
        if (isSave) {
            methodTask.save();
        }
        return methodTask;
    }
}
