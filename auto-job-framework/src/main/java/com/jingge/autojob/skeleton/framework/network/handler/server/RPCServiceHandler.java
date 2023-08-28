package com.jingge.autojob.skeleton.framework.network.handler.server;

import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.config.AutoJobClusterConfig;
import com.jingge.autojob.skeleton.framework.network.enums.ReqType;
import com.jingge.autojob.skeleton.framework.network.enums.ResponseCode;
import com.jingge.autojob.skeleton.framework.network.handler.client.RPCRequestHelper;
import com.jingge.autojob.skeleton.framework.network.protocol.RPCProtocol;
import com.jingge.autojob.skeleton.framework.network.protocol.RPCRequest;
import com.jingge.autojob.skeleton.framework.network.protocol.RPCResponse;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.concurrent.Callable;

/**
 * 服务处理
 *
 * @Author Huang Yongxiang
 * @Date 2022/09/17 18:07
 */
public class RPCServiceHandler implements Callable<RPCResponse> {
    private final RPCProtocol<RPCRequest> rpcRequest;
    private final AutoJobClusterConfig clusterConfig = AutoJobApplication
            .getInstance()
            .getConfigHolder()
            .getClusterConfig();

    public RPCServiceHandler(RPCProtocol<RPCRequest> rpcRequest) {
        this.rpcRequest = rpcRequest;
    }

    @Override
    public RPCResponse call() {
        RPCResponse rpcResponse = new RPCResponse();
        if (rpcRequest != null) {
            RPCRequestHelper.initialization(rpcRequest);
            if (clusterConfig.getEnableAuth() && !RPCRequestHelper.isAuthSuccess()) {
                rpcResponse.setCode(ResponseCode.FORBIDDEN.code);
                rpcResponse.setMessage("您没有足够的权限");
                return rpcResponse;
            }
            if (clusterConfig.getClusterAllowMaxJetLag() != null && clusterConfig.getClusterAllowMaxJetLag() > 0 && !RPCRequestHelper.isClockSync()) {
                rpcResponse.setCode(ResponseCode.FORBIDDEN.code);
                rpcResponse.setMessage("时钟相差过大");
                return rpcResponse;
            }
            if (RPCRequestHelper.getCurrentReqType() == ReqType.HEARTBEAT) {
                rpcResponse.setCode(ResponseCode.SUCCESS.code);
                rpcResponse.setMessage("hey");
                return rpcResponse;
            }
            RPCRequest request = rpcRequest.getContent();
            try {
                Object result = ServiceContext
                        .getInstance()
                        .invokeServiceMethod(request.getServiceName(), request.getMethodName(), request.getParamTypes(), request.getParams());
                rpcResponse.setCode(ResponseCode.SUCCESS.code);
                rpcResponse.setMessage("请求成功");
                rpcResponse.setResult(result);
                return rpcResponse;
            } catch (NoSuchServiceException e) {
                rpcResponse.setCode(ResponseCode.NO_SERVICE.code);
                rpcResponse.setMessage("请求失败，没有指定服务：" + request.getServiceName());
                return rpcResponse;
            } catch (NoSuchServiceMethodException e) {
                rpcResponse.setCode(ResponseCode.NO_SERVICE_METHOD.code);
                rpcResponse.setMessage("请求失败，没有指定服务方法：" + request.getMethodName());
                return rpcResponse;
            } catch (Exception e) {
                rpcResponse.setCode(ResponseCode.SERVER_ERROR.code);
                rpcResponse.setMessage("服务异常：" + ExceptionUtils.getMessage(e.getCause()));
                return rpcResponse;
            } finally {
                RPCRequestHelper.destroy();
            }
        }
        rpcResponse.setCode(ResponseCode.FORBIDDEN.code);
        rpcResponse.setMessage("空请求");
        return rpcResponse;
    }

    public boolean withReturn() {
        RPCRequest request = rpcRequest.getContent();
        return ServiceContext
                .getInstance()
                .withReturn(request.getServiceName(), request.getMethodName(), request.getParamTypes());
    }


}
