package com.jingge.autojob.skeleton.framework.pool;

import com.jingge.autojob.skeleton.framework.task.DefaultRunnablePostProcessor;

/**
 * 运行后置处理器，框架使用默认实现：{@link DefaultRunnablePostProcessor}，原则上无需自己实现，如果确需自己实现，请继承{@link DefaultRunnablePostProcessor}，并且保证父方法能被执行，而不是重新实现该接口
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/02 13:52
 */
public interface RunnablePostProcessor {
    void beforeRun(final Executable executable, AutoJobPoolExecutor executor, Object... params);

    void afterRun(final Executable executable, AutoJobPoolExecutor executor, Object result);

    void runError(final Executable executable, AutoJobPoolExecutor executor, Throwable throwable, Object result);
}
