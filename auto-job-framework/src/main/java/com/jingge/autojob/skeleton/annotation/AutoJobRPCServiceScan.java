package com.jingge.autojob.skeleton.annotation;

import java.lang.annotation.*;

/**
 * RPC服务扫描
 *
 * @author Huang Yongxiang
 * @date 2023-01-04 11:42
 * @email 1158055613@qq.com
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoJobRPCServiceScan {
    String[] value();
}
