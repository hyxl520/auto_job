package com.jingge.autojob.util.bean;

import com.jingge.autojob.util.convert.StringUtils;
import com.jingge.autojob.util.json.JsonUtil;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Auther Huang Yongxiang
 * @Date 2022/02/24 10:46
 */
public class ObjectUtil {
    private static final Logger logger = LoggerFactory.getLogger(ObjectUtil.class);

    /**
     * 将一个对象中的字段值拷贝到另外一个对象的同名字段中，字段如果类型相同会直接拷贝，否则会通过JSON进行序列化和反序列化
     *
     * @param fromObj      被拷贝的对象
     * @param toObj        拷贝的对象
     * @param ignoreFields 要忽略的字段名
     * @return void
     * @author Huang Yongxiang
     * @date 2022/7/13 13:50
     */
    public static <T1, T2> void convertObjectFields(T1 fromObj, T2 toObj, String... ignoreFields) {
        if (fromObj == null || toObj == null) {
            throw new NullPointerException("比较属性失败");
        }
        Map<String, Object> fromMap = convertPojoToMap(fromObj, false);
        List<String> ignoreList = Arrays.asList(ignoreFields);
        Field[] toFields = getObjectFields(toObj);
        for (Field field : toFields) {
            field.setAccessible(true);
            if (ignoreList.contains(field.getName())) {
                continue;
            }
            if (fromMap.containsKey(field.getName())) {
                try {
                    if (fromMap
                            .get(field.getName())
                            .getClass() == field.getType()) {
                        field.set(toObj, fromMap.get(field.getName()));
                    } else {
                        field.set(JsonUtil.jsonStringToPojo(JsonUtil.pojoToJsonString(fromMap.get(field.getName())), field.getType()), toObj);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * 检测指定对象中是否有空字段
     *
     * @param object           对象实例
     * @param ignoreFieldsName 忽略的字段名
     * @return java.util.List<java.lang.String> 空字段的字段名
     * @author Huang Yongxiang
     * @date 2022/2/24 10:55
     */
    public static List<String> checkEmptyField(Object object, String... ignoreFieldsName) {
        if (object == null) {
            throw new NullPointerException("对象属性失败");
        }
        List<String> emptyFields = new ArrayList<>();
        try {
            for (Field field : getObjectFields(object)) {
                field.setAccessible(true);
                if (ignoreFieldsName != null && ignoreFieldsName.length > 0 && Arrays
                        .asList(ignoreFieldsName)
                        .contains(field.getName())) {
                    continue;
                }
                Object value = field.get(object);
                if (value instanceof String && StringUtils.isEmpty((String) value)) {
                    emptyFields.add(field.getName());
                } else if (value == null) {
                    emptyFields.add(field.getName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return emptyFields;
    }

    /**
     * 增强版判断对象是否为空，当对象为空或者对象属性全为空时返回true
     *
     * @param object 要判断的对象
     * @return boolean 对象为空或者对象属性全为空
     * @author Huang Yongxiang
     * @date 2022/4/12 11:04
     */
    public static boolean isNull(Object object, String... ignoreFields) {
        if (object == null) {
            return true;
        }
        Field[] fields = getObjectFields(object);
        List<String> ignoredList = null;
        if (ignoreFields != null) {
            ignoredList = Arrays.asList(ignoreFields);
        }
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                if (ignoredList != null && ignoredList.contains(field.getName())) {
                    continue;
                }
                Object value = field.get(object);
                if (value != null) {
                    return false;
                }
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    /**
     * 将一个对象实例中指定字段转为String类型
     *
     * @param object         要转化的对象实例
     * @param convertColumns 要转化的字段，不指定时将会转化Long和BigDecimal类型的字段
     * @return java.util.Map<java.lang.String, java.lang.Object> 包含已转化的字段和未转化的原字段值，不包含空字段
     * @author Huang Yongxiang
     * @date 2022/3/16 12:49
     */
    public static Map<String, Object> convertFieldsToString(Object object, String... convertColumns) {
        if (object == null) {
            throw new NullPointerException("转化对象属性失败");
        }
        Map<String, Object> convertResult = new HashMap<>();
        try {
            for (Field field : getObjectFields(object)) {
                field.setAccessible(true);
                Object value = field.get(object);
                if (value == null) {
                    continue;
                }
                if (convertColumns != null && convertColumns.length > 0 && Arrays
                        .asList(convertColumns)
                        .contains(field.getName())) {
                    convertResult.put(field.getName(), String.valueOf(value));
                } else if (convertColumns == null || convertColumns.length == 0) {
                    if (value instanceof Long || value instanceof BigDecimal) {
                        convertResult.put(field.getName(), String.valueOf(value));
                        continue;
                    }
                } else if (!Arrays
                        .asList(convertColumns)
                        .contains(field.getName())) {
                    convertResult.put(field.getName(), value);
                    continue;
                }
                if (!(value instanceof Collection) && !(value instanceof Number) && !(value instanceof String) && !(value instanceof Boolean) && !(value instanceof Date)) {
                    convertResult.put(field.getName(), convertFieldsToString(value, convertColumns));
                } else {
                    convertResult.put(field.getName(), value);
                }
            }
            return convertResult;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Map<String, Object> convertFieldsToStringByClass(Object object, Class<?>... convertColumns) {
        if (object == null) {
            throw new NullPointerException("转化对象属性失败");
        }
        Map<String, Object> convertResult = new HashMap<>();
        try {
            for (Field field : getObjectFields(object)) {
                field.setAccessible(true);
                Object value = field.get(object);
                if (value == null) {
                    continue;
                }
                if (convertColumns == null || convertColumns.length == 0) {
                    if (value instanceof Long || value instanceof BigDecimal) {
                        convertResult.put(field.getName(), String.valueOf(value));
                        continue;
                    }
                } else if (Arrays
                        .asList(convertColumns)
                        .contains(value.getClass())) {
                    convertResult.put(field.getName(), String.valueOf(value));
                    continue;
                }
                if (!(value instanceof Collection) && !(value instanceof Number) && !(value instanceof String) && !(value instanceof Boolean) && !(value instanceof Date)) {
                    convertResult.put(field.getName(), convertFieldsToStringByClass(value, convertColumns));
                } else {
                    convertResult.put(field.getName(), value);
                }
            }
            return convertResult;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Field[] getObjectFields(Object object) {
        if (object == null) {
            return new Field[]{};
        }
        return getClassFields(object.getClass());
    }

    public static Field[] getClassFields(Class<?> clazz) {
        if (clazz == null) {
            return new Field[]{};
        }
        List<Field> fields = new ArrayList<>();
        for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
        }
        return fields.toArray(new Field[]{});
    }

    /**
     * 将一个对象实例转为map对象
     *
     * @param object        对象实例
     * @param isUnCamelCase 对应实例的属性名是否将驼峰命名转为下划线命名
     * @return java.util.Map<java.lang.String, java.lang.Object>
     * @author Huang Yongxiang
     * @date 2022/3/27 11:32
     */
    public static Map<String, Object> convertPojoToMap(Object object, boolean isUnCamelCase, String... ignoreFieldName) {
        if (object == null) {
            return null;
        }
        Map<String, Object> result = new HashMap<>();
        for (Field field : getObjectFields(object)) {
            field.setAccessible(true);
            if (ignoreFieldName != null && ignoreFieldName.length > 0 && Arrays
                    .stream(ignoreFieldName)
                    .anyMatch(str -> field
                            .getName()
                            .equals(str))) {
                continue;
            }
            String key = isUnCamelCase ? StringUtils.uncamelCase(field.getName()) : field.getName();
            try {
                result.put(key, field.get(object));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static <T> T getClassInstance(Class<T> clazz) {
        if (clazz != null) {
            try {
                return clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException ignored) {
            }
        }
        return null;
    }

    public static Object getClassInstanceObject(Class<?> clazz) {
        if (clazz != null) {
            try {
                return clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException ignored) {
            }
        }
        return null;
    }

    public static Class<?> classPath2Class(String className) {
        if (StringUtils.isEmpty(className)) {
            return null;
        }
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ignored) {
            logger.error("没有指定类：{}", className);
        }
        return null;
    }

    public static String getSimpleMethodReferencePath(String methodName, Class<?> methodClass) {
        return getSimpleMethodReferencePath(methodName, methodClass.getName());
    }

    public static String getSimpleMethodReferencePath(String methodName, String methodClassName) {
        return String.format("%s.%s", methodClassName, methodName);
    }

    public static <T> Set<Class<? extends T>> getInterfaceImplementation(Class<T> clazz, String... packagePattern) {
        Reflections reflections = new Reflections((Object) packagePattern);
        return reflections.getSubTypesOf(clazz);
    }


    /**
     * 通过名称找到匹配的方法，多个重名方法默认返回第一个
     *
     * @param methodName  要查找的方法名
     * @param methodClass 方法所在的类对象
     * @return java.lang.reflect.Method
     * @author Huang Yongxiang
     * @date 2022/7/1 14:00
     */
    public static Method findMethod(String methodName, Class<?> methodClass) {
        Method[] methods = methodClass.getMethods();
        for (Method method : methods) {
            if (method
                    .getName()
                    .equals(methodName)) {
                return method;
            }
        }
        return null;
    }

    public static Method findMethod(String methodName, Class<?> methodClass, Class<?>... paramsType) {
        try {
            return methodClass.getMethod(methodName, paramsType);
        } catch (NoSuchMethodException e) {
            logger.error("没有找到指定方法：{}", methodName);
        }
        return null;
    }

    public static Method findMethod(String methodName, Object[] attributes, Class<?> methodClass) {
        if (attributes == null) {
            return findMethod(methodName, methodClass);
        }
        Class<?>[] classes = Arrays
                .stream(attributes)
                .map(Object::getClass)
                .collect(Collectors.toList())
                .toArray(new Class[]{});
        try {
            return methodClass.getMethod(methodName, classes);
        } catch (NoSuchMethodException e) {
            logger.error("没有找到指定方法：{}", methodName);
        }
        return null;
    }

    public static Method findMethod(String methodName, Object[] attributes, Object methodObject) {
        if (methodObject == null) {
            return null;
        }
        return findMethod(methodName, attributes, methodObject.getClass());
    }

    /**
     * 比较两个对象的属性，并生成一个合并对象，合并规则：当obj1中的属性不为空且和obj2中属性不同时使用obj1的，否则使用obj2的
     *
     * @param object1          修改对象
     * @param object2          待修改对象
     * @param ignoreFieldsName 忽略的字段名
     * @return T 合并后的对象
     * @author Huang Yongxiang
     * @date 2022/1/26 11:10
     */
    public static <T> T mergeObject(T object1, T object2, String... ignoreFieldsName) {
        if (object1 == null || object2 == null) {
            throw new NullPointerException("比较属性失败");
        }
        if (!object1
                .getClass()
                .equals(object2.getClass())) {
            throw new IllegalArgumentException("对象类型不同，无法比较");
        }

        try {
            Field[] fields = getObjectFields(object1);
            for (Field field : fields) {
                field.setAccessible(true);
                Object value1 = field.get(object1);
                Object value2 = field.get(object2);
                if (ignoreFieldsName != null && ignoreFieldsName.length > 0 && Arrays
                        .asList(ignoreFieldsName)
                        .contains(field.getName())) {
                    continue;
                }
                if (value1 != null && !value1.equals(value2)) {
                    field.set(object2, value1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return object2;
    }
}
