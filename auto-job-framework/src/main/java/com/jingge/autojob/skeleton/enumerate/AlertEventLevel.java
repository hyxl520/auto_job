package com.jingge.autojob.skeleton.enumerate;

/**
 * 报警事件级别
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/28 13:52
 */
public enum AlertEventLevel {
    /**
     * 普通提醒
     */
    INFO,
    /**
     * 警告
     */
    WARN,
    /**
     * 严重警告
     */
    SERIOUS_WARN,
    /**
     * 错误
     */
    ERROR;

    public static String valueOf(AlertEventLevel level) {
        switch (level) {
            case INFO: {
                return "提醒";
            }
            case WARN: {
                return "警告";
            }
            case SERIOUS_WARN: {
                return "严重警告";
            }
            case ERROR: {
                return "系统错误";
            }
        }
        return null;
    }
}
