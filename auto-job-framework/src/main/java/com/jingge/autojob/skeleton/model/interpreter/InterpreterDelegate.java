package com.jingge.autojob.skeleton.model.interpreter;

import com.jingge.autojob.util.convert.DefaultValueUtil;
import com.jingge.autojob.util.convert.StringUtils;
import com.jingge.autojob.util.json.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @Description 解释器委派者
 * @Author Huang Yongxiang
 * @Date 2022/07/06 16:25
 */
@Slf4j
public class InterpreterDelegate {
    public static Attribute interpreter(JsonObject o, int pos, String type) {
        if (o == null || !o.has("type") || !o.has("values")) {
            return null;
        }
        String typeString = DefaultValueUtil.defaultStringWhenEmpty(type, o
                .get("type")
                .getAsString()
                .trim());
        IExpression typeExpression = new TypeInterpreter(typeString);
        switch (typeString) {
            case "string": {
                return (Attribute) new StringInterpreter(typeExpression, o
                        .get("values")
                        .getAsJsonObject()
                        .get("value")
                        .getAsString(), pos).interpreter();
            }
            case "decimal": {
                return (Attribute) new DecimalInterpreter(typeExpression, o
                        .get("values")
                        .getAsJsonObject()
                        .get("value")
                        .getAsString(), pos).interpreter();
            }
            case "integer":
            case "long": {
                return (Attribute) new IntegerInterpreter(typeExpression, o
                        .get("values")
                        .getAsJsonObject()
                        .get("value")
                        .getAsString(), pos).interpreter();
            }
            case "boolean": {
                return (Attribute) new BooleanInterpreter(typeExpression, o
                        .get("values")
                        .getAsJsonObject()
                        .get("value")
                        .getAsString(), pos).interpreter();
            }
            default: {
                return (Attribute) new ObjectInterpreter(typeExpression, o
                        .get("values")
                        .toString(), pos).interpreter();
            }
        }
    }

    public static List<Attribute> convertAttributeString(String attributeString) {
        if (StringUtils.isEmpty(attributeString)) {
            return null;
        }
        JsonArray attributes = JsonUtil.stringToJsonArray(attributeString);
        List<Attribute> attributeList = new LinkedList<>();
        try {
            for (int i = 0; i < attributes.size(); i++) {
                Attribute attribute = interpreter(attributes
                        .get(i)
                        .getAsJsonObject(), i, null);
                if (attribute != null) {
                    attributeList.add(attribute);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return attributeList;
    }

    public static List<Attribute> convertAttributeString(Method target, String attributeString) {
        if (target == null) {
            return convertAttributeString(attributeString);
        }
        Class<?>[] attributeClass = target.getParameterTypes();
        JsonArray attributes = JsonUtil.stringToJsonArray(attributeString);
        if (attributeClass.length != attributes.size()) {
            log.error("获取参数失败，参数列表个数不匹配");
            return Collections.emptyList();
        }
        List<Attribute> attributeList = new LinkedList<>();
        String type = null;
        try {
            for (int i = 0; i < attributes.size(); i++) {
                if (isObjectType(attributes
                        .get(i)
                        .getAsJsonObject())) {
                    type = attributeClass[i].getName();
                } else {
                    type = null;
                }
                Attribute attribute = interpreter(attributes
                        .get(i)
                        .getAsJsonObject(), i, type);
                if (attribute != null) {
                    attributeList.add(attribute);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return attributeList;
    }

    private static boolean isObjectType(JsonObject o) {
        return "object".equals(o
                .get("type")
                .getAsString()
                .trim()
                .toLowerCase());
    }

}
