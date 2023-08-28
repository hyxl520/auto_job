package com.jingge.autojob.skeleton.annotation;

import java.lang.annotation.*;

/**
 * 表明一个配置是可热加载的
 *
 * @author Huang Yongxiang
 * @date 2022-12-27 11:11
 * @email 1158055613@qq.com
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.CLASS)
@Documented
public @interface HotLoadable {
}
