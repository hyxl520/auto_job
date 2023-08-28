package com.jingge.autojob.skeleton.framework.task;

import com.jingge.autojob.logging.model.producer.AutoJobLogHelper;
import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;
import com.jingge.autojob.skeleton.enumerate.SaveStrategy;
import com.jingge.autojob.skeleton.enumerate.SchedulingStrategy;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.config.AutoJobMailConfig;
import com.jingge.autojob.skeleton.framework.config.AutoJobRetryConfig;
import com.jingge.autojob.skeleton.framework.config.AutoJobShardingConfig;
import com.jingge.autojob.skeleton.framework.mail.IMailClient;
import com.jingge.autojob.skeleton.framework.pool.RunnablePostProcessor;
import com.jingge.autojob.skeleton.model.builder.AutoJobMethodTaskBuilder;
import com.jingge.autojob.skeleton.model.task.TaskExecutable;
import com.jingge.autojob.skeleton.model.task.method.MethodTask;
import com.jingge.autojob.skeleton.model.task.script.ScriptTask;
import com.jingge.autojob.util.convert.ProtoStuffUtil;
import com.jingge.autojob.util.convert.StringUtils;
import com.jingge.autojob.util.id.IdGenerator;
import com.jingge.autojob.util.message.MessageManager;
import com.jingge.autojob.util.thread.ScheduleTaskUtil;
import jdk.nashorn.internal.scripts.JD;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import sun.security.provider.SHA;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 任务的超类，所有任务都应该继承该类，该类及其子类作为任务调度的基本单元
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/02 16:55
 */
@Getter
@Setter
@Accessors(chain = true)
public abstract class AutoJobTask implements Serializable {
    /**
     * 任务Id
     */
    protected Long id;
    /**
     * 任务别名
     */
    protected String alias;
    /**
     * 版本ID，同一个版本ID的任务为同一个任务的不同版本，每次调度都将调度最新版本
     */
    protected Long versionId;
    /**
     * 调度队列中标识的唯一ID
     */
    protected Long scheduleQueueId;
    /**
     * 触发器
     */
    protected AutoJobTrigger trigger;
    /**
     * 调度策略
     */
    protected SchedulingStrategy schedulingStrategy;
    /**
     * 保存策略
     */
    protected SaveStrategy saveStrategy = SaveStrategy.SAVE_IF_ABSENT;
    /**
     * 运行状态
     */
    protected volatile AtomicReference<AutoJobRunningStatus> runningStatus = new AtomicReference<>(AutoJobRunningStatus.CREATED);
    /**
     * 重试配置
     */
    protected AutoJobRetryConfig retryConfig;
    /**
     * 邮件配置
     */
    protected AutoJobMailConfig mailConfig;
    /**
     * 分片配置
     */
    protected AutoJobShardingConfig shardingConfig;

    /**
     * 分片ID
     */
    protected Long shardingId;

    /**
     * 分片策略
     */
    protected ShardingStrategy shardingStrategy = new NumericalShardingStrategy();

    /**
     * 运行结果
     */
    protected AutoJobRunResult runResult;
    /**
     * 是否已结束
     */
    protected Boolean isFinished;
    /**
     * 任务参数
     */
    protected Object[] params;
    /**
     * 参数字符串，Simple型参数或FULL型参数
     */
    protected String paramsString;
    /**
     * 是否允许被注册
     */
    protected Boolean isAllowRegister = true;
    /**
     * 任务类型
     */
    protected TaskType type;

    /**
     * 归属于
     */
    protected Long belongTo;

    /**
     * 任务优先级
     */
    protected Integer taskLevel = -1;
    /**
     * 是否是子任务
     */
    protected Boolean isChildTask = false;
    /**
     * 是否是分片任务
     */
    protected Boolean isShardingTask = false;
    /**
     * 是否已经广播分片
     */
    protected Boolean isAlreadyBroadcastSharding = false;
    /**
     * 可执行的机器，存在且在这个列表内的机器才可以执行
     */
    protected List<String> executableMachines;
    /**
     * 任务持有的logHelper对象实例
     */
    protected AutoJobLogHelper logHelper;
    /**
     * 邮件报警客户端
     */
    protected IMailClient mailClient;
    /**
     * 运行堆栈
     */
    protected AutoJobRunningStack stack;

    public static AutoJobTask deepCopyFrom(AutoJobTask other) {
        return ProtoStuffUtil.cloneObject(other);
    }

    /**
     * 获取该任务的分片
     *
     * @param deepCopy        是否进行深拷贝
     * @param changeType      是否更改任务类型为{@link TaskType#SHARDING}
     * @param shardingId      分片ID
     * @param currentSharding 当前的分片
     * @return com.jingge.autojob.skeleton.framework.task.AutoJobTask
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/8/17 17:09
     */
    public AutoJobTask splitShardingTask(boolean deepCopy, boolean changeType, long shardingId, Object currentSharding) {
        if (!isEnableSharding()) {
            throw new UnsupportedOperationException("当前任务不支持切片");
        }
        AutoJobTask newOne = deepCopy ? deepCopyFrom(this) : this;
        if (changeType) {
            newOne.setType(TaskType.SHARDING);
        }
        newOne
                .getTrigger()
                .setRepeatTimes(0);
        newOne.setShardingId(shardingId);
        newOne.shardingConfig.setCurrent(currentSharding);
        return newOne;
    }

