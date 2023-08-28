package com.jingge.autojob.skeleton.annotation;

import com.jingge.autojob.skeleton.framework.processor.IAutoJobLoader;

import java.lang.annotation.*;

/**
 * 处理器级别，注解高级别>注解低级别>=注解默认值>没有注解
 *
 * @see IAutoJobLoader , IAutoJobProcessor
 * @Author Huang Yongxiang
 * @Date 2022/07/30 16:43
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ProcessorLevel {
    /**
     * 默认的优先级为中间优先级
     */
    int value() default 0;
}
