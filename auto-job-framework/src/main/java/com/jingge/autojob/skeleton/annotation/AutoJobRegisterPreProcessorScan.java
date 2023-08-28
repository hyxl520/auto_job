package com.jingge.autojob.skeleton.annotation;

import java.lang.annotation.*;

/**
 * 该注解用于配置注册前置处理器的扫描路径，不指定时默认扫描全项目，支持子包扫描
 *
 * @Author Huang Yongxiang
 * @Date 2022/11/21 10:03
 * @Email 1158055613@qq.com
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoJobRegisterPreProcessorScan {
    String[] value();
}
