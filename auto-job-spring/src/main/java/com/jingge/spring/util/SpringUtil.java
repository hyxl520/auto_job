package com.jingge.spring.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @Description Spring的动态Bean获取工具类，用于解决反射时Autowired的对象为空的问题
 * @Auther Huang Yongxiang
 * @Date 2021/12/16 16:25
 */
@Component("autoJobSpringUtil")
public class SpringUtil implements ApplicationContextAware {
    private static ApplicationContext applicationContext;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringUtil.applicationContext = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    //通过name获取 Bean.
    public static Object getBean(String name) {
        if (applicationContext == null) {
            return null;
        }
        try {
            return applicationContext.getBean(name);
        } catch (Exception ignored) {
        }
        return null;
    }

    //通过class获取Bean.
    public static <T> T getBean(Class<T> clazz) {
        if (applicationContext == null) {
            return null;
        }
        try {
            return applicationContext.getBean(clazz);
        } catch (Exception ignored) {
        }
        return null;
    }

    //通过name,以及Clazz返回指定的Bean
    public static <T> T getBean(String name, Class<T> clazz) {
        if (applicationContext == null) {
            return null;
        }
        try {
            return applicationContext.getBean(name, clazz);
        } catch (Exception ignored) {
        }
        return null;
    }

    public static <T> Set<T> getBeanOfSubType(Class<T> subType) {
        if (applicationContext == null) {
            return Collections.emptySet();
        }
        Map<String, T> holder = applicationContext.getBeansOfType(subType);
        if(CollectionUtils.isEmpty(holder)){
            return Collections.emptySet();
        }
        return new HashSet<>(holder.values());
    }

    public static <T> T getInstance(Class<T> clazz) {
        T instance = getBean(clazz);
        return instance == null ? getClassInstance(clazz) : instance;
    }

    public static <T> T getClassInstance(Class<T> clazz) {
        if (clazz == null) {
            return null;
        }
        try {
            Constructor<T> constructor = clazz.getConstructor();
            return constructor.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
