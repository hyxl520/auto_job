package com.jingge.autojob.skeleton.model.task;

import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.framework.pool.Executable;

/**
 * 可执行的任务
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/04 10:49
 */
public interface TaskExecutable extends Executable {
    /**
     * 一个可执行的任务对象必须能转化成AutoJobTask对象实体，该对象应该为单例并且全局共享，即能保证每一次调用该方法返回的对象一定是同一个
     *
     * @return com.example.autojob.skeleton.framework.task.AutoJobTask
     * @author Huang Yongxiang
     * @date 2022/8/2 17:17
     */
    AutoJobTask getAutoJobTask();

    /**
     * 判断该可执行对象能否被执行
     *
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/11/2 17:38
     */
    boolean isExecutable();
}
