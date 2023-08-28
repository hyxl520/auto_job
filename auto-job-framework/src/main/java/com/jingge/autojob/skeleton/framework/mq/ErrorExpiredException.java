package com.jingge.autojob.skeleton.framework.mq;

/**
 * 消息过期异常类
 *
 * @Auther Huang Yongxiang
 * @Date 2022/03/20 11:32
 */
public class ErrorExpiredException extends RuntimeException {
    public ErrorExpiredException() {
        super();
    }

    public ErrorExpiredException(String message) {
        super(message);
    }

    public ErrorExpiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public ErrorExpiredException(Throwable cause) {
        super(cause);
    }

    protected ErrorExpiredException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
