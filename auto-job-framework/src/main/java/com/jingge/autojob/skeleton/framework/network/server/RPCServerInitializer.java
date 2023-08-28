package com.jingge.autojob.skeleton.framework.network.server;

import com.jingge.autojob.skeleton.framework.network.codec.RPCDecoder;
import com.jingge.autojob.skeleton.framework.network.codec.RPCEncoder;
import com.jingge.autojob.skeleton.framework.network.handler.server.RPCServerHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

/**
 * RPC服务初始化程序
 *
 * @Author Huang Yongxiang
 * @Date 2022/09/17 17:09
 */
public class RPCServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new RPCDecoder())
                .addLast(new RPCEncoder())
                .addLast(new RPCServerHandler());
    }
}
