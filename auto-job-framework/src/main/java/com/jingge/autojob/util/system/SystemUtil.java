package com.jingge.autojob.util.system;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;

/**
 * 系统工具类
 *
 * @Auther Huang Yongxiang
 * @Date 2022/03/31 12:17
 */
public class SystemUtil {
    private static final OperatingSystemMXBean operatingSystemMXBean;

    static {
        operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    }

    /**
     * 获取cpu使用率
     */
    public static double getSystemCpuLoad() {
        return operatingSystemMXBean.getSystemCpuLoad();
    }

    /**
     * 获取cpu数量
     */
    public static int getSystemCpuCount() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static void main(String[] args) {
        double use = 0.0;
        System.out.println(getSystemCpuCount());
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            use = getSystemCpuLoad();
            System.out.println(use + "%");
        } while (true);

        //System.out.println(getSystemCpuCount());
    }

}
