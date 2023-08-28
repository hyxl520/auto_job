package com.jingge.autojob.skeleton.framework.network.handler.client;

/**
 * @Author Huang Yongxiang
 * @Date 2022/09/19 16:01
 */
public class RequestFailedException extends RuntimeException{

    public RequestFailedException() {
        super();
    }

    public RequestFailedException(String message) {
        super(message);
    }

    public RequestFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public RequestFailedException(Throwable cause) {
        super(cause);
    }

    protected RequestFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
