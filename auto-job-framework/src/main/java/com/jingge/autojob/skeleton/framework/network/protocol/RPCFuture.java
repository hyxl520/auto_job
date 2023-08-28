package com.jingge.autojob.skeleton.framework.network.protocol;

import io.netty.util.concurrent.Promise;
import lombok.Getter;

import java.io.Serializable;

/**
 * 异步请求结果
 *
 * @Author Huang Yongxiang
 * @Date 2022/09/19 15:35
 */
@Getter
public class RPCFuture implements Serializable {
    private final Promise<RPCResponse> promise;

    public RPCFuture(Promise<RPCResponse> promise) {
        this.promise = promise;
    }
}
