package com.jingge.autojob.skeleton.annotation;


import java.lang.annotation.*;

/**
 * 该注解用于定义扫描所有实现了IAutoJobProcessor接口的类的特定包，支持子包扫描
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/12 15:43
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoJobProcessorScan {
    String[] value();
}
