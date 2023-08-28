package com.jingge.autojob.skeleton.framework.config;

import com.jingge.autojob.skeleton.db.entity.AutoJobConfigEntity;
import com.jingge.autojob.skeleton.lang.Deserializer;

/**
 * 配置反序列化器
 *
 * @author Huang Yongxiang
 * @date 2023-01-04 15:12
 * @email 1158055613@qq.com
 */
public interface ConfigDeserializer extends Deserializer<StorableConfig, AutoJobConfigEntity> {
    String deserializationType();

    @Override
    StorableConfig deserialize(AutoJobConfigEntity entity);
}
