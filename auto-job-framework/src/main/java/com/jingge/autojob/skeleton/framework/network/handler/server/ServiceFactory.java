package com.jingge.autojob.skeleton.framework.network.handler.server;

import com.jingge.autojob.skeleton.lang.IAutoJobFactory;

/**
 * 服务工厂
 *
 * @author Huang Yongxiang
 * @date 2023-01-04 11:35
 * @email 1158055613@qq.com
 */
public interface ServiceFactory extends IAutoJobFactory {
    Object newService(Class<?> service);
}
