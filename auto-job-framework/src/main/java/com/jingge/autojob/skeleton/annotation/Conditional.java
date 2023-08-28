package com.jingge.autojob.skeleton.annotation;

import java.lang.annotation.*;

/**
 * 任务注入条件，具体条件的实现请看{@link IAutoJobCondition}接口
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/12 13:52
 * @see IAutoJobCondition
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Conditional {
    Class<? extends IAutoJobCondition>[] value();
}
