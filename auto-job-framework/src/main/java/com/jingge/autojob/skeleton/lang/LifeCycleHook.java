package com.jingge.autojob.skeleton.lang;

/**
 * 生命周期钩子
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-03-15 13:33
 * @email 1158055613@qq.com
 */
public interface LifeCycleHook {
    default void beforeInitialize(Object... params) throws Exception {

    }

    default void afterInitialize(Object... params) throws Exception {

    }

    default void beforeClose(Object... params) throws Exception {

    }

    default void afterClose(Object... params) throws Exception {

    }
}
