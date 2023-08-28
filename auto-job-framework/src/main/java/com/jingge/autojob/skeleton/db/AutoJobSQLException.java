package com.jingge.autojob.skeleton.db;

/**
 * @Author Huang Yongxiang
 * @Date 2022/10/14 11:29
 */
public class AutoJobSQLException extends RuntimeException{
    public AutoJobSQLException() {
        super();
    }

    public AutoJobSQLException(String message) {
        super(message);
    }

    public AutoJobSQLException(String message, Throwable cause) {
        super(message, cause);
    }

    public AutoJobSQLException(Throwable cause) {
        super(cause);
    }

    protected AutoJobSQLException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
