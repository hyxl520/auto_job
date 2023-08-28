package com.jingge.autojob.skeleton.model.builder;

import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;
import com.jingge.autojob.skeleton.enumerate.SaveStrategy;
import com.jingge.autojob.skeleton.enumerate.SchedulingStrategy;
import com.jingge.autojob.skeleton.enumerate.ScriptType;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.config.AutoJobMailConfig;
import com.jingge.autojob.skeleton.framework.config.AutoJobRetryConfig;
import com.jingge.autojob.skeleton.framework.config.AutoJobShardingConfig;
import com.jingge.autojob.skeleton.framework.mail.IMailClient;
import com.jingge.autojob.skeleton.framework.mail.MailClientFactory;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.framework.task.AutoJobTrigger;
import com.jingge.autojob.skeleton.framework.task.NumericalShardingStrategy;
import com.jingge.autojob.skeleton.framework.task.ShardingStrategy;
import com.jingge.autojob.skeleton.model.executor.IMethodObjectFactory;
import com.jingge.autojob.skeleton.model.task.script.ScriptTask;
import com.jingge.autojob.util.convert.DefaultValueUtil;
import com.jingge.autojob.util.id.IdGenerator;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 脚本型任务构建类，构建一个完整的脚本型任务
 *
 * @Author Huang Yongxiang
 * @Date 2022/10/29 16:59
 * @Email 1158055613@qq.com
 */
public class AutoJobScriptTaskBuilder {
    /**
     * 任务ID，该ID将会作为调度的唯一键
     */
    private long taskId;
    private Long versionID;
    /**
     * 任务类型，默认是内存型任务
     */
    private AutoJobTask.TaskType taskType;

    /**
     * 任务别名
     */
    private String taskAlias;

    /**
     * 任务级别
     */
    private int taskLevel;
    /**
     * 触发器
     */
    private AutoJobTrigger trigger;

    /**
     * 所属
     */
    private Long belongTo;
    /**
     * 分片策略
     */
    private ShardingStrategy shardingStrategy;

    /**
     * 分片配置
     */
    private AutoJobShardingConfig shardingConfig;

    /**
     * 保存策略
     */
    private SaveStrategy saveStrategy;

    /**
     * 调度策略
     */
    private SchedulingStrategy schedulingStrategy;

    /**
     * 是否是子任务
     */
    private boolean isChildTask;

    /**
     * 重试配置
     */
    private AutoJobRetryConfig retryConfig;

    /**
     * 邮件配置
     */
    private AutoJobMailConfig mailConfig;

    private Object[] params;

    /**
     * 是否自动保存
     */
    private boolean isSave;

    private IMailClient mailClient;

    private final List<String> executableMachines = new LinkedList<>();

    public AutoJobScriptTaskBuilder() {
        this(IdGenerator.getNextIdAsLong());
    }

    public AutoJobScriptTaskBuilder(long taskId) {
        this.taskId = taskId;
        trigger = AutoJobTriggerFactory.newDelayTrigger((long) (AutoJobApplication
                .getInstance()
                .getConfigHolder()
                .getAutoJobConfig()
                .getAnnotationDefaultDelayTime() * 60 * 1000), TimeUnit.MILLISECONDS);
        taskType = AutoJobTask.TaskType.MEMORY_TASk;
        saveStrategy = SaveStrategy.SAVE_IF_ABSENT;
    }

    public AutoJobScriptTaskBuilder setTaskId(long taskId) {
        this.taskId = taskId;
        return this;
    }

    public AutoJobScriptTaskBuilder setTaskType(AutoJobTask.TaskType taskType) {
        this.taskType = taskType;
        return this;
    }

    public AutoJobScriptTaskBuilder setTaskAlias(String taskAlias) {
        this.taskAlias = taskAlias;
        return this;
    }

    public AutoJobScriptTaskBuilder setSchedulingStrategy(SchedulingStrategy schedulingStrategy) {
        this.schedulingStrategy = schedulingStrategy;
        return this;
    }

    public AutoJobScriptTaskBuilder setTaskLevel(int taskLevel) {
        this.taskLevel = taskLevel;
        return this;
    }

    public AutoJobScriptTaskBuilder setBelongTo(Long belongTo) {
        this.belongTo = belongTo;
        return this;
    }

    public AutoJobScriptTaskBuilder setShardingStrategy(ShardingStrategy shardingStrategy) {
        this.shardingStrategy = shardingStrategy;
        return this;
    }

    public AutoJobScriptTaskBuilder setShardingConfig(boolean enable, Object total) {
        this.shardingConfig = new AutoJobShardingConfig(null, enable, total);
        return this;
    }

