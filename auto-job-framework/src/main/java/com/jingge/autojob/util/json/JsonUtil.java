package com.jingge.autojob.util.json;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * json的工具类，可以静态调用和生成实例调用，静态调用使用默认的机制
 *
 * @author Huang Yongxiang
 */
public class JsonUtil {
    private static final Gson gson = getGson();

    /**
     * 构建默认的Gson对象
     *
     * @return com.google.gson.Gson
     * @author Huang Yongxiang
     * @date 2022/1/4 15:03
     */
    private static Gson getGson() {
        return new GsonBuilder()
                .enableComplexMapKeySerialization()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .registerTypeAdapter(Class.class, new ClassCodec())
                .create();
    }

    /**
     * 把list转为json字符串
     *
     * @param list
     * @return java.lang.String
     * @author Huang Yongxiang
     * @date 2021/8/11 9:05
     */
    public static String getJson(List<?> list) {
        return getGson().toJson(list);
    }

    /**
     * @param map 数据
     * @return java.lang.String
     * @author Huang Yongxiang
     * @date 2021/7/28 16:26
     */
    public static String getMapJson(Map<String, Object> map) {
        return getGson().toJson(map);
    }


    /**
     * 将map转为JSON对象
     *
     * @param map Map
     * @return com.google.gson.JsonObject
     * @author Huang Yongxiang
     * @date 2021/10/15 9:09
     */
    public static JsonObject mapToJsonObj(Map<String, Object> map) {
        return getGson().fromJson(getGson().toJson(map), JsonObject.class);
    }


    /**
     * 将json对象转为pojo
     *
     * @param jsonString json字符串
     * @param clazz      要转为的pojo类型
     * @return T
     * @author Huang Yongxiang
     * @date 2021/12/22 10:57
     */
    public static <T> T jsonStringToPojo(String jsonString, Class<T> clazz) {
        return getGson().fromJson(jsonString, clazz);
    }

    public static <T> List<T> jsonStringToList(String json, Class<T> clazz) {
        return getGson().fromJson(json, TypeToken
                .getParameterized(List.class, clazz)
                .getType());
    }

    public static <K, V> Map<K, V> jsonStringToMap(String json, Class<K> kClazz, Class<V> vClazz) {
        return getGson().fromJson(json, TypeToken
                .getParameterized(Map.class, kClazz, vClazz)
                .getType());
    }


    /**
     * 把json对象转为Map
     *
     * @param json json对象
     * @return java.util.Map<java.lang.String, com.google.gson.JsonElement>
     * @author Huang Yongxiang
     * @date 2021/8/11 9:19
     */
    public static Map<String, JsonElement> jsonObjToMap(JsonObject json) {
        if (json != null) {
            Map<String, JsonElement> map = new HashMap<>();
            for (String key : json.keySet()) {
                map.put(key, json.get(key));
            }
            return map;
        }
        return null;
    }

    public static Map<String, String> jsonObjToStringMap(JsonObject json) {
        if (json != null) {
            Map<String, String> map = new HashMap<>();
            for (String key : json.keySet()) {
                if (json.get(key) instanceof JsonArray) {
                    map.put(key, json
                            .get(key)
                            .toString());
                } else {
                    map.put(key, json
                            .get(key)
                            .getAsString());
                }
            }
            return map;
        }
        return null;
    }


    /**
     * 字符串转为json对象
     *
     * @param json
     * @return com.google.gson.JsonObject
     * @author Huang Yongxiang
     * @date 2021/8/11 9:19
     */
    public static JsonObject stringToJsonObj(String json) {
        return getGson().fromJson(json, JsonObject.class);
    }


    /**
     * @param json json字符串
     * @return com.google.gson.JsonArray
     * @author Huang Yongxiang
     * @date 2021/7/30 14:20
     */
    public static JsonArray stringToJsonArray(String json) {
        return getGson().fromJson(json, JsonArray.class);

    }

    /**
     * @param pojo pojo对象
     * @return java.lang.String
     * @author Huang Yongxiang
     * @date 2021/7/30 14:11
     */
    public static String pojoToJsonString(Object pojo) {
        return getGson().toJson(pojo);
    }
}
