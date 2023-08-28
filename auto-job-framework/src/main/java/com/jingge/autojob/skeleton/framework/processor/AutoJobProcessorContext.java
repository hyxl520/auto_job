package com.jingge.autojob.skeleton.framework.processor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 处理器容器
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/12 16:07
 */
public class AutoJobProcessorContext {
    private final Map<String, IAutoJobProcessor> processorMap;

    private AutoJobProcessorContext() {
        this.processorMap = new ConcurrentHashMap<>();
    }

    public boolean addProcessor(IAutoJobProcessor autoJobProcessor) {
        if (autoJobProcessor == null) {
            return false;
        }
        processorMap.put(autoJobProcessor.getClass().getName(), autoJobProcessor);
        return true;
    }

    public boolean addAllProcessors(List<IAutoJobProcessor> processors) {
        if (processors == null) {
            return false;
        }
        processors.forEach(processor -> processorMap.put(processor.getClass().getName(), processor));
        return true;
    }

    public boolean removeProcessor(Class<? extends IAutoJobProcessor> processor) {
        return processorMap.remove(processor.getName()) != null;
    }

    public boolean containsProcessor(Class<? extends IAutoJobProcessor> processor) {
        return processorMap.containsKey(processor.getName());
    }

    @SuppressWarnings("unchecked")
    public <T extends IAutoJobProcessor> T getProcessor(Class<T> clazz) {
        return (T) processorMap.get(clazz.getName());
    }

    public static AutoJobProcessorContext getInstance() {
        return InstanceHolder.CONTEXT;
    }

    private static class InstanceHolder {
        private static final AutoJobProcessorContext CONTEXT = new AutoJobProcessorContext();
    }


}
