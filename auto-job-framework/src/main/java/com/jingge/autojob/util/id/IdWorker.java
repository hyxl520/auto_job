package com.jingge.autojob.util.id;

import java.math.BigDecimal;

/**
 * ID生成的统一接口
 *
 * @Auther Huang Yongxiang
 * @Date 2022/01/24 13:48
 */
public interface IdWorker {
    /**
     * 核心算法
     *
     * @return java.lang.Long
     * @author Huang Yongxiang
     * @date 2022/1/24 13:54
     */
    Object nextId();

    /**
     * 返回生成ID的long格式
     *
     * @return long
     * @author Huang Yongxiang
     * @date 2022/1/24 14:02
     */
    long getAsLong();

    String getAsString();

    /**
     * 返回生成ID的int格式
     *
     * @return int
     * @author Huang Yongxiang
     * @date 2022/1/24 14:02
     */
    int getAsInteger();

    /**
     * 返回生成ID的BigDecimal格式
     *
     * @return java.math.BigDecimal
     * @author Huang Yongxiang
     * @date 2022/1/24 14:13
     */
    BigDecimal getAsBigDecimal();

}
