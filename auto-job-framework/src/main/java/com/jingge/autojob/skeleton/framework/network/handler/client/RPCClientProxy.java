package com.jingge.autojob.skeleton.framework.network.handler.client;

import com.jingge.autojob.util.servlet.InetUtil;

import java.lang.reflect.Proxy;

/**
 * 客户端代理类
 *
 * @Author Huang Yongxiang
 * @Date 2022/09/19 16:40
 */
public class RPCClientProxy<T> {
    private final RPCClientInvokeProxy invokeProxy;
    private final Class<T> proxyInterface;

    public RPCClientProxy(String host, int port, Class<T> proxyInterface) {
        this.proxyInterface = proxyInterface;
        this.invokeProxy = new RPCClientInvokeProxy("localhost".equals(host) ? InetUtil.getLocalhostIp() : host, port, proxyInterface);
    }

    /**
     * 创建目标接口的代理
     *
     * @return T
     * @author Huang Yongxiang
     * @date 2022/11/1 15:05
     */
    @SuppressWarnings("unchecked")
    public T clientProxy() {
        return (T) Proxy.newProxyInstance(proxyInterface.getClassLoader(), new Class<?>[]{proxyInterface}, invokeProxy);
    }

    public void destroyProxy() {
        invokeProxy.close();
    }
}
