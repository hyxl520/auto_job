package com.jingge.autojob.skeleton.framework.task;

/**
 * 分片策略枚举
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-08-02 16:19
 * @email 1158055613@qq.com
 */
public enum ShardingStrategyEnum {
    NUMERICAL(new NumericalShardingStrategy());

    private final ShardingStrategy shardingStrategy;

    ShardingStrategyEnum(ShardingStrategy shardingStrategy) {
        this.shardingStrategy = shardingStrategy;
    }

    public ShardingStrategy getShardingStrategy() {
        return shardingStrategy;
    }
}
