package com.jingge.autojob.skeleton.model.register;

/**
 * 任务被拒绝异常
 *
 * @Author Huang Yongxiang
 * @Date 2022/10/25 9:55
 */
public class AutoJobRegisterRefusedException extends RuntimeException {
    public AutoJobRegisterRefusedException() {
        super();
    }

    public AutoJobRegisterRefusedException(String message) {
        super(message);
    }

    public AutoJobRegisterRefusedException(String message, Throwable cause) {
        super(message, cause);
    }

    public AutoJobRegisterRefusedException(Throwable cause) {
        super(cause);
    }

    protected AutoJobRegisterRefusedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
