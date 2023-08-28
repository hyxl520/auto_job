package com.jingge.spring.processor;

/**
 * @Author Huang Yongxiang
 * @Date 2022/09/19 14:04
 */
public interface SpringProcessor {
    default int getProcessorLevel(){
        return 0;
    }
}
