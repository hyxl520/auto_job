package com.jingge.autojob.skeleton.annotation;

import java.lang.annotation.*;

/**
 * 标注在一个接口上，表明该接口是一个RPC客户端
 *
 * @Author Huang Yongxiang
 * @Date 2022/09/19 16:46
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoJobRPCClient {
    /**
     * 服务名称
     */
    String value();
}
