package com.jingge.autojob.skeleton.framework.container;

/**
 * 容器异常
 *
 * @Author Huang Yongxiang
 * @Date 2022/10/14 14:32
 */
public class AutoJobContainerException extends RuntimeException {
    public AutoJobContainerException() {
        super();
    }

    public AutoJobContainerException(String message) {
        super(message);
    }

    public AutoJobContainerException(String message, Throwable cause) {
        super(message, cause);
    }

    public AutoJobContainerException(Throwable cause) {
        super(cause);
    }

    protected AutoJobContainerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
