package com.jingge.autojob.skeleton.model.register;

import com.jingge.autojob.skeleton.framework.task.AutoJobTask;

/**
 * @Description 注册处理器链，AutoJob会自动注册被Spring管理的该类的子类对象
 * @Author Huang Yongxiang
 * @Date 2022/07/06 14:09
 */
public abstract class AbstractRegisterHandler {
    protected AbstractRegisterHandler chain;

    protected AbstractRegisterHandler() {
    }

    private void next(AbstractRegisterHandler handler) {
        this.chain = handler;
    }

    public synchronized AbstractRegisterHandler add(AbstractRegisterHandler handler) {
        AbstractRegisterHandler tail = null;
        for (tail = this; tail.chain != null; ) {
            tail = tail.chain;
        }
        tail.chain = handler;
        tail = handler;
        return tail;
    }

    public static Builder builder() {
        return new Builder();
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
        private AbstractRegisterHandler head;
        private AbstractRegisterHandler tail;

        public synchronized Builder addHandler(AbstractRegisterHandler handler) {
            if (handler == null) {
                return this;
            }
            if (head == null) {
                head = tail = handler;
                return this;
            }
            tail.next(handler);
            tail = handler;
            return this;
        }

        public AbstractRegisterHandler build() {
            return head;
        }
    }


}
