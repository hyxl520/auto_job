package com.jingge.autojob.skeleton.framework.network.handler.server;

/**
 * @Author Huang Yongxiang
 * @Date 2022/09/19 15:03
 */
public class NoSuchServiceException extends RuntimeException{

    public NoSuchServiceException() {
        super();
    }

    public NoSuchServiceException(String message) {
        super(message);
    }

    public NoSuchServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchServiceException(Throwable cause) {
        super(cause);
    }

    protected NoSuchServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
