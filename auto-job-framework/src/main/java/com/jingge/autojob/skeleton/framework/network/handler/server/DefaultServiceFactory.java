package com.jingge.autojob.skeleton.framework.network.handler.server;

import com.jingge.autojob.util.bean.ObjectUtil;

/**
 * 默认服务工厂
 *
 * @author Huang Yongxiang
 * @date 2023-01-04 11:37
 * @email 1158055613@qq.com
 */
public class DefaultServiceFactory implements ServiceFactory {
    @Override
    public Object newService(Class<?> service) {
        return ObjectUtil.getClassInstance(service);
    }
}
