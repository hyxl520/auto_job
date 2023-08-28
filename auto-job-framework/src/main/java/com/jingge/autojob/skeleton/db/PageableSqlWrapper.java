package com.jingge.autojob.skeleton.db;

/**
 * 分页sql包装器
 *
 * @author Huang Yongxiang
 * @date 2023-01-12 17:32
 * @email 1158055613@qq.com
 */
public interface PageableSqlWrapper {
    String wrap(String sql, int pageNum, int pageSize);
}
