package com.jingge.autojob.skeleton.framework.config;

import com.jingge.autojob.skeleton.db.entity.AutoJobConfigEntity;
import com.jingge.autojob.skeleton.lang.Serializer;

/**
 * 配置序列化器
 *
 * @author Huang Yongxiang
 * @date 2023-01-04 15:12
 * @email 1158055613@qq.com
 */
public interface ConfigSerializer extends Serializer<AutoJobConfigEntity, StorableConfig> {
    String serializationType();

    @Override
    AutoJobConfigEntity serialize(StorableConfig config);
}
