package com.jingge.autojob.skeleton.framework.network.server;

import com.jingge.autojob.util.servlet.InetUtil;
import com.jingge.autojob.util.thread.ScheduleTaskUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * RPC服务端
 *
 * @Author Huang Yongxiang
 * @Date 2022/09/17 17:07
 */
@Slf4j
public class RPCServer {
    private final int port;
    private volatile boolean isStart = false;
    private final ScheduleTaskUtil serverThread = ScheduleTaskUtil.build(true, "RPCServerRunThread");

    public RPCServer(int port) {
        this.port = port;
    }

    public void startServer() {
        if (isStart) {
            throw new IllegalStateException("RPC Server已启动");
        }
        serverThread.EOneTimeTask(() -> {
            log.info("RPC server beginning");
            EventLoopGroup boss = new NioEventLoopGroup();
            EventLoopGroup worker = new NioEventLoopGroup();
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap
                    .group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new RPCServerInitializer());
            try {
                String host = InetUtil.getLocalhostIp();
                ChannelFuture future = bootstrap
                        .bind(host, this.port)
                        .sync();
                log.info("RPC server start success on port {}, ip {}", port, host);
                isStart = true;
                future
                        .channel()
                        .closeFuture()
                        .sync();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                boss.shutdownGracefully();
                worker.shutdownGracefully();
            }
            return null;
        }, 0, TimeUnit.MILLISECONDS);
    }

    public boolean isStart() {
        return isStart;
    }
}
