package com.jingge.autojob.skeleton.model.handler;

import com.jingge.autojob.skeleton.enumerate.SchedulingStrategy;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.framework.task.AutoJobWrapper;
import com.jingge.autojob.skeleton.framework.task.Template;
import com.jingge.autojob.skeleton.model.builder.AutoJobMethodTaskBuilder;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-03-28 16:15
 * @email 1158055613@qq.com
 */
public class TemplateAutoJobAnnotationWrapper implements AutoJobWrapper {
    private static final String ID = "id";
    private static final String RETRY_CONFIG = "retryConfig";
    private static final String WITH_PROFILE = "withProfile";
    private static final String ALIAS = "alias";
    private static final String PARAMS = "params";
    private static final String CRON = "cron";
    private static final String ANNOTATION_ID = "annotationID";
    private static final String REPEAT_TIMES = "repeatTimes";
    private static final String MAXIMUM_EXECUTION_TIME = "maximumExecutionTime";
    private static final String METHOD_OBJECT_FACTORY = "methodObjectFactory";
    private static final String CHILD_TASKS = "childTasks";
    private static final String SCHEDULING_STRATEGY = "schedulingStrategy";
    private static final String MAIL_CONFIG = "mailConfig";
    private static final String TASK_TYPE = "taskType";
    private static final String SHARDING_CONFIG = "shardingConfig";
    private static final String SHARDING_STRATEGY = "shardingStrategy";
    private static final String SAVE_STRATEGY = "saveStrategy";

    @Override
    public AutoJobTask wrapper(Method method, Class<?> clazz) {
        Template templateProxy = getTemplateProxy(clazz);
        long taskID = templateProxy.id();
        AutoJobTask task = new AutoJobMethodTaskBuilder(clazz, "core")
                .setTaskId(taskID)
                .addParam(Object[].class, templateProxy.params())
                .setTrigger(templateProxy
                        .schedulingStrategy()
                        .createTrigger(taskID, templateProxy.cron(), templateProxy.repeatTimes()))
                .setMaximumExecutionTime(templateProxy.maximumExecutionTime(), TimeUnit.MILLISECONDS)
                .setTaskType(templateProxy.taskType())
                .setVersionID(templateProxy.versionID())
                .setSaveStrategy(templateProxy.saveStrategy())
                .setMailConfig(templateProxy.mailConfig())
                .setShardingStrategy(templateProxy.shardingStrategy())
                .setShardingConfig(templateProxy.shardingConfig().setTaskID(taskID))
                .setMethodObjectFactory(templateProxy.methodObjectFactory())
                .setSchedulingStrategy(SchedulingStrategy.JOIN_SCHEDULING)
                .setRetryConfig(templateProxy.retryConfig().setTaskId(taskID))
                .build();
        if (task.getTrigger() != null) {
            task
                    .getTrigger()
                    .setChildTask(templateProxy.childTasks());
        }
        return task;
    }

    private static class Interceptor implements MethodInterceptor {
        private final List<String> needInvokeMethod;

        public Interceptor(List<String> needInvokeMethod) {
            this.needInvokeMethod = needInvokeMethod;
        }

        @Override
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
            if (needInvokeMethod.contains(method.getName())) {
                return proxy.invokeSuper(obj, args);
            }
            return null;
        }
    }

    private Template getTemplateProxy(Class<?> clazz) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(clazz);
        enhancer.setCallback(new Interceptor(Arrays.asList(WITH_PROFILE, ALIAS, PARAMS, CRON, REPEAT_TIMES, MAXIMUM_EXECUTION_TIME, METHOD_OBJECT_FACTORY, ID, RETRY_CONFIG, CHILD_TASKS, SCHEDULING_STRATEGY, MAIL_CONFIG, TASK_TYPE, ANNOTATION_ID, SHARDING_CONFIG, SAVE_STRATEGY, SHARDING_STRATEGY)));
        return (Template) enhancer.create();
    }
}
