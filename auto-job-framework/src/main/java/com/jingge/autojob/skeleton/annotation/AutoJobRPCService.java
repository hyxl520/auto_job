package com.jingge.autojob.skeleton.annotation;

import com.jingge.autojob.skeleton.framework.network.handler.server.DefaultServiceFactory;
import com.jingge.autojob.skeleton.framework.network.handler.server.ServiceFactory;

import java.lang.annotation.*;

/**
 * 标注一个类是RPC服务
 *
 * @Author Huang Yongxiang
 * @Date 2022/09/17 18:16
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoJobRPCService {
    /**
     * 服务名称
     */
    String value();

    /**
     * 服务创建工厂
     */
    Class<? extends ServiceFactory> serviceFactory() default DefaultServiceFactory.class;
}
