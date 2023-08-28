package com.jingge.autojob.skeleton.framework.container;

import com.jingge.autojob.util.convert.StringUtils;

/**
 * 内存任务容器的清理策略
 *
 * @Author Huang Yongxiang
 * @Date 2022/10/14 14:58
 */
public enum CleanStrategy {
    /**
     * 清理已完成的内存任务
     */
    CLEAN_FINISHED_TASK,
    /**
     * 将已完成的内存任务移出并且移动到一个Cache中
     */
    KEEP_FINISHED_TASK;

    public static CleanStrategy findWithName(String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        name = name.toUpperCase();
        switch (name) {
            case "CLEAN_FINISHED": {
                return CLEAN_FINISHED_TASK;
            }
            case "KEEP_FINISHED": {
                return KEEP_FINISHED_TASK;
            }
        }
        return null;
    }
}
