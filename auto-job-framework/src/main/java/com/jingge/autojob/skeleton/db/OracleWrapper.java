package com.jingge.autojob.skeleton.db;

/**
 * oracle分页
 *
 * @author Huang Yongxiang
 * @date 2023-01-12 17:46
 * @email 1158055613@qq.com
 */
public class OracleWrapper implements PageableSqlWrapper {
    @Override
    public String wrap(String sql, int pageNum, int pageSize) {
        int skip = (pageNum - 1) * pageSize;
        return String.format("select * from (select a.*, rownum rn from (%s) a where rownum <= %d) where rn >= %d", sql, skip, skip + pageSize);
    }
}
