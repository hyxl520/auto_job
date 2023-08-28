package com.jingge.autojob.skeleton.db;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.lang.AutoJobException;
import com.jingge.autojob.util.convert.StringUtils;
import com.jingge.autojob.util.io.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 连接池持有者
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/17 16:31
 */
@Slf4j
public class DataSourceHolder {
    // 声明连接池对象
    private final DataSource dataSource;

    public DataSourceHolder() {
        this(AutoJobApplication
                .getInstance()
                .getEnv());
    }

    public DataSourceHolder(String profile) {
        try {
            if (StringUtils.isEmpty(profile)) {
                dataSource = DruidDataSourceFactory.createDataSource(PropertiesHolder
                        .builder()
                        .addPropertiesFile("druid.properties")
                        .build()
                        .getProperties());
                log.info("AutoJob未配置环境变量，本次读取druid.properties作为数据源配置");
            } else {
                String loadFile = String.format("druid-%s.properties", profile);
                dataSource = DruidDataSourceFactory.createDataSource(PropertiesHolder
                        .builder()
                        .addPropertiesFile(loadFile)
                        .build()
                        .getProperties());
                log.info("AutoJob的环境变量为{}，本次读取druid-{}.properties作为数据源配置", profile, profile);
            }
            if (!isAvailable()) {
                throw new AutoJobException("数据源不可用");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("druid连接池初始化失败...");
        }
    }

    public boolean isAvailable() {
        Connection connection = null;
        try {
            connection = getConnection();
            return connection != null;
        } finally {
            release(connection);
        }
    }


    public DataSourceHolder(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void startTransaction(Connection connection) {
        if (connection != null) {
            try {
                connection.setAutoCommit(false);
            } catch (SQLException ignored) {
            }
        }
    }

    public void commit(Connection connection) throws SQLException {
        if (connection != null) {
            connection.commit();
        }
    }

    public void rollbackQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            } finally {
                release(connection);
            }
        }
    }


    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void release(ResultSet resultSet, Statement statement, Connection connection) {
        // 关闭ResultSet
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        // 关闭Statement
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        // 关闭Connection
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // 方法重载
    public void release(Statement statement, Connection connection) {
        release(null, statement, connection);
    }

    public void release(Connection connection) {
        release(null, null, connection);
    }


}
