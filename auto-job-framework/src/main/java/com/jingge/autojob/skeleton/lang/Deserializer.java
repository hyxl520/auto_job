package com.jingge.autojob.skeleton.lang;

/**
 * 反序列器的公共接口
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-03-21 15:04
 * @email 1158055613@qq.com
 */
public interface Deserializer<R, S> {
    default R deserialize(S source) {
        return null;
    }

    default R deserialize(S source, Class<R> type) {
        return null;
    }
}
