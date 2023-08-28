package com.jingge.autojob.util.convert;

import com.jingge.autojob.util.bean.ObjectUtil;

/**
 * @Description 默认值工具类
 * @Author Huang Yongxiang
 * @Date 2022/07/11 11:43
 */
public class DefaultValueUtil {

    public static String chooseString(boolean predicate, String trueValue, String falseValue) {
        return predicate ? trueValue : falseValue;
    }

    public static Object chooseObject(boolean predicate, Object trueValue, Object falseValue) {
        return predicate ? trueValue : falseValue;
    }

    @SuppressWarnings("unchecked")
    public static <T> T chooseTypeObject(boolean predicate, Object trueValue, Object falseValue, Class<T> type) {
        return (T) chooseObject(predicate, trueValue, falseValue);
    }

    public static Number chooseNumber(boolean predicate, Number trueValue, Number falseValue) {
        return predicate ? trueValue : falseValue;
    }

    public static int chooseInteger(boolean predicate, int trueValue, int falseValue) {
        return (int) chooseNumber(predicate, trueValue, falseValue);
    }

    public static double chooseDouble(boolean predicate, double trueValue, double falseValue) {
        return (double) chooseNumber(predicate, trueValue, falseValue);
    }

    public static String defaultStringWhenEmpty(String value, String defaultString) {
        return chooseString(StringUtils.isEmpty(value), defaultString, value);
    }

    public static Object defaultObjectWhenNull(Object value, Object defaultObject) {
        return chooseObject(value == null, defaultObject, value);
    }

    public static Number defaultNumberWhenNull(Number value, Number defaultNumber) {
        return chooseNumber(value == null, defaultNumber, value);
    }

    public static <T> T defaultValue(T value, T orElse) {
        if (ObjectUtil.isNull(value) || (value instanceof String && StringUtils.isEmpty((String) value))) {
            return orElse;
        }
        return value;
    }


}
