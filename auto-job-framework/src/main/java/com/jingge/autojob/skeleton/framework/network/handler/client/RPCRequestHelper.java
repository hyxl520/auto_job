package com.jingge.autojob.skeleton.framework.network.handler.client;

import com.jingge.autojob.skeleton.framework.config.AutoJobClusterConfig;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.network.enums.ReqType;
import com.jingge.autojob.skeleton.framework.network.protocol.RPCHeader;
import com.jingge.autojob.skeleton.framework.network.protocol.RPCProtocol;
import com.jingge.autojob.skeleton.framework.network.protocol.RPCRequest;

/**
 * RPC请求辅助类，该类将请求与业务线程绑定
 *
 * @Author Huang Yongxiang
 * @Date 2022/09/19 14:49
 */
public class RPCRequestHelper {
    private static final ThreadLocal<RPCProtocol<RPCRequest>> currentRequest = new ThreadLocal<>();

    public static void initialization(RPCProtocol<RPCRequest> request) {
        currentRequest.set(request);
    }

    public static void destroy() {
        currentRequest.remove();
    }

    public static RPCRequest getCurrentRequestBody() {
        return currentRequest
                .get()
                .getContent();
    }

    public static RPCProtocol<RPCRequest> getCurrentRequest() {
        return currentRequest.get();
    }

    public static RPCHeader getCurrentHeader() {
        return currentRequest
                .get()
                .getHeader();
    }

    public static ReqType getCurrentReqType() {
        return ReqType.findByTypeCode(currentRequest
                .get()
                .getHeader()
                .getReqType());
    }

    public static boolean isAuthSuccess() {
        AutoJobClusterConfig clusterConfig = AutoJobApplication
                .getInstance()
                .getConfigHolder()
                .getClusterConfig();
        return clusterConfig
                .getClusterToken()
                .equals(getCurrentHeader().getToken());
    }

    public static boolean isClockSync() {
        AutoJobClusterConfig clusterConfig = AutoJobApplication
                .getInstance()
                .getConfigHolder()
                .getClusterConfig();
        return Math.abs(System.currentTimeMillis() - getCurrentHeader().getHandleTime()) <= clusterConfig.getClusterAllowMaxJetLag();
    }
}
