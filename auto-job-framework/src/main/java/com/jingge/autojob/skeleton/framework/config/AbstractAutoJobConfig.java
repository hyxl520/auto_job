package com.jingge.autojob.skeleton.framework.config;

import com.jingge.autojob.skeleton.db.entity.AutoJobConfigEntity;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.util.convert.DefaultValueUtil;
import com.jingge.autojob.util.io.PropertiesHolder;

import java.io.Serializable;

/**
 * 抽象配置器
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/12 14:17
 */
public abstract class AbstractAutoJobConfig implements Serializable {
    protected transient PropertiesHolder propertiesHolder;

    public AbstractAutoJobConfig(PropertiesHolder propertiesHolder) {
        this.propertiesHolder = propertiesHolder;
    }

    public AbstractAutoJobConfig() {
        if (AutoJobApplication
                .getInstance()
                .isNoCreated()) {
            this.propertiesHolder = PropertiesHolder
                    .builder()
                    .build();
        } else {
            this.propertiesHolder = DefaultValueUtil.defaultValue(AutoJobApplication
                    .getInstance()
                    .getConfigHolder()
                    .getPropertiesHolder(), PropertiesHolder
                    .builder()
                    .addPropertiesFile("auto-job.yml")
                    .addPropertiesFile("application.properties")
                    .build());
        }
    }

    public static StorableConfig deserialize(ConfigDeserializer deserializer, AutoJobConfigEntity entity) {
        if (deserializer == null) {
            throw new NullPointerException();
        }
        if (entity == null) {
            return null;
        }
        return deserializer.deserialize(entity);
    }
}
