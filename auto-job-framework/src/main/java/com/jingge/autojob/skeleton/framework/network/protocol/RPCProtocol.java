package com.jingge.autojob.skeleton.framework.network.protocol;

import com.jingge.autojob.skeleton.framework.network.enums.ReqType;
import lombok.Data;

/**
 * RPC协议对象
 *
 * @Author Huang Yongxiang
 * @Date 2022/09/09 16:48
 */
@Data
public class RPCProtocol<T> {
    private RPCHeader header;
    private T content;

    public RPCProtocol(T content, ReqType type) {
        this.content = content;
        this.header = new RPCHeader(type);
    }

    public RPCProtocol() {
    }
}
