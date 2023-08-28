package com.jingge.autojob.skeleton.db;

import com.jingge.autojob.skeleton.enumerate.DatabaseType;

/**
 * 分页管理
 *
 * @author Huang Yongxiang
 * @date 2023-02-02 13:49
 * @email 1158055613@qq.com
 */
public class PageManager {
    private String database;

    public PageManager(String database) {
        this.database = database;
    }

    public PageManager(DatabaseType databaseType) {
        this.database = databaseType.getName();
    }

    public String getPageSql(String sql, int pageNum, int pageSize) {
        PageableSqlWrapper wrapper = getSqlWrapper();
        return wrapper.wrap(sql, pageNum, pageSize);
    }

    private PageableSqlWrapper getSqlWrapper() {
        database = database.toLowerCase();
        switch (database) {
            case "mysql":
                return new MySqlWrapper();
            case "postgresql":
                return new PostgreSqlWrapper();
            case "oracle":
                return new OracleWrapper();
            case "sqlserver":
                return new SqlServerWrapper();
            case "dameng":
                return new DaMengWrapper();
            default:
                throw new UnsupportedOperationException("不支持的数据库类型" + database);
        }
    }
}
