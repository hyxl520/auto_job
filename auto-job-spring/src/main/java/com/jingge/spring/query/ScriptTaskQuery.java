package com.jingge.spring.query;

import lombok.Getter;
import lombok.Setter;

/**
 * 脚本任务query
 *
 * @author Huang Yongxiang
 * @date 2022-12-20 10:27
 * @email 1158055613@qq.com
 */
@Setter
@Getter
public class ScriptTaskQuery {
    /**
     * 类型 0-一段cmd命令 1-一个脚本内容 2-一个对应路径的脚本文件
     */
    private Integer type;
    /**
     * 内容
     */
    private String content;
    /**
     * 脚本类型，py、shell、php、nodeJs、powerShell
     */
    private String fileType;
    /**
     * 脚本路径
     */
    private String path;
    /**
     * 脚本文件名
     */
    private String scriptFileName;
    /**
     * 脚本后缀
     */
    private String suffix;
    /**
     * 启动命令
     */
    private String runCmd;
}
