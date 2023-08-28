package com.jingge.autojob.skeleton.framework.network.handler.server;

import com.jingge.autojob.skeleton.annotation.AutoJobRPCService;
import com.jingge.autojob.skeleton.annotation.AutoJobRPCServiceScan;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.util.Set;

/**
 * 服务扫描器
 *
 * @Author Huang Yongxiang
 * @Date 2022/09/17 18:09
 */
public class ServiceScanner {
    private final Reflections reflections;

    public Set<Class<?>> scan() {
        return reflections.getTypesAnnotatedWith(AutoJobRPCService.class);
    }

    public ServiceScanner() {
        Class<?> app = AutoJobApplication
                .getInstance()
                .getApplication();
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder().forPackages("com.jingge.autojob.skeleton.cluster", "com.jingge.autojob.skeleton.model.register");
        if (app != null) {
            AutoJobRPCServiceScan serviceScan = app.getAnnotation(AutoJobRPCServiceScan.class);
            if (serviceScan != null) {
                configurationBuilder.forPackages(serviceScan.value());
            }
        }
        configurationBuilder.addScanners(Scanners.TypesAnnotated);
        reflections = new Reflections(configurationBuilder);
    }
}
