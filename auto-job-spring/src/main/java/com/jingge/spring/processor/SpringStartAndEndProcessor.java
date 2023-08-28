package com.jingge.spring.processor;


import com.jingge.spring.util.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Spring 启动关闭处理器
 *
 * @Author Huang Yongxiang
 * @Date 2022/09/19 9:47
 */
@Component("AutoJobSpringProcessor")
@Slf4j
public class SpringStartAndEndProcessor implements ApplicationListener<ContextRefreshedEvent> {
    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        for (SpringStartProcessor processor : SpringUtil
                .getBeanOfSubType(SpringStartProcessor.class).stream().sorted(new ProcessorComparator()).collect(Collectors.toList())) {
            try {
                processor.onStart();
            } catch (Exception e) {
                e.printStackTrace();
                log.error("启动处理器{}执行时发生异常：{}", processor.getClass().getName(), e.getMessage());
            }
        }
    }

    @PreDestroy
    public void destroy() {
        for (SpringEndProcessor processor : SpringUtil.getBeanOfSubType(SpringEndProcessor.class).stream().sorted(new ProcessorComparator()).collect(Collectors.toList())) {
            try {
                processor.onEnd();
            } catch (Exception e) {
                e.printStackTrace();
                log.error("结束处理器{}执行时发生异常：{}", processor.getClass().getName(), e.getMessage());
            }
        }
    }

    private static class ProcessorComparator implements Comparator<SpringProcessor> {
        @Override
        public int compare(SpringProcessor o1, SpringProcessor o2) {
            return Integer.compare(o2.getProcessorLevel(), o1.getProcessorLevel());
        }
    }
}
