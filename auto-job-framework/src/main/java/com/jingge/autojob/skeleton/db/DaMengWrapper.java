package com.jingge.autojob.skeleton.db;

/**
 * 达梦数据库分页
 *
 * @author Huang Yongxiang
 * @date 2023-01-12 17:54
 * @email 1158055613@qq.com
 */
public class DaMengWrapper implements PageableSqlWrapper{
    @Override
    public String wrap(String sql, int pageNum, int pageSize) {
        int skip = (pageNum - 1) * pageSize;
        return String.format("%s limit %d offset %d", sql, pageSize, skip);
    }
}
