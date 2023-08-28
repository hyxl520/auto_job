package com.jingge.autojob.skeleton.framework.config;

import com.jingge.autojob.skeleton.db.entity.AutoJobConfigEntity;
import com.jingge.autojob.util.bean.ObjectUtil;
import com.jingge.autojob.util.convert.StringUtils;
import com.jingge.autojob.util.id.IdGenerator;
import com.jingge.autojob.util.json.JsonUtil;

import java.sql.Timestamp;

/**
 * 基于JSON的配置序列化和反序列化器
 *
 * @author Huang Yongxiang
 * @date 2023-01-04 15:42
 * @email 1158055613@qq.com
 */
public class ConfigJsonSerializerAndDeserializer implements ConfigSerializer, ConfigDeserializer {
    @Override
    public String deserializationType() {
        return "json";
    }

    @Override
    public StorableConfig deserialize(AutoJobConfigEntity entity) {
        if (entity == null || StringUtils.isEmpty(entity.getContent()) || StringUtils.isEmpty(entity.getContentType())) {
            return null;
        }
        if (!deserializationType().equalsIgnoreCase(entity.getSerializationType())) {
            throw new UnsupportedOperationException("不支持" + entity.getContentType() + "类型的反序列化");
        }
        Class<?> clazz = ObjectUtil.classPath2Class(entity.getContentType());
        if (clazz == null || !StorableConfig.class.isAssignableFrom(clazz)) {
            throw new UnsupportedOperationException("所要反序列化的类型为空或不支持：" + clazz);
        }
        return (StorableConfig) JsonUtil.jsonStringToPojo(entity.getContent(), clazz);
    }

    @Override
    public String serializationType() {
        return "json";
    }

    @Override
    public AutoJobConfigEntity serialize(StorableConfig config) {
        AutoJobConfigEntity entity = new AutoJobConfigEntity();
        entity.setContent(JsonUtil.pojoToJsonString(config));
        entity.setContentType(config
                .getClass()
                .getName());
        entity.setCreateTime(new Timestamp(System.currentTimeMillis()));
        entity.setWriteTimestamp(System.currentTimeMillis());
        entity.setDelFlag(0);
        entity.setTaskId(config.getTaskId());
        entity.setId(IdGenerator.getNextIdAsLong());
        entity.setStatus(1);
        entity.setSerializationType(serializationType());
        return entity;
    }

}
