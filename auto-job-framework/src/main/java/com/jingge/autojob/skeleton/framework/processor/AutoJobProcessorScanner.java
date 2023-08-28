package com.jingge.autojob.skeleton.framework.processor;

import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 加载器扫描器，扫描指定目录下实现了IAutoJobLoader接口的类
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/12 15:25
 */
@Slf4j
public class AutoJobProcessorScanner {
    private final String[] processorPattern;

    public AutoJobProcessorScanner(String... loaderPattern) {
        this.processorPattern = loaderPattern;
    }

    public List<IAutoJobProcessor> scanInstance() {
        Set<Class<? extends IAutoJobProcessor>> classSet = scanClass();
        classSet = classSet == null ? Collections.emptySet() : classSet;
        //从容器查找指定的loader
        List<IAutoJobProcessor> processors = new LinkedList<>();
        AutoJobProcessorContext context = AutoJobProcessorContext.getInstance();
        for (Class<? extends IAutoJobProcessor> type : classSet) {
            if (context.containsProcessor(type)) {
                processors.add(context.getProcessor(type));
            }
        }
        log.info("扫描到{}个处理器", processors.size());
        return processors;
    }

    public Set<Class<? extends IAutoJobProcessor>> scanClass() {
        Reflections reflections = null;
        if (processorPattern != null && processorPattern.length > 0) {
            reflections = new Reflections(processorPattern, Scanners.SubTypes);
        } else {
            reflections = new Reflections(Scanners.SubTypes);
        }
        return reflections.getSubTypesOf(IAutoJobProcessor.class).stream().filter(item -> !item.isInterface() && !AutoJobProcessorContext.getInstance().containsProcessor(item)).collect(Collectors.toSet());
    }


}
