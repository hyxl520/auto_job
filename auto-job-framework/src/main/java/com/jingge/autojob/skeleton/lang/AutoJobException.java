package com.jingge.autojob.skeleton.lang;

/**
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-08-15 10:11
 * @email 1158055613@qq.com
 */
public class AutoJobException extends RuntimeException{
    public AutoJobException() {
        super();
    }

    public AutoJobException(String message) {
        super(message);
    }

    public AutoJobException(String message, Throwable cause) {
        super(message, cause);
    }

    public AutoJobException(Throwable cause) {
        super(cause);
    }

    protected AutoJobException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
