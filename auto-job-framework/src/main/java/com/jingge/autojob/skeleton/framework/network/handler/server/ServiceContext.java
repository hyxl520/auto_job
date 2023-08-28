package com.jingge.autojob.skeleton.framework.network.handler.server;

import com.jingge.autojob.skeleton.annotation.AutoJobRPCService;
import com.jingge.autojob.util.bean.ObjectUtil;
import com.jingge.autojob.util.cache.LocalCacheManager;
import lombok.extern.slf4j.Slf4j;
import net.jodah.expiringmap.ExpirationPolicy;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 服务上下文
 *
 * @Author Huang Yongxiang
 * @Date 2022/09/17 18:08
 */
@Slf4j
public class ServiceContext {

    private final Map<String, Object> serviceContainer = new ConcurrentHashMap<>();
    /**
     * 缓存Method对象，提高效率
     */
    private final LocalCacheManager<ServiceMethodKey, Method> methodCache = LocalCacheManager
            .builder()
            .setExpiringTime(10, TimeUnit.MINUTES)
            .setPolicy(ExpirationPolicy.ACCESSED)
            .setMaxLength(500)
            .build();


    public static ServiceContext getInstance() {
        return InstanceHolder.CONTEXT;
    }

    private ServiceContext() {
        Set<Class<?>> serviceClasses = new ServiceScanner().scan();
        for (Class<?> clazz : serviceClasses) {
            AutoJobRPCService ASRPCService = clazz.getAnnotation(AutoJobRPCService.class);
            ServiceFactory serviceFactory = ObjectUtil.getClassInstance(ASRPCService.serviceFactory());
            Object instance = serviceFactory.newService(clazz);
            if (instance != null) {
                if (serviceContainer.containsKey(ASRPCService.value())) {
                    log.error("服务：{}已存在一个实例", ASRPCService.value());
                    throw new IllegalArgumentException();
                }
                serviceContainer.put(ASRPCService.value(), instance);
            }
        }
    }

    public Object getServiceInstance(String serviceName) {
        if (isExist(serviceName)) {
            return serviceContainer.get(serviceName);
        }
        return null;
    }

    public boolean isExist(String serviceName) {
        return serviceContainer.containsKey(serviceName);
    }

    public Method getServiceMethod(String serviceName, String methodName, Class<?>... paramsType) {
        if (!isExist(serviceName)) {
            throw new NoSuchServiceException("没有服务：" + serviceName);
        }
        Object service = getServiceInstance(serviceName);
        ServiceMethodKey key = new ServiceMethodKey(serviceName, methodName, paramsType);
        if (methodCache.exist(key)) {
            return methodCache.get(key);
        }
        Method method;
        if (paramsType != null && paramsType.length > 0) {
            method = ObjectUtil.findMethod(methodName, service.getClass(), paramsType);
        } else {
            method = ObjectUtil.findMethod(methodName, service.getClass());
        }
        if (method != null) {
            methodCache.set(key, method);
        }
        return method;
    }

    public boolean withReturn(String serviceName, String methodName, Class<?>... paramsType) {
        return !((getReturnType(serviceName, methodName, paramsType) == void.class));
    }

    public Class<?> getReturnType(String serviceName, String methodName, Class<?>... paramsType) {
        Method method = getServiceMethod(serviceName, methodName, paramsType);
        if (method == null) {
            throw new NoSuchServiceMethodException();
        }
        return method.getReturnType();
    }

    public Object invokeServiceMethod(String serviceName, String methodName, Class<?>[] paramsType, Object... params) throws Exception {
        if (!isExist(serviceName)) {
            throw new NoSuchServiceException("没有服务：" + serviceName);
        }
        Object service = getServiceInstance(serviceName);
        Method method = getServiceMethod(serviceName, methodName, paramsType);
        if (method != null) {
            try {
                return method.invoke(service, params);
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }
        throw new NoSuchServiceMethodException("服务" + serviceName + "没有方法：" + methodName);
    }

    public Object removeAndGet(String serviceName) {
        return serviceContainer.remove(serviceName);
    }

    private static class InstanceHolder {
        private static final ServiceContext CONTEXT = new ServiceContext();
    }

    private static class ServiceMethodKey {
        String serviceName;
        String methodName;
        Class<?>[] paramsType;

        public ServiceMethodKey(String serviceName, String methodName, Class<?>[] paramsType) {
            this.serviceName = serviceName;
            this.methodName = methodName;
            this.paramsType = paramsType;
        }

        @Override
        public int hashCode() {
            int result = 17;
            result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
            result = 31 * result + (methodName != null ? methodName.hashCode() : 0);
            result = 31 * result + Arrays.hashCode(paramsType);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            if (hashCode() != obj.hashCode()) {
                return false;
            }
            ServiceMethodKey other = (ServiceMethodKey) obj;
            if (serviceName != null && other.serviceName != null && !serviceName.equals(other.serviceName)) {
                return false;
            } else if (methodName != null && other.methodName != null && !methodName.equals(other.methodName)) {
                return false;
            }
            return Arrays.equals(paramsType, other.paramsType);
        }
    }
}
