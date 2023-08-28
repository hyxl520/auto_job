package com.jingge.autojob.skeleton.enumerate;

import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.util.convert.StringUtils;

/**
 * @author Huang Yongxiang
 * @date 2022-12-29 17:08
 * @email 1158055613@qq.com
 */
public enum DatabaseType {
    MY_SQL("mysql"), POSTGRES_SQL("postgresql"), ORACLE("oracle"), DA_MENG("dameng"), SQL_SERVER("sqlserver");

    private final String name;

    DatabaseType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static DatabaseType findByName(String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        for (DatabaseType type : DatabaseType.values()) {
            if (name
                    .trim()
                    .equalsIgnoreCase(type.name)) {
                return type;
            }
        }
        return null;
    }

    public static DatabaseType getCurrentDatabaseType() {
        return AutoJobApplication
                .getInstance()
                .getConfigHolder()
                .getAutoJobConfig()
                .getDatabaseType();
    }
}