    public boolean isSharding() {
        return type == TaskType.SHARDING && isEnableSharding();
    }

    public boolean isEnableSharding() {
        return isShardingTask && shardingConfig != null && shardingConfig.isEnable();
    }

    public AutoJobTask setTrigger(AutoJobTrigger trigger) {
        this.trigger = trigger;
        if (trigger != null && trigger.isNextReachable()) {
            updateRunningStatus(AutoJobRunningStatus.SCHEDULING);
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof AutoJobTask) {
            AutoJobTask task = (AutoJobTask) o;
            return task.id != null && task.id.equals(id);
        }
        return false;
    }

    public void updateRunningStatus(AutoJobRunningStatus status) {
        this.runningStatus.set(status);
        if (type == TaskType.DB_TASK && status.isUpdateDB()) {
            ScheduleTaskUtil.oneTimeTask(() -> {
                AutoJobMapperHolder
                        .getMatchTaskMapper(id)
                        .updateRunningStatus(status, id);
                //System.out.println("更新任务" + id + "的状态为" + status.name());
                return null;
            }, 1000, TimeUnit.MILLISECONDS);
        } else if (type == TaskType.MEMORY_TASk) {
            AutoJobApplication
                    .getInstance()
                    .getMemoryTaskContainer()
                    .updateRunningStatus(id, status);
        }
    }

    public AutoJobTask setShardingConfig(AutoJobShardingConfig shardingConfig) {
        this.shardingConfig = shardingConfig;
        this.isShardingTask = shardingConfig != null && shardingConfig.isEnable();
        return this;
    }

    public AutoJobTask setRunningStatus(AutoJobRunningStatus runningStatus) {
        this.runningStatus.set(runningStatus);
        return this;
    }

    public AutoJobRunningStatus getRunningStatus() {
        return runningStatus.get();
    }

    public void inScheduleQueue() {
        scheduleQueueId = IdGenerator.getNextIdAsLong();
    }

    public void outScheduleQueue() {
        scheduleQueueId = null;
    }

    /**
     * 任务需要能够从中获取可执行对象
     *
     * @return com.example.autojob.skeleton.framework.pool.Executable
     * @author Huang Yongxiang
     * @date 2022/8/3 13:58
     */
    public abstract TaskExecutable getExecutable();

    /**
     * 用于判断某个任务能否被执行
     *
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/11/2 17:33
     */
    public boolean isExecutable() {
        return getExecutable() != null && getExecutable().isExecutable();
    }

    public boolean save() {
        if (id == null || !isExecutable()) {
            return false;
        }
        if (type == TaskType.DB_TASK) {
            if (this instanceof MethodTask) {
                return AutoJobMapperHolder.METHOD_TASK_ENTITY_MAPPER.saveTasks(Arrays.asList(this));
            } else if (this instanceof ScriptTask) {
                return AutoJobMapperHolder.SCRIPT_TASK_ENTITY_MAPPER.saveTasks(Arrays.asList(this));
            }
        } else {
            AutoJobApplication
                    .getInstance()
                    .getMemoryTaskContainer()
                    .insert(this);
        }
        return true;
    }

    /**
     * 任务需要能够获取对应的后置处理器，自定义后置处理器必须保证能执行父后置处理器的逻辑
     *
     * @return com.example.autojob.skeleton.framework.pool.RunnablePostProcessor
     * @author Huang Yongxiang
     * @date 2022/8/3 16:27
     */
    public RunnablePostProcessor getRunnablePostProcessor() {
        return new DefaultRunnablePostProcessor();
    }

    public String getReference() {
        return "" + id;
    }

    /**
     * 拒绝任务执行
     *
     * @return void
     * @author Huang Yongxiang
     * @date 2023/1/3 14:02
     */
    public void forbidden() {
        isAllowRegister = false;
    }

    public boolean isAllowRetry() {
        return retryConfig != null && retryConfig.getEnable() != null && retryConfig.getEnable() && retryConfig.getRetryCount() > trigger
                .getCurrentRepeatTimes()
                .get();
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public enum TaskType {
        /**
         * 内存型任务的生命周期都将在内存进行，其本身要比DB型任务的调度更为精确和快速，适合周期短的任务
         */
        MEMORY_TASk("Memory"),
        /**
         * DB型任务将会在数据库保存任务基本信息和触发器信息，短周期DB任务触发可能不会很精确，DB任务在调度前都会经历一个获取锁的过程，只有获取到锁才能被执行
         */
        DB_TASK("DB"),

        /**
         * 任务分片，分片只会运行一次
         */
        SHARDING("Sharding");
        String description;

        TaskType(String description) {
            this.description = description;
        }

        public static TaskType convert(String description) {
            if (StringUtils.isEmpty(description)) {
                return null;
            }
            description = description.toLowerCase();
            switch (description) {
                case "db": {
                    return DB_TASK;
                }
                case "memory": {
                    return MEMORY_TASk;
                }
                case "sharding": {
                    return SHARDING;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return description;
        }
    }
}
