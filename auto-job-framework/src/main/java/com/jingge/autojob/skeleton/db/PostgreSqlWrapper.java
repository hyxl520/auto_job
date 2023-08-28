package com.jingge.autojob.skeleton.db;

/**
 * PostgresSql分页
 *
 * @author Huang Yongxiang
 * @date 2023-01-12 17:44
 * @email 1158055613@qq.com
 */
public class PostgreSqlWrapper implements PageableSqlWrapper {
    @Override
    public String wrap(String sql, int pageNum, int pageSize) {
        int skip = (pageNum - 1) * pageSize;
        return String.format("%s limit %d offset %d", sql, pageSize, skip);
    }
}
