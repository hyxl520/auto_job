package com.jingge.autojob.skeleton.db;

/**
 * Sql server2012版本以上分页
 *
 * @author Huang Yongxiang
 * @date 2023-01-13 10:11
 * @email 1158055613@qq.com
 */
public class SqlServerWrapper implements PageableSqlWrapper {
    @Override
    public String wrap(String sql, int pageNum, int pageSize) {
        throw new UnsupportedOperationException("暂不支持sqlserver");
    }
}
