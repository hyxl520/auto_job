package com.jingge.autojob.skeleton.enumerate;

import com.jingge.autojob.util.convert.StringUtils;

/**
 * 常用脚本类型
 *
 * @Author Huang Yongxiang
 * @Date 2022/10/28 15:11
 * @Email 1158055613@qq.com
 */
public enum ScriptType {
    SHELL("bash", "shell", ".sh"),
    PYTHON("python", "python", ".py"),
    PHP("php", "php", ".php"),
    NODEJS("node", "nodeJs", ".js"),
    POWERSHELL("powershell", "powerShell", ".psl");
    /**
     * 前缀启动命令
     */
    private final String cmd;
    private final String name;
    private final String suffix;

    ScriptType(String cmd, String name, String suffix) {
        this.cmd = cmd;
        this.name = name;
        this.suffix = suffix;
    }

    public String getCmd() {
        return cmd;
    }

    public String getName() {
        return name;
    }

    public String getSuffix() {
        return suffix;
    }

    public static ScriptType findBySuffix(String suffix) {
        if (StringUtils.isEmpty(suffix)) {
            return null;
        }
        String trim = suffix
                .trim()
                .toLowerCase();
        suffix = trim.charAt(0) == '.' ? trim : "." + trim;
        for (ScriptType type : ScriptType.values()) {
            if (type.suffix.equals(suffix)) {
                return type;
            }
        }
        return null;
    }

    public static ScriptType findByName(String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        for (ScriptType type : ScriptType.values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
