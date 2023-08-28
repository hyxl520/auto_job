package com.jingge.autojob.skeleton.framework.config;

import com.jingge.autojob.skeleton.db.entity.AutoJobConfigEntity;
import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 配置存储库
 *
 * @author Huang Yongxiang
 * @date 2023-01-05 10:09
 * @email 1158055613@qq.com
 */
public class ConfigRepository {
    public static int storageConfigs(List<StorableConfig> configs) {
        if (configs == null || configs.size() == 0) {
            return 0;
        }
        List<AutoJobConfigEntity> entities = configs
                .stream()
                .filter(Objects::nonNull)
                .map(config -> {
                    try {
                        config.beforeSerialize();
                        return config
                                .getSerializer()
                                .serialize(config);
                    } finally {
                        config.afterSerialize();
                    }
                })
                .collect(Collectors.toList());
        return AutoJobMapperHolder.CONFIG_ENTITY_MAPPER.insertList(entities);
    }

    public static boolean storageConfig(StorableConfig config) {
        if (config == null) {
            return true;
        }
        try {
            config.beforeSerialize();
            AutoJobConfigEntity entity = config
                    .getSerializer()
                    .serialize(config);
            return AutoJobMapperHolder.CONFIG_ENTITY_MAPPER.insertList(Collections.singletonList(entity)) == 1;
        } finally {
            config.afterSerialize();
        }
    }
}
