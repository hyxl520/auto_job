package com.jingge.autojob.skeleton.lang;

/**
 * 转化器的抽象接口
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-03-15 15:52
 * @email 1158055613@qq.com
 */
public interface Convertor<R, T> {
    R convert(T source);
}
