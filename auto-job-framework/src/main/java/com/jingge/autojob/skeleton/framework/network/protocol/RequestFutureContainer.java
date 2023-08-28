package com.jingge.autojob.skeleton.framework.network.protocol;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 异步请求结果持有者
 *
 * @Author Huang Yongxiang
 * @Date 2022/09/19 15:44
 */
public class RequestFutureContainer {
    private static final Map<Long, RPCFuture> FUTURE_MAP = new ConcurrentHashMap<>();

    public static boolean insert(long reqId, RPCFuture future) {
        if (FUTURE_MAP.containsKey(reqId)) {
            return false;
        }
        FUTURE_MAP.put(reqId, future);
        return true;
    }

    public static boolean isExist(long reqId) {
        return FUTURE_MAP.containsKey(reqId);
    }

    public static RPCFuture removeAndGet(long reqId) {
        return FUTURE_MAP.remove(reqId);
    }

    public static RPCFuture get(long reqId) {
        return FUTURE_MAP.get(reqId);
    }
}
