package com.jingge.autojob.util.servlet;

import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.util.convert.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.net.Inet4Address;
import java.net.SocketException;
import java.util.Optional;

/**
 * 网络工具类
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/22 16:33
 */
@Slf4j
public class InetUtil {
    public static String localhostIP;

    public static String getLocalhostIp() {
        if (!StringUtils.isEmpty(localhostIP)) {
            //log.warn("返回缓存IP");
            return localhostIP;
        }
        try {
            Optional<Inet4Address> optional = IpUtil.getLocalIp4Address();
            if (optional != null && optional.isPresent()) {
                String ip = optional
                        .get()
                        .getHostAddress();
                InetUtil.localhostIP = ip;
                return ip;
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int getPort() {
        return AutoJobApplication
                .getInstance()
                .getConfigHolder()
                .getClusterConfig()
                .getPort();
    }

    public static String getTCPAddress() {
        return String.format("%s:%d", getLocalhostIp(), getPort());
    }

}
