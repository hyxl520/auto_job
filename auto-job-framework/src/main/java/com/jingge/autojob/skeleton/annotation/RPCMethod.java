package com.jingge.autojob.skeleton.annotation;

import java.lang.annotation.*;

/**
 * 标注在一个RPC客户端下的方法上，标明该方法的代理信息
 *
 * @Author Huang Yongxiang
 * @Date 2022/09/19 16:47
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RPCMethod {
    /**
     * 远程服务方法名，默认是客户端方法名
     */
    String value();
}
