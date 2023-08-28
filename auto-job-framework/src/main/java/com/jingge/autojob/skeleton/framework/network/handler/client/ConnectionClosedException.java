package com.jingge.autojob.skeleton.framework.network.handler.client;

/**
 * @Author Huang Yongxiang
 * @Date 2022/09/20 10:47
 */
public class ConnectionClosedException extends RuntimeException{
    public ConnectionClosedException() {
        super();
    }

    public ConnectionClosedException(String message) {
        super(message);
    }

    public ConnectionClosedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnectionClosedException(Throwable cause) {
        super(cause);
    }

    protected ConnectionClosedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
