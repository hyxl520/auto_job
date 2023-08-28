package com.jingge.autojob.skeleton.framework.config;

import java.io.Serializable;

/**
 * 声明某个配置类是可以被存储的
 *
 * @author Huang Yongxiang
 * @date 2023-01-04 15:09
 * @email 1158055613@qq.com
 */
public interface StorableConfig extends Serializable {
    ConfigSerializer getSerializer();

    default Long getTaskId() {
        return null;
    }

    default void beforeSerialize() {
    }

    default void afterSerialize() {
    }
}
