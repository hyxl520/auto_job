package com.jingge.autojob.skeleton.db.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-03-15 14:53
 * @email 1158055613@qq.com
 */
@Getter
@Setter
public class AutoJobScriptTaskEntity extends AutoJobTaskBaseEntity {
    /**
     * 任务内容，用于存放脚本任务的脚本
     */
    private String scriptContent;
    /**
     * 脚本路径
     */
    private String scriptPath;
    /**
     * 脚本文件名
     */
    private String scriptFileName;
    /**
     * 脚本命令行
     */
    private String scriptCmd;
}
