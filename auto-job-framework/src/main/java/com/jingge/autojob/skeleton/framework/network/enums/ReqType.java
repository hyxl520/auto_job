package com.jingge.autojob.skeleton.framework.network.enums;

/**
 * 请求类型
 *
 * @Author Huang Yongxiang
 * @Date 2022/09/09 17:58
 */
public enum ReqType {
    REQUEST((byte) 0), RESPONSE((byte) 1), HEARTBEAT((byte) 2);
    private final byte type;

    public byte getType() {
        return type;
    }

    ReqType(byte type) {
        this.type = type;
    }

    public static ReqType findByTypeCode(int code) {
        byte type = (byte) code;
        for (ReqType reqType : ReqType.values()) {
            if (reqType.type == type) {
                return reqType;
            }
        }
        return null;
    }
}
