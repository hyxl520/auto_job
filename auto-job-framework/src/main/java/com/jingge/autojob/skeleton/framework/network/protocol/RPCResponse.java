package com.jingge.autojob.skeleton.framework.network.protocol;

import com.jingge.autojob.skeleton.framework.network.enums.ResponseCode;
import lombok.Data;

import java.io.Serializable;

/**
 * RPC响应对象
 *
 * @Author Huang Yongxiang
 * @Date 2022/09/09 16:39
 */
@Data
public class RPCResponse implements Serializable {
    /**
     * 回执码
     */
    private Integer code;
    /**
     * 方法返回结果
     */
    private Object result;
    /**
     * 方法返回类型
     */
    private Class<?> returnType;
    /**
     * 消息内容
     */
    private String message;

    public boolean isSuccess() {
        if (code == null) {
            return false;
        }
        return ResponseCode.findByCode(code) == ResponseCode.SUCCESS;
    }
}
