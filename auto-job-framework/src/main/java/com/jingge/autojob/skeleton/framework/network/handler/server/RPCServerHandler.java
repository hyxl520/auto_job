package com.jingge.autojob.skeleton.framework.network.handler.server;

import com.jingge.autojob.skeleton.framework.network.enums.ReqType;
import com.jingge.autojob.skeleton.framework.network.enums.ResponseCode;
import com.jingge.autojob.skeleton.framework.network.protocol.RPCHeader;
import com.jingge.autojob.skeleton.framework.network.protocol.RPCProtocol;
import com.jingge.autojob.skeleton.framework.network.protocol.RPCRequest;
import com.jingge.autojob.skeleton.framework.network.protocol.RPCResponse;
import com.jingge.autojob.skeleton.framework.pool.AbstractAutoJobPool;
import com.jingge.autojob.util.thread.FlowThreadPoolExecutorHelper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 服务端消息处理程序
 *
 * @Author Huang Yongxiang
 * @Date 2022/09/17 17:39
 */
@Slf4j
public class RPCServerHandler extends SimpleChannelInboundHandler<RPCProtocol<RPCRequest>> {
    private final FlowThreadPoolExecutorHelper executorHelper = FlowThreadPoolExecutorHelper
            .builder()
            .setAllowMaxCoreThreadCount(20)
            .setAllowMinCoreThreadCount(1)
            .setAllowMaxResponseTime(3)
            .setCostTimePerTask(1)
            .setMaxTaskCountPerSecond(10)
            .setMinTaskCountPerSecond(1)
            .setEightyPercentTaskCountPerSecond(10)
            .setAllowMinThreadCount(5)
            .setTrafficListenerCycle(10)
            .setThreadFactory(new AbstractAutoJobPool.NamedThreadFactory("RPCServerHandler"))
            .setAllowUpdate(true)
            .build();

    public RPCServerHandler() {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RPCProtocol<RPCRequest> msg) throws Exception {
        RPCServiceHandler handler = new RPCServiceHandler(msg);
        RPCProtocol<RPCResponse> response = new RPCProtocol<>();

        RPCResponse result = null;
        if (handler.withReturn()) {
            result = executorHelper
                    .submit(handler)
                    .get();
        } else {
            executorHelper.submit(handler);
            result = new RPCResponse();
            result.setCode(ResponseCode.SUCCESS.code);
            result.setReturnType(void.class);
            result.setMessage("请求成功");
        }
        RPCHeader header = new RPCHeader(ReqType.RESPONSE);
        header.setHandleTime(System.currentTimeMillis() - msg
                .getHeader()
                .getHandleTime());
        header.setReqId(msg
                .getHeader()
                .getReqId());
        response.setContent(result);
        response.setHeader(header);
        ctx.writeAndFlush(response);
    }
}
