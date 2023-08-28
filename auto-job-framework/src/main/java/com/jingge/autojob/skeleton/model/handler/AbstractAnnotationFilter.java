package com.jingge.autojob.skeleton.model.handler;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * 注解扫描过滤器链
 *
 * @author Huang Yongxiang
 * @date 2022-12-03 17:12
 * @email 1158055613@qq.com
 */
public abstract class AbstractAnnotationFilter {
    protected AbstractAnnotationFilter nextFilter;

    private void next(AbstractAnnotationFilter handler) {
        this.nextFilter = handler;
    }

    public synchronized AbstractAnnotationFilter add(AbstractAnnotationFilter filter) {
        if (filter == null) {
            return this;
        }
        AbstractAnnotationFilter tail = null;
        for (tail = this; tail.nextFilter != null; ) {
            tail = tail.nextFilter;
        }
        tail.nextFilter = filter;
        tail = filter;
        return tail;
    }

    public abstract void doFilter(Set<Method> scannedMethods);

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private AbstractAnnotationFilter head;
        private AbstractAnnotationFilter tail;

        public synchronized Builder addHandler(AbstractAnnotationFilter filter) {
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

        public AbstractAnnotationFilter build() {
            return head;
        }
    }


}
