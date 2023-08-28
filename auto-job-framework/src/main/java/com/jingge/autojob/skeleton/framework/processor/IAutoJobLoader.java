package com.jingge.autojob.skeleton.framework.processor;

/**
 * 标注一个类为一个模块加载器，该方法将会在项目启动时执行
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/05 17:22
 */
public interface IAutoJobLoader extends IAutoJobProcessor {
    void load();
}
