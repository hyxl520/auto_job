package com.jingge.autojob.skeleton.model.interpreter;

/**
 * @Description 支持的基本参数类型
 * @Author Huang Yongxiang
 * @Date 2022/07/06 15:27
 */
public enum SupportAttributeType {
    INTEGER, LONG, DECIMAL, STRING, BOOLEAN, OBJECT, UN_SUPPORT;

    public static SupportAttributeType convert(Object o) {
        Class<?> type;
        if (o instanceof Class) {
            type = (Class<?>) o;
            if (type == Integer.class) {
                return INTEGER;
            } else if (type == Long.class) {
                return LONG;
            } else if (type == Double.class) {
                return DECIMAL;
            } else if (type == String.class) {
                return STRING;
            } else if (type == Boolean.class) {
                return BOOLEAN;
            } else {
                return OBJECT;
            }
        }
        return UN_SUPPORT;
    }
}