    public AutoJobScriptTaskBuilder setShardingConfig(AutoJobShardingConfig shardingConfig) {
        this.shardingConfig = shardingConfig;
        return this;
    }

    public AutoJobScriptTaskBuilder setRetryConfig(AutoJobRetryConfig retryConfig) {
        this.retryConfig = retryConfig;
        return this;
    }

    public AutoJobScriptTaskBuilder setChildTask(boolean childTask) {
        isChildTask = childTask;
        return this;
    }

    public AutoJobScriptTaskBuilder setSaveStrategy(SaveStrategy saveStrategy) {
        this.saveStrategy = saveStrategy;
        return this;
    }

    public AutoJobScriptTaskBuilder setSave(boolean save) {
        isSave = save;
        return this;
    }

    public AutoJobScriptTaskBuilder setParams(String... params) {
        this.params = params;
        return this;
    }

    public AutoJobScriptTaskBuilder addExecutableMachine(String machineAddress) {
        this.executableMachines.add(machineAddress);
        return this;
    }

    public AutoJobScriptTaskBuilder addAllExecutableMachine(List<String> machineAddress) {
        this.executableMachines.addAll(machineAddress);
        return this;
    }

    public AutoJobScriptTaskBuilder setMailConfig(AutoJobMailConfig mailConfig) {
        this.mailConfig = mailConfig;
        if (mailConfig != null) {
            mailClient = MailClientFactory.createMailClient(mailConfig);
        }
        return this;
    }

    public AutoJobScriptTaskBuilder setVersionID(Long versionID) {
        this.versionID = versionID;
        return this;
    }

    /**
     * 添加一个简单触发器，添加多个触发器时前者将被后者覆盖
     *
     * @param firstTriggeringTime 首次触发时间
     * @param repeatTimes         重复次数，任务总触发次数=1+repeatTimes，-1表示无限次触发
     * @param cycle               周期
     * @param unit                时间单位
     * @return com.example.autojob.skeleton.model.builder.AutoJobScriptTaskBuilder
     * @author Huang Yongxiang
     * @date 2022/10/27 9:59
     */
    public AutoJobScriptTaskBuilder addASimpleTrigger(long firstTriggeringTime, int repeatTimes, long cycle, TimeUnit unit) {
        this.trigger = AutoJobTriggerFactory
                .newSimpleTrigger(firstTriggeringTime, repeatTimes, cycle, unit)
                .setTaskId(taskId);
        return this;
    }

    /**
     * 添加一个cron-like表达式的触发器，添加多个触发器时前者将被后者覆盖
     *
     * @param cronExpression cron-like表达式
     * @param repeatTimes    重复次数，任务总触发次数=1+repeatTimes，-1表示无限次触发
     * @return com.example.autojob.skeleton.model.builder.AutoJobScriptTaskBuilder
     * @author Huang Yongxiang
     * @date 2022/10/27 10:03
     */
    public AutoJobScriptTaskBuilder addACronExpressionTrigger(String cronExpression, int repeatTimes) {
        this.trigger = AutoJobTriggerFactory
                .newCronExpressionTrigger(cronExpression, repeatTimes)
                .setTaskId(taskId);
        return this;
    }

    /**
     * 添加一个子任务触发器，该任务将会作为一个子任务参与调度，添加多个触发器时前者将被后者覆盖
     *
     * @return com.example.autojob.skeleton.model.builder.AutoJobScriptTaskBuilder
     * @author Huang Yongxiang
     * @date 2022/10/27 10:09
     */
    public AutoJobScriptTaskBuilder addAChildTaskTrigger() {
        this.trigger = AutoJobTriggerFactory
                .newChildTrigger()
                .setTaskId(taskId);
        isChildTask = true;
        return this;
    }

    /**
     * 添加一个延迟触发器，任务将会在给定延迟后触发一次，添加多个触发器时前者将被后者覆盖
     *
     * @param delay 距离现在延迟执行的时间
     * @param unit  时间单位
     * @return com.example.autojob.skeleton.model.builder.AutoJobScriptTaskBuilder
     * @author Huang Yongxiang
     * @date 2022/10/27 10:12
     */
    public AutoJobScriptTaskBuilder addADelayTrigger(long delay, TimeUnit unit) {
        this.trigger = AutoJobTriggerFactory
                .newDelayTrigger(delay, unit)
                .setTaskId(taskId);
        return this;
    }

    public AutoJobScriptTaskBuilder setTrigger(AutoJobTrigger trigger) {
        this.trigger = trigger;
        return this;
    }

