package com.jingge.autojob.skeleton.framework.network.handler.server;

/**
 * @Author Huang Yongxiang
 * @Date 2022/09/19 15:03
 */
public class NoSuchServiceMethodException extends RuntimeException{
    public NoSuchServiceMethodException() {
        super();
    }

    public NoSuchServiceMethodException(String message) {
        super(message);
    }

    public NoSuchServiceMethodException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchServiceMethodException(Throwable cause) {
        super(cause);
    }

    protected NoSuchServiceMethodException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
