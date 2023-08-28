package com.jingge.autojob.skeleton.framework.network.codec;

import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.config.AutoJobClusterConfig;
import com.jingge.autojob.skeleton.framework.network.protocol.RPCHeader;
import com.jingge.autojob.skeleton.framework.network.protocol.RPCProtocol;
import com.jingge.autojob.util.convert.ProtoStuffUtil;
import com.jingge.autojob.util.encrypt.AESUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * 编码器
 *
 * @Author Huang Yongxiang
 * @Date 2022/09/09 17:20
 */
@Slf4j
public class RPCEncoder extends MessageToByteEncoder<RPCProtocol<Object>> {
    @Override
    protected void encode(ChannelHandlerContext ctx, RPCProtocol<Object> msg, ByteBuf out) throws Exception {
        log.debug("=================RPC Start Encoding=================>");
        AutoJobClusterConfig clusterConfig = AutoJobApplication
                .getInstance()
                .getConfigHolder()
                .getClusterConfig();
        RPCHeader header = msg.getHeader();
        //按顺序写入数据
        byte[] headerBytes = clusterConfig.getEnableAuth() ? AESUtil
                .build(clusterConfig.getClusterPublicKey())
                .aesEncryptToBytes(ProtoStuffUtil.serialize(header)) : ProtoStuffUtil.serialize(header);
        byte[] contentBytes = clusterConfig.getEnableAuth() ? AESUtil
                .build(header.getRandomKey())
                .aesEncryptToBytes(ProtoStuffUtil.serialize(msg.getContent())) : ProtoStuffUtil.serialize(msg.getContent());
        //log.info("写入头长度：{}，内容长度：{}", headerBytes.length, contentBytes.length);
        out
                .writeInt(12 + headerBytes.length + contentBytes.length)
                .writeInt(headerBytes.length)
                .writeBytes(headerBytes)
                .writeInt(contentBytes.length)
                .writeBytes(contentBytes);
        log.debug("=================RPC Encoding Done=================<");
    }
}
