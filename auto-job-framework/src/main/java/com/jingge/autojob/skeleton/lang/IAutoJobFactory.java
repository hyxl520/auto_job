package com.jingge.autojob.skeleton.lang;

/**
 * 工厂类的公共接口，该类只做语意上的说明
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/06 10:50
 */
public interface IAutoJobFactory {
    default Object create(Object... params) {
        return null;
    }
}
