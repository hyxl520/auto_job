package com.jingge.autojob.skeleton.framework.config;

/**
 * @Description 集群的一些常量
 * @Author Huang Yongxiang
 * @Date 2022/07/23 10:37
 */
public class ClusterConstant {
    /**
     * 集群节点通信秘钥所属Header名
     */
    public static final String HEADER_KEY = "auto-job-cluster-key";
    /**
     * 集群节点通信controller路径
     */
    public static final String API_PATH = ".*.skeleton.cluster.api.controller.*";

    public static final String TOKEN_QUERY = "TOKEN";

    public static final String LOG_KEY_QUERY = "KEY";

    public static final String HOST_QUERY = "HOST";

    public static final String PORT_QUERY = "PORT";

}
