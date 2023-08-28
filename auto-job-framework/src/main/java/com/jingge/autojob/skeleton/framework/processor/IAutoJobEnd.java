package com.jingge.autojob.skeleton.framework.processor;

/**
 * 实现该接口的类将会在系统关闭前执行，该类需要在Spring环境中存在
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/29 11:05
 * @see IAutoJobLoader
 */
public interface IAutoJobEnd extends IAutoJobProcessor {
    void end();
}
