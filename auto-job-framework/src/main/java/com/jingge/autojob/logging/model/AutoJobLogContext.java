package com.jingge.autojob.logging.model;

import com.jingge.autojob.logging.model.consumer.AutoJobLogConsumer;
import com.jingge.autojob.logging.model.memory.AutoJobLogCache;
import com.jingge.autojob.logging.model.memory.AutoJobRunLogCache;
import com.jingge.autojob.logging.model.producer.AutoJobLogHelper;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * 日志处理上下文
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/15 15:03
 */
@Getter
@Setter
@Accessors(chain = true)
@Slf4j
public class AutoJobLogContext {

    /*=================内存Cache=================>*/
    private AutoJobLogCache logCache;
    private AutoJobRunLogCache runLogCache;
    /*=======================Finished======================<*/

    /**
     * 日志管理器
     */
    private AutoJobLogConsumer logManager;
    /**
     * 日志容器
     */
    private AutoJobLogContainer logContainer;

    private AutoJobLogContext() {
    }

    public static AutoJobLogContext getInstance() {
        return InstanceHolder.CONTEXT;
    }


    public static class InstanceHolder {
        private static final AutoJobLogContext CONTEXT = new AutoJobLogContext();
    }
}
