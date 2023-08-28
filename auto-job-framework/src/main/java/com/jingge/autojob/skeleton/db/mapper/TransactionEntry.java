package com.jingge.autojob.skeleton.db.mapper;

import java.sql.Connection;

/**
 * 事务条目
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/18 12:08
 */
public interface TransactionEntry {
    /**
     * 事务中的一个操作，注意一定使用参数给定的连接进行sql操作，否则不保证能开启事务功能
     *
     * @param connection 开启事务的连接
     * @return int
     * @author Huang Yongxiang
     * @date 2022/8/27 9:33
     */
    int runSql(Connection connection) throws Exception;
}
