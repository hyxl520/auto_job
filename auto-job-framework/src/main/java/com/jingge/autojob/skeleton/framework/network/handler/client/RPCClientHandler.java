package com.jingge.autojob.skeleton.framework.network.handler.client;

import com.jingge.autojob.skeleton.framework.network.protocol.RPCFuture;
import com.jingge.autojob.skeleton.framework.network.protocol.RPCProtocol;
import com.jingge.autojob.skeleton.framework.network.protocol.RPCResponse;
import com.jingge.autojob.skeleton.framework.network.protocol.RequestFutureContainer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * 响应处理器
 *
 * @Author Huang Yongxiang
 * @Date 2022/09/19 15:38
 */
public class RPCClientHandler extends SimpleChannelInboundHandler<RPCProtocol<RPCResponse>> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RPCProtocol<RPCResponse> msg) {
        long reqId = msg.getHeader().getReqId();
        RPCFuture future = RequestFutureContainer.removeAndGet(reqId);
        if (future != null) {
            if (msg.getContent().isSuccess()) {
                future.getPromise().setSuccess(msg.getContent());
            } else {
                future.getPromise().setFailure(new RequestFailedException(msg.getContent().getMessage()));
            }
        }
    }
}
