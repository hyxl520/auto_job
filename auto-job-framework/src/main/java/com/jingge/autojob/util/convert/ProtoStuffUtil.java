package com.jingge.autojob.util.convert;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-03-06 15:04
 * @email 1158055613@qq.com
 */
public class ProtoStuffUtil {
    private static final ThreadLocal<LinkedBuffer> threadSafeBuffer = new ThreadLocal<>();
    private static final Map<Class<?>, Schema<?>> schemaMap = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> byte[] serialize(T source) {
        if (source == null) {
            return null;
        }
        Class<T> clazz = (Class<T>) source.getClass();
        Schema<T> schema = getSchema(clazz);
        if (schema == null) {
            throw new NullPointerException("无法获取" + clazz + "的模式");
        }
        try {
            if (threadSafeBuffer.get() == null) {
                threadSafeBuffer.set(LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));
            }
            return ProtostuffIOUtil.toByteArray(source, schema, threadSafeBuffer.get());
        } finally {
            threadSafeBuffer
                    .get()
                    .clear();
        }
    }

    public static <T> T deserialize(byte[] data, Class<T> type) {
        if (data == null || data.length == 0) {
            return null;
        }
        Schema<T> schema = getSchema(type);
        if (schema == null) {
            throw new NullPointerException("无法获取" + type + "的模式");
        }
        T source = schema.newMessage();
        ProtostuffIOUtil.mergeFrom(data, source, schema);
        return source;
    }

    @SuppressWarnings("unchecked")
    public static <T> T cloneObject(T object) {
        if (object == null) {
            return null;
        }
        byte[] source = serialize(object);
        return (T) deserialize(source,object.getClass());
    }

    @SuppressWarnings("unchecked")
    private static <T> Schema<T> getSchema(Class<T> clazz) {
        Schema<T> schema = (Schema<T>) schemaMap.get(clazz);
        if (schema == null) {
            schema = RuntimeSchema.getSchema(clazz);
            schemaMap.put(clazz, schema);
        }
        return schema;
    }


}

