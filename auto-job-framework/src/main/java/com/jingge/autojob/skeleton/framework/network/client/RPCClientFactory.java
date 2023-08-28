package com.jingge.autojob.skeleton.framework.network.client;

import io.netty.bootstrap.Bootstrap;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.util.concurrent.TimeUnit;

/**
 * RPC Client的工厂
 *
 * @Author Huang Yongxiang
 * @Date 2022/10/19 16:09
 */
public class RPCClientFactory implements PooledObjectFactory<RPCClient> {
    private final long connectTimeout;
    private final long getDataTimeout;
    private final Bootstrap bootstrap;

    public RPCClientFactory(Bootstrap bootstrap, long connectTimeout, long getDataTimeout, TimeUnit unit) {
        this.bootstrap = bootstrap;
        this.connectTimeout = unit.toMillis(connectTimeout);
        this.getDataTimeout = unit.toMillis(getDataTimeout);
    }

    @Override
    public void activateObject(PooledObject<RPCClient> pooledObject) throws Exception {
        pooledObject
                .getObject()
                .refresh();
    }

    @Override
    public void destroyObject(PooledObject<RPCClient> pooledObject) {
        pooledObject
                .getObject()
                .close();
    }

    @Override
    public PooledObject<RPCClient> makeObject() {
        return new DefaultPooledObject<>(new RPCClient(bootstrap, connectTimeout, getDataTimeout, TimeUnit.MILLISECONDS));
    }

    @Override
    public void passivateObject(PooledObject<RPCClient> pooledObject) {
        pooledObject
                .getObject()
                .close();
    }

    @Override
    public boolean validateObject(PooledObject<RPCClient> pooledObject) {
        return true;
    }
}
