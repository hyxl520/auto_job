package com.jingge.autojob.skeleton.annotation;


import java.lang.annotation.*;

/**
 * 分片配置
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-08-02 16:17
 * @email 1158055613@qq.com
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface ShardingConfig {
    /**
     * 是否启用分片
     */
    boolean enable() default false;

    /**
     * 总分片数
     */
    long total() default 0L;

    /**
     * 是否允许分片进行重试
     */
    boolean enableShardingRetry() default false;
}
