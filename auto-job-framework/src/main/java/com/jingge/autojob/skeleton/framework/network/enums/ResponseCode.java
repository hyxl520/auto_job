package com.jingge.autojob.skeleton.framework.network.enums;

/**
 * RPC响应回执码
 *
 * @Author Huang Yongxiang
 * @Date 2022/09/09 17:14
 */
public enum ResponseCode {
    /**
     * 成功
     */
    SUCCESS(0),
    /**
     * 没有指定服务
     */
    NO_SERVICE(1),
    /**
     * 没有指定服务的方法
     */
    NO_SERVICE_METHOD(2),
    /**
     * 服务器错误
     */
    SERVER_ERROR(-1),
    /**
     * 未授权访问
     */
    FORBIDDEN(-2);
    public int code;

    public static ResponseCode findByCode(int code) {
        for (ResponseCode c : ResponseCode.values()) {
            if (c.code == code) {
                return c;
            }
        }
        return null;
    }

    ResponseCode(int code) {
        this.code = code;
    }
}
