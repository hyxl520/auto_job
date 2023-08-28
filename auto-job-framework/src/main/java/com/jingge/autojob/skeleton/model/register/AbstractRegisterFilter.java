package com.jingge.autojob.skeleton.model.register;

import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.framework.config.AutoJobConfig;

/**
 * 注册过滤器，AutoJob会自动注册该类的子类对象
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/11 16:37
 */
public abstract class AbstractRegisterFilter {
    protected AbstractRegisterFilter chain;
    protected AutoJobConfig config;


    protected AbstractRegisterFilter() {
        this.config = AutoJobApplication
                .getInstance()
                .getConfigHolder()
                .getAutoJobConfig();
    }

    private void next(AbstractRegisterFilter handler) {
        this.chain = handler;
    }

    public synchronized AbstractRegisterFilter add(AbstractRegisterFilter filter) {
        AbstractRegisterFilter tail = null;
        for (tail = this; tail.chain != null; ) {
            tail = tail.chain;
        }
        tail.chain = filter;
        tail = filter;
        return tail;
    }

    public static Builder builder() {
        return new AbstractRegisterFilter.Builder();
    }

    /**
     * 对将要注册的任务进行处理
     *
     * @param task 任务
     * @return void
     * @author Huang Yongxiang
     * @date 2022/7/6 14:13
     */
    public abstract void doHandle(AutoJobTask task);

    public static class Builder {
        private AbstractRegisterFilter head;
        private AbstractRegisterFilter tail;

        public synchronized Builder addHandler(AbstractRegisterFilter filter) {
            if (filter == null) {
                return this;
            }
            if (head == null) {
                head = tail = filter;
                return this;
            }
            tail.next(filter);
            tail = filter;
            return this;
        }

        public AbstractRegisterFilter build() {
            return head;
        }
    }
}
