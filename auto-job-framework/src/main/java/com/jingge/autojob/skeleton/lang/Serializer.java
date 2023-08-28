package com.jingge.autojob.skeleton.lang;

/**
 * 序列器的公共接口
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-03-21 15:04
 * @email 1158055613@qq.com
 */
public interface Serializer<R, S> {
    R serialize(S source);

    @SuppressWarnings("unchecked")
    default R serialize() {
        return serialize((S) this);
    }
}
