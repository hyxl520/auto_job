package com.jingge.autojob.skeleton.framework.network.client;

import com.jingge.autojob.skeleton.framework.network.codec.RPCDecoder;
import com.jingge.autojob.skeleton.framework.network.codec.RPCEncoder;
import com.jingge.autojob.skeleton.framework.network.handler.client.RPCClientHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LoggingHandler;


/**
 * @Author Huang Yongxiang
 * @Date 2022/09/19 15:28
 */
public class RpcClientInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline()
                .addLast(new LoggingHandler())
                .addLast(new RPCEncoder())
                .addLast(new RPCDecoder())
                .addLast(new RPCClientHandler());
    }
}
