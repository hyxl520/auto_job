package com.jingge.autojob.skeleton.framework.config;

import java.util.concurrent.TimeUnit;

/**
 * 一些常用的时间戳
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/26 11:55
 */
public class TimeConstant {
    public static final long A_MILLS = 1;
    public static final long A_SECOND = 1000;
    public static final long A_MINUTE = TimeUnit.MINUTES.toMillis(1);
    public static final long A_HOUR = TimeUnit.HOURS.toMillis(1);
    public static final long A_DAY = TimeUnit.DAYS.toMillis(1);
    public static final long A_WEEK = 7 * A_DAY;
    public static final long A_MONTH_30 = 30 * A_DAY;
    public static final long A_MONTH_31 = 31 * A_DAY;
    public static final long A_YEAR_365 = 365 * A_DAY;
    public static final long A_YEAR_366 = 366 * A_DAY;

}
