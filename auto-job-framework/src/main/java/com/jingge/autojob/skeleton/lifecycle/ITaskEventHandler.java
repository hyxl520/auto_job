package com.jingge.autojob.skeleton.lifecycle;

import com.jingge.autojob.skeleton.framework.event.IEventHandler;
import com.jingge.autojob.skeleton.lifecycle.event.TaskEvent;

/**
 * 任务事件处理器
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/05 9:36
 */
public interface ITaskEventHandler<E extends TaskEvent> extends IEventHandler<E> {
    /**
     * 当前事件处理器的级别，高级别的会被优先执行
     *
     * @return int
     * @author Huang Yongxiang
     * @date 2022/8/16 13:57
     */
    default int getHandlerLevel() {
        return 0;
    }
}
