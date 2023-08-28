package com.jingge.autojob.skeleton.annotation;

import java.lang.annotation.*;

/**
 * 扫描任务的包路径，支持子包扫描
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/12 17:44
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoJobScan {
    /**
     * 扫描的父包路径，不用也不能指定通配符，自动进行子包扫描
     */
    String[] value();
}
