package com.jingge.autojob.skeleton.model.register;

import com.jingge.autojob.skeleton.annotation.AutoJobRegisterPreProcessorScan;
import com.jingge.autojob.skeleton.annotation.ProcessorLevel;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.processor.IAutoJobLoader;
import com.jingge.autojob.util.bean.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 注册器加载器
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/30 11:19
 */
@ProcessorLevel(Integer.MAX_VALUE)
@Slf4j
public class AutoJobRegisterLoader implements IAutoJobLoader {

    @Override
    public void load() {
        IAutoJobRegister autoJobRegister = AutoJobApplication
                .getInstance()
                .getRegister();
        Class<?> application = AutoJobApplication
                .getInstance()
                .getApplication();
        AutoJobRegisterPreProcessorScan scan = application.getDeclaredAnnotation(AutoJobRegisterPreProcessorScan.class);
        Reflections reflections = null;
        if (scan == null) {
            reflections = new Reflections(new String[]{AutoJobRegister.class.getPackage().getName()}, Scanners.SubTypes);
        } else {
            List<String> patternList = new ArrayList<>(Arrays.asList(scan.value()));
            patternList.add(AutoJobRegister.class
                    .getPackage()
                    .getName());
            reflections = new Reflections(patternList.toArray(), Scanners.SubTypes);
        }
        Set<Class<? extends AbstractRegisterHandler>> classes = reflections.getSubTypesOf(AbstractRegisterHandler.class);
        Set<Class<? extends AbstractRegisterFilter>> filterClasses = reflections.getSubTypesOf(AbstractRegisterFilter.class);
        AbstractRegisterHandler.Builder handlerBuilder = AbstractRegisterHandler.builder();
        for (Class<? extends AbstractRegisterHandler> clazz : classes) {
            handlerBuilder.addHandler(ObjectUtil.getClassInstance(clazz));
        }
        log.debug("成功加载{}个注册处理器", classes.size());

        AbstractRegisterFilter.Builder filterBuilder = AbstractRegisterFilter.builder();
        for (Class<? extends AbstractRegisterFilter> clazz : filterClasses) {
            filterBuilder.addHandler(ObjectUtil.getClassInstance(clazz));
        }
        log.debug("成功加载{}个注册过滤器", filterClasses.size());

        autoJobRegister.setHandler(handlerBuilder.build());
        autoJobRegister.setFilter(filterBuilder.build());
    }

}
