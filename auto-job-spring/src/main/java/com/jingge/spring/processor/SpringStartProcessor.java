package com.jingge.spring.processor;

/**
 * Spring启动处理器
 *
 * @Author Huang Yongxiang
 * @Date 2022/09/19 9:45
 */
public interface SpringStartProcessor extends SpringProcessor{
    void onStart();
}
