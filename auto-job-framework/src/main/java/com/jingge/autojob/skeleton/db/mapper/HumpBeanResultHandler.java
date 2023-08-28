package com.jingge.autojob.skeleton.db.mapper;

import com.jingge.autojob.util.bean.ObjectUtil;
import com.jingge.autojob.util.convert.StringUtils;
import org.apache.commons.dbutils.ResultSetHandler;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 下划线命名转驼峰命名结果处理器
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/17 22:02
 */
public class HumpBeanResultHandler<T> implements ResultSetHandler<T> {
    private final Class<T> type;


    public HumpBeanResultHandler(Class<T> type) {
        this.type = type;
    }

    @Override
    public T handle(ResultSet rs) throws SQLException {
        T instance = ObjectUtil.getClassInstance(type);
        if (instance == null) {
            throw new NullPointerException();
        }
        int count = 0;
        int nullCount = 0;
        Field[] fields = ObjectUtil.getClassFields(type);
        while (rs.next()) {
            for (Field field : fields) {
                field.setAccessible(true);
                String humpName = field.getName();
                String unHumpName = StringUtils.uncamelCase(humpName);
                try {
                    Object value = rs.getObject(unHumpName, field.getType());
                    if (value == null) {
                        nullCount++;
                        continue;
                    }
                    field.set(instance, value);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (++count == 1) {
                break;
            }
        }
        if (nullCount == fields.length) {
            return null;
        }
        return instance;
    }
}
