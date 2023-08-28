package com.jingge.autojob.skeleton.framework.network.handler.client;

import com.jingge.autojob.skeleton.annotation.AutoJobRPCClient;
import com.jingge.autojob.skeleton.annotation.RPCMethod;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.network.client.RPCClient;
import com.jingge.autojob.skeleton.framework.network.enums.ReqType;
import com.jingge.autojob.skeleton.framework.network.protocol.RPCHeader;
import com.jingge.autojob.skeleton.framework.network.protocol.RPCProtocol;
import com.jingge.autojob.skeleton.framework.network.protocol.RPCRequest;
import com.jingge.autojob.skeleton.framework.network.protocol.RPCResponse;

import java.io.Closeable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 客户端同步执行代理
 *
 * @Author Huang Yongxiang
 * @Date 2022/09/19 16:30
 */
public class RPCClientInvokeProxy implements InvocationHandler, Closeable {
    private final String host;
    private final int port;
    private final Class<?> targetClass;
    private final RPCClient client;

    /**
     * 构造一个RPC客户端执行代理
     *
     * @param host        目标主机
     * @param port        目标端口
     * @param targetClass 具有{@link AutoJobRPCClient}注解的接口
     * @author Huang Yongxiang
     * @date 2022/10/28 10:41
     */
    public RPCClientInvokeProxy(String host, int port, Class<?> targetClass) {
        this.host = host;
        this.port = port;
        this.targetClass = targetClass;
        this.client = AutoJobApplication
                .getInstance()
                .getNetWorkManager()
                .getRPCClient();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        AutoJobRPCClient client = targetClass.getAnnotation(AutoJobRPCClient.class);
        String serviceName = client == null ? targetClass.getSimpleName() : client.value();
        RPCMethod rpcMethod = method.getAnnotation(RPCMethod.class);
        String methodName = rpcMethod == null ? method.getName() : rpcMethod.value();
        RPCProtocol<RPCRequest> rpcProtocol = new RPCProtocol<>();
        RPCHeader header = new RPCHeader(ReqType.REQUEST);
        RPCRequest request = new RPCRequest();
        request.setMethodName(methodName);
        request.setServiceName(serviceName);
        request.setParams(args);
        Class<?>[] paramTypes = method.getParameterTypes();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (paramTypes[i] != args[i].getClass() && paramTypes[i].isAssignableFrom(args[i].getClass())) {
                    paramTypes[i] = args[i].getClass();
                }
            }
        }
        request.setParamTypes(method.getParameterTypes());
        rpcProtocol.setHeader(header);
        rpcProtocol.setContent(request);
        this.client.connect(host, port);
        RPCResponse response = this.client.sendRequestSync(rpcProtocol);
        return response == null ? null : response.getResult();
    }

    @Override
    public void close() {
        AutoJobApplication
                .getInstance()
                .getNetWorkManager()
                .closeClient(client);
    }
}
