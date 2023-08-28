package com.jingge.autojob.skeleton.framework.network.client;

import com.jingge.autojob.skeleton.framework.config.TimeConstant;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.util.concurrent.TimeUnit;

/**
 * 客户端池
 *
 * @Author Huang Yongxiang
 * @Date 2022/09/20 12:12
 */
public class RPCClientPool extends GenericObjectPool<RPCClient> {
    public static Builder builder() {
        return new Builder();
    }

    private RPCClientPool(Bootstrap bootstrap, GenericObjectPoolConfig config, long connectTimeout, long getDataTimeout, TimeUnit unit) {
        super(new RPCClientFactory(bootstrap, connectTimeout, getDataTimeout, unit), config);
    }


    public RPCClient getClient() {
        try {
            return this.borrowObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void release(RPCClient client) {
        this.returnObject(client);
    }

    public void shutdown() {
        this.close();
    }

    @Setter
    @Accessors(chain = true)
    public static class Builder {
        private long connectTimeout = 10000;
        private long getDataTimeout = 10000;
        private long getClientTimeout = 3000;
        private long keepAliveTime = TimeConstant.A_MINUTE * 20;
        private int poolSize = 5;
        private int minIdle = 1;
        private int maxIdle = 3;


        public Builder setConnectTimeout(long connectTimeout, TimeUnit unit) {
            this.connectTimeout = unit.toMillis(connectTimeout);
            return this;
        }

        public Builder setGetClientTimeout(long getClientTimeout, TimeUnit unit) {
            this.getClientTimeout = unit.toMillis(getClientTimeout);
            return this;
        }

        public Builder setGetDataTimeout(long getDataTimeout, TimeUnit unit) {
            this.getDataTimeout = unit.toMillis(getDataTimeout);
            return this;
        }

        public Builder setKeepAliveTime(long keepAliveTime, TimeUnit unit) {
            this.keepAliveTime = unit.toMillis(keepAliveTime);
            return this;
        }

        public RPCClientPool build() {
            Bootstrap bootstrap = new Bootstrap();
            EventLoopGroup eventExecutors = new NioEventLoopGroup();
            bootstrap
                    .group(eventExecutors)
                    .channel(NioSocketChannel.class)
                    .handler(new RpcClientInitializer());
            GenericObjectPoolConfig config = new GenericObjectPoolConfig();
            config.setMaxTotal(poolSize);
            config.setMaxIdle(maxIdle);
            config.setMinIdle(minIdle);
            config.setMaxWaitMillis(getClientTimeout);
            config.setSoftMinEvictableIdleTimeMillis(keepAliveTime);
            config.setJmxEnabled(false);
            return new RPCClientPool(bootstrap, config, connectTimeout, getDataTimeout, TimeUnit.MILLISECONDS);
        }

    }
}
