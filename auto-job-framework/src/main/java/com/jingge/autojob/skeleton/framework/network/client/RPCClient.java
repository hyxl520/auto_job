package com.jingge.autojob.skeleton.framework.network.client;

import com.jingge.autojob.skeleton.framework.network.enums.ReqType;
import com.jingge.autojob.skeleton.framework.network.handler.client.ConnectTimeoutException;
import com.jingge.autojob.skeleton.framework.network.handler.client.ConnectionClosedException;
import com.jingge.autojob.skeleton.framework.network.handler.client.ResponseTimeoutException;
import com.jingge.autojob.skeleton.framework.network.protocol.*;
import com.jingge.autojob.skeleton.framework.network.protocol.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * RPC客户端
 *
 * @Author Huang Yongxiang
 * @Date 2022/09/19 15:24
 */
@Slf4j
public class RPCClient implements AutoCloseable {
    private final Bootstrap bootstrap;
    private Channel channel;
    private final long timeout;
    private final long getDataTimeout;
    private final Object lock = new Object();
    private static final int CREATED = 1;
    private static final int USING = 2;
    private static final int DESTROYED = 3;
    private volatile int status = CREATED;


    public RPCClient() {
        this.bootstrap = new Bootstrap();
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        bootstrap
                .group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new RpcClientInitializer());
        this.timeout = 5000;
        this.getDataTimeout = 5000;
    }

    public RPCClient(Bootstrap bootstrap, long connectTimeout, long getDataTimeout, TimeUnit unit) {
        this.bootstrap = bootstrap;
        this.timeout = unit.toMillis(connectTimeout);
        this.getDataTimeout = getDataTimeout;
    }

    /**
     * 支持超时的建立连接
     *
     * @return io.netty.channel.Channel
     * @author Huang Yongxiang
     * @date 2022/9/20 10:38
     */
    private Channel getChannel(String host, int port) {
        try {
            final ChannelFuture future = bootstrap.connect(host, port);
            future.addListener(listener -> {
                if (future.isSuccess()) {
                    log.debug("connect rpc server {} success.", host);
                } else {
                    log.error("connect rpc server {} failed. ", host);
                    future
                            .cause()
                            .printStackTrace();
                }
            });
            if (future.await(timeout, TimeUnit.MILLISECONDS)) {
                return future.channel();
            } else {
                throw new ConnectTimeoutException();
            }
        } catch (InterruptedException ignored) {
        }
        return null;
    }

    /**
     * 尝试连接指定服务，如果该客户端已连接到服务则不作任何操作
     *
     * @param host 要连接到的服务IP
     * @param port TCP端口号
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/10/19 14:35
     */
    public boolean connect(String host, int port) {
        if (status == DESTROYED) {
            throw new IllegalStateException("客户端已被摧毁");
        }
        if (isActive()) {
            return true;
        }
        synchronized (lock) {
            this.channel = getChannel(host, port);
            status = USING;
        }
        return isActive();
    }

    public boolean disConnect() {
        if (this.channel != null) {
            try {
                synchronized (lock) {
                    this.channel
                            .close()
                            .sync();
                    this.channel = null;
                }
                log.info("rpc client disconnect success");
                return true;
            } catch (InterruptedException ignored) {
            }
        }
        return false;
    }

    public boolean isActive() {
        return channel != null && channel.isActive();
    }

    public static void sendRequest(Channel activeChannel, RPCProtocol<RPCRequest> request) {
        if (activeChannel.isActive()) {
            activeChannel.writeAndFlush(request);
        } else {
            throw new ConnectionClosedException();
        }
    }

    public static RPCResponse sendRequestSync(Channel activeChannel, RPCProtocol<RPCRequest> request, long getDataTimeout, TimeUnit unit) {
        if (activeChannel.isActive()) {
            try {
                RPCFuture future = new RPCFuture(new DefaultPromise<>(new DefaultEventLoop()));
                RequestFutureContainer.insert(request
                        .getHeader()
                        .getReqId(), future);
                activeChannel
                        .writeAndFlush(request)
                        .sync();
                try {
                    return future
                            .getPromise()
                            .get(getDataTimeout, unit);
                } catch (TimeoutException e) {
                    throw new ResponseTimeoutException("请求超时：" + unit.toMillis(getDataTimeout) + "ms");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        } else {
            throw new ConnectionClosedException();
        }
    }

    public boolean isRemoteServerAlive() {
        if (!isActive()) {
            throw new ConnectionClosedException();
        }
        RPCHeader header = new RPCHeader(ReqType.HEARTBEAT);
        RPCRequest request = new RPCRequest();
        RPCProtocol<RPCRequest> rpcProtocol = new RPCProtocol<>();
        rpcProtocol.setHeader(header);
        rpcProtocol.setContent(request);
        try {
            RPCResponse response = sendRequestSync(rpcProtocol);
            return response.isSuccess();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * 发送一个请求，该方法将立即返回，具体的结果请通过RequestFutureContainer异步获取
     *
     * @param request 要发送的请求
     * @return void
     * @author Huang Yongxiang
     * @date 2022/9/21 16:42
     * @see RequestFutureContainer
     */
    public void sendRequest(RPCProtocol<RPCRequest> request) {
        sendRequest(channel, request);
    }

    /**
     * 发送一个请求，该方法将会阻塞等待请求内容发送完成，注意该方法不会关闭连接
     *
     * @param request 要发送的请求
     * @return RPCResponse 响应
     * @author Huang Yongxiang
     * @date 2022/9/21 15:03
     */
    public RPCResponse sendRequestSync(RPCProtocol<RPCRequest> request) {
        return sendRequestSync(channel, request, getDataTimeout, TimeUnit.MILLISECONDS);
    }

    public RPCResponse sendRequestSync(RPCProtocol<RPCRequest> request, long getDataTimeout, TimeUnit unit) {
        return sendRequestSync(channel, request, getDataTimeout, unit);
    }

    public void refresh() {
        status = CREATED;
    }


    @Override
    public void close() {
        disConnect();
        status = DESTROYED;
    }
}
