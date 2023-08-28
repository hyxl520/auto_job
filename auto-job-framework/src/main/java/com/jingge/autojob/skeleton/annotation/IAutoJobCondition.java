package com.jingge.autojob.skeleton.annotation;

import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.util.io.PropertiesHolder;

/**
 * 任务启动条件
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/12 13:54
 * @see Conditional
 */
public interface IAutoJobCondition {
    /**
     * 该方法返回值将决定是否对启动该任务
     *
     * @param propertiesHolder   配置项容器
     * @param autoJobApplication AutoJob应用上下文，注意此时上下文正在启动中，组件都已创建，但可能还未启动
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/10/28 10:46
     */
    boolean matches(PropertiesHolder propertiesHolder, AutoJobApplication autoJobApplication);
}
