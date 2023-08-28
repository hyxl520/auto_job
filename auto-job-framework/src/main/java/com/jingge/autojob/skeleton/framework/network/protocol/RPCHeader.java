package com.jingge.autojob.skeleton.framework.network.protocol;

import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.config.AutoJobClusterConfig;
import com.jingge.autojob.skeleton.framework.network.enums.ReqType;
import com.jingge.autojob.util.convert.StringUtils;
import com.jingge.autojob.util.id.IdGenerator;
import com.jingge.autojob.util.servlet.InetUtil;
import lombok.Data;

import java.io.Serializable;

/**
 * 消息体Header部分
 *
 * @Author Huang Yongxiang
 * @Date 2022/09/09 12:11
 */
@Data
public class RPCHeader implements Serializable {
    /**
     * 通信token
     */
    private String token;
    /**
     * 通信公钥
     */
    private String randomKey;
    /**
     * 请求编号
     */
    private long reqId;
    /**
     * 发送方的IP地址
     */
    private String sendIp;
    /**
     * 发送方的端口号
     */
    private int sendPort;
    /**
     * 处理时间，请求中是发出请求的时间，响应中是处理完请求花费的时间
     */
    private long handleTime;
    /**
     * 请求类型
     */
    private byte reqType;

    public RPCHeader(ReqType reqType) {
        AutoJobClusterConfig config = AutoJobApplication
                .getInstance()
                .getConfigHolder()
                .getClusterConfig();
        this.reqType = reqType.getType();
        this.token = config.getClusterToken();
        this.randomKey = StringUtils.getRandomStr(16);
        this.sendPort = InetUtil.getPort();
        this.handleTime = System.currentTimeMillis();
        this.sendIp = InetUtil.getLocalhostIp();
        this.reqId = IdGenerator.getNextIdAsLong();
    }
}
