package com.jingge.autojob.util.json;

import com.google.gson.*;

import java.lang.reflect.Type;

/**
 * @Author Huang Yongxiang
 * @Date 2022/09/09 18:17
 */
public class ClassCodec implements JsonSerializer<Class<?>>, JsonDeserializer<Class<?>> {
    @Override
    public Class<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            String str = json.getAsString();
            return Class.forName(str);
        } catch (ClassNotFoundException e) {
            throw new JsonParseException(e);
        }
    }

    @Override
    public JsonElement serialize(Class<?> src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.getName());
    }
}
