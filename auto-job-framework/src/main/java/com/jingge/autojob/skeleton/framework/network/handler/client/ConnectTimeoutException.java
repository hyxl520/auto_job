package com.jingge.autojob.skeleton.framework.network.handler.client;

/**
 * 连接超时
 *
 * @Author Huang Yongxiang
 * @Date 2022/09/21 15:08
 */
public class ConnectTimeoutException extends RuntimeException{
    public ConnectTimeoutException() {
        super();
    }

    public ConnectTimeoutException(String message) {
        super(message);
    }

    public ConnectTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnectTimeoutException(Throwable cause) {
        super(cause);
    }

    protected ConnectTimeoutException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
