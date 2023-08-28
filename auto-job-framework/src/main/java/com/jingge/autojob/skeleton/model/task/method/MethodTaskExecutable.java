package com.jingge.autojob.skeleton.model.task.method;

import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.model.executor.DefaultMethodObjectFactory;
import com.jingge.autojob.skeleton.model.executor.IMethodObjectFactory;
import com.jingge.autojob.skeleton.model.task.TaskExecutable;
import com.jingge.autojob.util.bean.ObjectUtil;
import com.jingge.autojob.util.convert.DefaultValueUtil;
import com.jingge.autojob.util.convert.StringUtils;

import java.lang.reflect.Method;

/**
 * Method型任务的包装
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/02 11:14
 */
public class MethodTaskExecutable implements TaskExecutable {
    private final Method method;
    private Object methodObject;
    private Throwable throwable;
    private Object result;
    private transient final MethodTask task;

    public MethodTaskExecutable(MethodTask task) {
        if (task == null || task.getMethodClass() == null || StringUtils.isEmpty(task.getMethodName())) {
            throw new NullPointerException("方法型任务缺少类路径？方法名？");
        }
        IMethodObjectFactory methodObjectFactory = task.getMethodObjectFactory() == null ? new DefaultMethodObjectFactory() : task.getMethodObjectFactory();
        if (AutoJobApplication
                .getInstance()
                .getMethodObjectCache()
                .exist(task.getId())) {
            this.method = AutoJobApplication
                    .getInstance()
                    .getMethodObjectCache()
                    .get(task.getId());
        } else {
            if (task.getParams() != null) {
                this.method = ObjectUtil.findMethod(task.getMethodName(), task.getParams(), task.getMethodClass());
            } else {
                this.method = ObjectUtil.findMethod(task.getMethodName(), task.getMethodClass());
            }
            if (this.method != null) {
                AutoJobApplication
                        .getInstance()
                        .getMethodObjectCache()
                        .set(task.getId(), this.method);
            }
        }
        if (this.method == null) {
            throw new IllegalArgumentException("无指定方法");
        }
        this.methodObject = methodObjectFactory.createMethodObject(task, task.getMethodClass());
        this.task = task;
    }

    @Override
    public Object execute(Object... params) throws Exception {
        if (method != null) {
            method.setAccessible(true);
            methodObject = DefaultValueUtil.defaultObjectWhenNull(methodObject, ObjectUtil.getClassInstance(task.getMethodClass()));
            if (params != null && params.length > 0) {
                result = method.invoke(methodObject, params);
            } else {
                result = method.invoke(methodObject);
            }
            return result;
        } else {
            Exception exception = new NullPointerException("空任务，无法执行");
            throwable = exception;
            throw exception;
        }
    }

    @Override
    public Object[] getExecuteParams() {
        return task.getParams();
    }

    @Override
    public AutoJobTask getAutoJobTask() {
        return task;
    }

    @Override
    public boolean isExecutable() {
        return method != null;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public Object getResult() {
        return result;
    }
}
