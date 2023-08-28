package com.jingge.autojob.util.id;

import com.jingge.autojob.util.id.imp.IdSnowFlakeWorker;

/**
 * ID生成器，使用雪花算法
 *
 * @Auther Huang Yongxiang
 * @Date 2022/01/24 12:30
 */
public class IdGenerator {
    public static IdWorker instance() {
        return InstanceHolder.ID_WORKER;
    }

    /**
     * 获取ID
     *
     * @return java.lang.Long
     * @author Huang Yongxiang
     * @date 2022/1/24 13:40
     */
    public static Long getNextIdAsLong() {
        return instance().getAsLong();
    }

    public static String getNextIdAsString() {
        return instance().getAsString();
    }

    public static class InstanceHolder {
        private static final IdWorker ID_WORKER = new IdSnowFlakeWorker();
    }

}
