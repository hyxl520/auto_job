package com.jingge.autojob.skeleton.model.builder;

import com.jingge.autojob.util.json.JsonUtil;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 参数构建类，通过该类可以构造完整的方法参数字符串，注意单例下并不是线程安全的
 *
 * @Author Huang Yongxiang
 * @Date 2022/04/14 11:33
 */
public class AttributesBuilder {
    List<Map<String, Object>> attributeHolder;

    public AttributesBuilder() {
        attributeHolder = new LinkedList<>();
    }

    public static AttributesBuilder getInstance() {
        return InstanceHolder.builder;
    }

    public enum AttributesType {
        /**
         * 字符串类型
         */
        STRING("string"),
        /**
         * 浮点数类型，对应java的Double包装类型
         */
        DECIMAL("decimal"),
        /**
         * 整数类型，对应java的Integer包装类型
         */
        INTEGER("integer"),
        /**
         * 长整型，对于java的Long包装类型
         */
        LONG("long"),
        /**
         * 布尔类型，对应java的Boolean包装类型
         */
        BOOLEAN("boolean");
        private final String value;

        AttributesType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static Map<String, Object> getValue(AttributesType attributesType, Object values) {
        if (attributesType == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("type", attributesType.value);
        Map<String, Object> value = new HashMap<>();
        value.put("value", values);
        map.put("values", value);
        return map;
    }

    public static Map<String, Object> getValue(Class<?> attributesObjectType, Object values) {
        if (attributesObjectType == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("type", attributesObjectType.getName());
        map.put("values", values);
        return map;
    }

    public void clear() {
        attributeHolder.clear();
        System.gc();
    }

    public AttributesBuilder addParams(AttributesType attributesType, Object values) {
        attributeHolder.add(getValue(attributesType, values));
        return this;
    }

    public AttributesBuilder addParams(Class<?> type, Object values) {
        attributeHolder.add(getValue(type, values));
        return this;
    }

    public String getAttributesString() {
        return JsonUtil.getJson(attributeHolder);
    }

    private static class InstanceHolder {
        private static final AttributesBuilder builder = new AttributesBuilder();
    }

    public static void main(String[] argc) {
        System.out.println(getInstance().addParams(AttributesType.LONG, 123).addParams(AttributesType.STRING, "cronExpression").getAttributesString());
    }


}
