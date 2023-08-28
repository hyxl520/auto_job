package com.jingge.autojob.skeleton.framework.task;

import com.jingge.autojob.logging.model.producer.AutoJobLogHelper;
import com.jingge.autojob.skeleton.enumerate.SaveStrategy;
import com.jingge.autojob.skeleton.enumerate.SchedulingStrategy;
import com.jingge.autojob.skeleton.framework.config.AutoJobMailConfig;
import com.jingge.autojob.skeleton.framework.config.AutoJobRetryConfig;
import com.jingge.autojob.skeleton.framework.config.AutoJobShardingConfig;
import com.jingge.autojob.skeleton.framework.config.RetryStrategy;
import com.jingge.autojob.skeleton.model.executor.DefaultMethodObjectFactory;
import com.jingge.autojob.skeleton.model.executor.IMethodObjectFactory;
import com.jingge.autojob.util.convert.StringUtils;
import com.jingge.autojob.util.id.IdGenerator;
import com.jingge.autojob.skeleton.annotation.TemplateAutoJob;
import com.jingge.autojob.util.io.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AutoJob的任务模板，基于该任务模板你能更加方便简单地开发任务。子类只需要继承该类，并且在类上加上注解{@link TemplateAutoJob}即可
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-03-28 15:31
 * @email 1158055613@qq.com
 */
@Slf4j
public abstract class AutoJobTemplate implements Template {
    protected PropertiesHolder propertiesHolder;

    /**
     * 任务的主要内容，子类必须实现该方法
     *
     * @param params  任务执行的参数，通过方法{@link #params()}返回
     * @param context 任务执行上下文
     * @return java.lang.Object
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/3/29 10:51
     */
    public abstract Object run(Object[] params, AutoJobRunningContext context) throws Exception;

    public AutoJobTemplate() {
        String profile = withProfile();
        List<InputStream> inputStreams = withProfilesInputStream();
        if (!StringUtils.isEmpty(profile) && propertiesHolder == null) {
            propertiesHolder = PropertiesHolder
                    .builder()
                    .addPropertiesFile(profile)
                    .addOthers(inputStreams)
                    .setIgnoreSystemProperties(false)
                    .build();
        }
    }

    /**
     * 有时该任务的一些行为需要通过配置文件给出，此时子类可以覆盖该方法返回类路径下的配置文件路径，模板将会创建一个{@link PropertiesHolder}对象
     *
     * @return java.lang.String
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/3/29 10:56
     */
    public String withProfile() {
        return null;
    }

    @Override
    public List<String> executableMachines() {
        return Collections.emptyList();
    }

    /**
     * 有时配置不来自于配置文件，此时可以通过输入流导入
     *
     * @return java.util.List<java.io.InputStream>
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/3/29 17:55
     */
    public List<InputStream> withProfilesInputStream() {
        return Collections.emptyList();
    }

    /**
     * 子类可以控制任务执行与否
     *
     * @return boolean
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/3/29 10:57
     */
    public boolean enable() {
        return true;
    }

    @Override
    public Long versionID() {
        return (long) Math.abs(this
                .getClass()
                .hashCode());
    }

    @Override
    public AutoJobTask.TaskType taskType() {
        return AutoJobTask.TaskType.MEMORY_TASk;
    }

    @Override
    public AutoJobRetryConfig retryConfig() {
        return new AutoJobRetryConfig(true, RetryStrategy.LOCAL_RETRY, 3, 5);
    }

    @Override
    public long id() {
        return IdGenerator.getNextIdAsLong();
    }

    @Override
    public String alias() {
        return this
                .getClass()
                .getSimpleName();
    }

    @Override
    public SaveStrategy saveStrategy() {
        return SaveStrategy.SAVE_IF_ABSENT;
    }

    @Override
    public ShardingStrategy shardingStrategy() {
        return new NumericalShardingStrategy();
    }

    @Override
    public AutoJobShardingConfig shardingConfig() {
        return new AutoJobShardingConfig(null, false, null);
    }

    @Override
    public int repeatTimes() {
        return 0;
    }

    @Override
    public long maximumExecutionTime() {
        return TimeUnit.HOURS.toMillis(24);
    }

    @Override
    public IMethodObjectFactory methodObjectFactory() {
        return new DefaultMethodObjectFactory();
    }

    @Override
    public List<Long> childTasks() {
        return null;
    }

    @Override
    public SchedulingStrategy schedulingStrategy() {
        return SchedulingStrategy.JOIN_SCHEDULING;
    }

    @Override
    public AutoJobMailConfig mailConfig() {
        return null;
    }

    /**
     * 任务执行的核心方法，该方法不允许被修改
     *
     * @param params 任务执行的参数，通过方法{@link #params()}返回
     * @return java.lang.Object
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/3/29 10:57
     */
    public final Object core(Object[] params) throws Exception {
        AutoJobLogHelper logHelper = AutoJobLogHelper.getInstance();
        logHelper.setSlf4jProxy(log);
        if (!enable()) {
            logHelper.info("模板任务：{}将不会被执行", alias());
            return null;
        }
        long start = System.currentTimeMillis();
        try {
            return run(params, AutoJobRunningContextHolder.currentTaskContext());
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                logHelper.warn("任务：{}被强制停止", alias());
            } else {
                throw e;
            }
        } finally {
            logHelper.info("任务：{}执行完成，共计用时：{}ms", alias(), System.currentTimeMillis() - start);
        }
        return null;
    }
}