    private void build(ScriptTask scriptTask) {
        scriptTask.setId(taskId);
        if (trigger.getTaskId() == null) {
            trigger.setTaskId(taskId);
        }
        scriptTask.setTrigger(trigger);
        scriptTask.setVersionId(versionID);
        scriptTask.setAlias(taskAlias);
        scriptTask.setIsChildTask(isChildTask);
        scriptTask.setBelongTo(belongTo);
        scriptTask.setSaveStrategy(saveStrategy);
        scriptTask.setSchedulingStrategy(schedulingStrategy);
        scriptTask.setMailConfig(mailConfig);
        scriptTask.setIsChildTask(isChildTask);
        scriptTask.setExecutableMachines(executableMachines);
        if (shardingConfig != null) {
            if (shardingConfig.getTaskId() == null) {
                shardingConfig.setTaskID(taskId);
            }
            scriptTask.setShardingStrategy(DefaultValueUtil.defaultValue(shardingStrategy, new NumericalShardingStrategy()));
            scriptTask.setShardingConfig(shardingConfig);
        }
        if (mailConfig != null && mailConfig.getTaskId() == null) {
            mailConfig.setTaskId(taskId);
        }
        scriptTask.setRetryConfig(DefaultValueUtil.defaultValue(retryConfig, new AutoJobRetryConfig().setTaskId(scriptTask.getId())));
        scriptTask.setMailClient(DefaultValueUtil.defaultValue(mailClient, AutoJobApplication
                .getInstance()
                .getMailClient()));
        scriptTask.setType(taskType);
        scriptTask.setTaskLevel(taskLevel);
        if (params != null) {
            scriptTask.setParams(params);
            AttributesBuilder attributesBuilder = new AttributesBuilder();
            for (Object param : params) {
                attributesBuilder.addParams(AttributesBuilder.AttributesType.STRING, param);
            }
            scriptTask.setParamsString(attributesBuilder.getAttributesString());
        }
        if (scriptTask.isNeedWrite()) {
            try {
                scriptTask.write();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (isSave) {
            scriptTask.save();
        }
    }

    /**
     * 创建一个简单命令行任务
     *
     * @param cmd 要执行的命令
     * @return com.example.autojob.skeleton.model.task.script.ScriptTask
     * @author Huang Yongxiang
     * @date 2022/10/31 9:52
     */
    public ScriptTask createNewWithCmd(String cmd) {
        ScriptTask scriptTask = new ScriptTask(cmd);
        build(scriptTask);
        return scriptTask;
    }

    /**
     * 使用脚本内容创建一个脚本任务
     *
     * @param scriptType    脚本类型
     * @param scriptContent 脚本内容
     * @return com.example.autojob.skeleton.model.task.script.ScriptTask
     * @author Huang Yongxiang
     * @date 2022/10/31 9:53
     */
    public ScriptTask createNewWithContent(ScriptType scriptType, String scriptContent) {
        ScriptTask scriptTask = new ScriptTask(scriptType, scriptContent);
        build(scriptTask);
        return scriptTask;
    }

    /**
     * 使用已存在的脚本文件创建一个脚本任务，该脚本文件的类型应该在{@link ScriptType}里存在，如果你想创建一个任意脚本文件的任务，请使用{@link #createNew(String, String, String, String)}
     *
     * @param scriptType     脚本类型
     * @param scriptPath     脚本路径，new File(scriptPath)
     * @param scriptFilename 文件名，不包含后缀
     * @return com.example.autojob.skeleton.model.task.script.ScriptTask
     * @author Huang Yongxiang
     * @date 2022/10/31 9:56
     */
    public ScriptTask createNewWithExistScriptFile(ScriptType scriptType, String scriptPath, String scriptFilename) {
        ScriptTask scriptTask = new ScriptTask(scriptType, scriptPath, scriptFilename);
        build(scriptTask);
        return scriptTask;
    }

    /**
     * 使用给定路径的任意脚本文件创建一个脚本任务
     *
     * @param cmd              脚本的启动命令，如mvn、npm、python、java
     * @param scriptPath       脚本路径，new File(scriptPath)
     * @param scriptFilename   脚本文件名，不包含后缀
     * @param scriptFileSuffix 脚本后缀：.py
     * @return com.example.autojob.skeleton.model.task.script.ScriptTask
     * @author Huang Yongxiang
     * @date 2022/10/31 9:57
     */
    public ScriptTask createNew(String cmd, String scriptPath, String scriptFilename, String scriptFileSuffix) {
        ScriptTask scriptTask = new ScriptTask(cmd, scriptPath, scriptFilename, scriptFileSuffix);
        build(scriptTask);
        return scriptTask;
    }

}
