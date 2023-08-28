package com.jingge.autojob.skeleton.framework.pool;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

/**
 * 执行器，执行器是对可执行对象的封装
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/02 13:50
 */
@Slf4j
public class AutoJobPoolExecutor implements Callable<Object>, RunnablePostProcessor {
    private String executorName;
    private volatile Executable executable;
    private Object[] params;
    private Throwable throwable;
    private Object result;
    private RunnablePostProcessor runnablePostProcessor;

    public AutoJobPoolExecutor(Executable executable) {
        this.executable = executable;
    }

    public AutoJobPoolExecutor(Executable executable, RunnablePostProcessor runnablePostProcessor) {
        this(executable, null, runnablePostProcessor);
    }

    public AutoJobPoolExecutor(Executable executable, Object[] params) {
        this(executable, params, null);
    }

    public AutoJobPoolExecutor(Executable executable, Object[] params, RunnablePostProcessor runnablePostProcessor) {
        this.executable = executable;
        this.params = params;
        this.runnablePostProcessor = runnablePostProcessor;
    }

    public AutoJobPoolExecutor() {
    }

    public void setRunnablePostProcessor(RunnablePostProcessor runnablePostProcessor) {
        this.runnablePostProcessor = runnablePostProcessor;
    }

    public void setExecutorName(String executorName) {
        this.executorName = executorName;
    }

    public String getExecutorName() {
        return executorName;
    }

    public boolean connect(Executable executable, Object... params) {
        if (executable == null) {
            log.error("无法建立与执行器{}的连接，可执行对象为null", executorName);
            return false;
        }
        this.executable = executable;
        this.params = params;
        return true;
    }

    @Override
    public Object call() {
        if (executable != null) {
            try {
                beforeRun(executable, this, params);
                result = executable.execute(params);
                afterRun(executable, this, result);
            } catch (Exception e) {
                e.printStackTrace();
                throwable = e;
                runError(executable, this, throwable, result);
            }
        } else {
            log.error("执行器{}无法执行，因为要执行的可执行对象Executable为null", executorName);
        }
        return result;
    }

    @Override
    public void beforeRun(final Executable executable, AutoJobPoolExecutor executor, Object... params) {
        if (runnablePostProcessor != null) {
            runnablePostProcessor.beforeRun(executable, this, params);
        }

    }

    @Override
    public void afterRun(final Executable executable, AutoJobPoolExecutor executor, Object result) {
        if (runnablePostProcessor != null) {
            runnablePostProcessor.afterRun(executable, this, result);
        }
    }

    @Override
    public void runError(final Executable executable, AutoJobPoolExecutor executor, Throwable throwable, Object result) {
        if (runnablePostProcessor != null) {
            runnablePostProcessor.runError(executable, this, this.throwable, result);
        }
    }
}
