package com.jingge.autojob.skeleton.db;

/**
 * MySQL分页
 *
 * @author Huang Yongxiang
 * @date 2023-01-12 17:42
 * @email 1158055613@qq.com
 */
public class MySqlWrapper implements PageableSqlWrapper {
    @Override
    public String wrap(String sql, int pageNum, int pageSize) {
        int skip = (pageNum - 1) * pageSize;
        return String.format("%s limit %d, %d", sql, skip, pageSize);
    }
}
