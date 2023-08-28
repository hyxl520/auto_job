package com.jingge.autojob.skeleton.framework.config;

import com.jingge.autojob.skeleton.model.handler.AutoJobRetryStrategy;
import com.jingge.autojob.skeleton.model.handler.FailoverStrategy;
import com.jingge.autojob.skeleton.model.handler.LocalRetryStrategy;

/**
 * 重试策略
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-07-24 17:08
 * @email 1158055613@qq.com
 */
public enum RetryStrategy {
    LOCAL_RETRY{
        @Override
        public AutoJobRetryStrategy getRetryStrategy() {
            return LOCAL_RETRY_STRATEGY;
        }
    }, FAILOVER{
        @Override
        public AutoJobRetryStrategy getRetryStrategy() {
            return FAILOVER_STRATEGY;
        }
    };

    private static final AutoJobRetryStrategy LOCAL_RETRY_STRATEGY=new LocalRetryStrategy();
    private static final AutoJobRetryStrategy FAILOVER_STRATEGY=new FailoverStrategy();

    public AutoJobRetryStrategy getRetryStrategy(){
        throw new UnsupportedOperationException();
    }

    public static RetryStrategy findByName(String name) {
        for (RetryStrategy s : values()) {
            if (s
                    .name()
                    .equalsIgnoreCase(name)) {
                return s;
            }
        }
        return null;
    }
}
