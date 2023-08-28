package com.jingge.autojob.skeleton.framework.network.handler.client;

/**
 * @Author Huang Yongxiang
 * @Date 2022/09/21 16:58
 */
public class ResponseTimeoutException extends RuntimeException{
    public ResponseTimeoutException() {
        super();
    }

    public ResponseTimeoutException(String message) {
        super(message);
    }

    public ResponseTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResponseTimeoutException(Throwable cause) {
        super(cause);
    }

    protected ResponseTimeoutException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
