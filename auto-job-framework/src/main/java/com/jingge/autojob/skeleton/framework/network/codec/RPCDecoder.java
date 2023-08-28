package com.jingge.autojob.skeleton.framework.network.codec;

import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.config.AutoJobClusterConfig;
import com.jingge.autojob.skeleton.framework.network.enums.ReqType;
import com.jingge.autojob.skeleton.framework.network.protocol.RPCHeader;
import com.jingge.autojob.skeleton.framework.network.protocol.RPCProtocol;
import com.jingge.autojob.skeleton.framework.network.protocol.RPCRequest;
import com.jingge.autojob.skeleton.framework.network.protocol.RPCResponse;
import com.jingge.autojob.util.convert.ProtoStuffUtil;
import com.jingge.autojob.util.encrypt.AESUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

/**
 * 解码器
 *
 * @Author Huang Yongxiang
 * @Date 2022/09/09 17:20
 */
@Slf4j
public class RPCDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        log.debug("=================RPC Start Decoding=================>");
        AutoJobClusterConfig clusterConfig = AutoJobApplication
                .getInstance()
                .getConfigHolder()
                .getClusterConfig();
        int totalLen = in.readInt() - 4;
        if (in.readableBytes() < totalLen) {
            in.resetReaderIndex();
            return;
        }
        int headerLen = in.readInt();
        byte[] encryptHeaderBytes = new byte[Math.min(in.readableBytes(), headerLen)];
        in.readBytes(encryptHeaderBytes);
        int contentLen = in.readInt();
        byte[] encryptContentBytes = new byte[Math.min(in.readableBytes(), contentLen)];
        in.readBytes(encryptContentBytes);
        byte[] decryptHeaderBytes = clusterConfig.getEnableAuth() ? AESUtil
                .build(clusterConfig.getClusterPublicKey())
                .aesDecryptByBytesToBytes(encryptHeaderBytes) : encryptHeaderBytes;
        RPCHeader header = ProtoStuffUtil.deserialize(decryptHeaderBytes,RPCHeader.class);
        byte[] decryptContentBytes=clusterConfig.getEnableAuth() ? AESUtil
                .build(header.getRandomKey())
                .aesDecryptByBytesToBytes(encryptContentBytes) : encryptContentBytes;
        ReqType type = ReqType.findByTypeCode(header.getReqType());
        switch (Objects.requireNonNull(type)) {
            case REQUEST: {
                RPCRequest request = ProtoStuffUtil.deserialize(decryptContentBytes, RPCRequest.class);
                RPCProtocol<RPCRequest> rpcProtocol = new RPCProtocol<>();
                rpcProtocol.setContent(request);
                rpcProtocol.setHeader(header);
                out.add(rpcProtocol);
                break;
            }
            case RESPONSE: {
                RPCResponse response = ProtoStuffUtil.deserialize(decryptContentBytes, RPCResponse.class);
                RPCProtocol<RPCResponse> rpcProtocol = new RPCProtocol<>();
                rpcProtocol.setContent(response);
                rpcProtocol.setHeader(header);
                out.add(rpcProtocol);
                break;
            }
            case HEARTBEAT: {
                break;
            }
            default:
                break;
        }
        log.debug("=================RPC Decoding Done=================<");
    }
}
