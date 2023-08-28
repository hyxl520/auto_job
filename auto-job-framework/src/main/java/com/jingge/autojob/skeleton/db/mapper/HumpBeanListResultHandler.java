package com.jingge.autojob.skeleton.db.mapper;

import com.jingge.autojob.util.bean.ObjectUtil;
import com.jingge.autojob.util.convert.StringUtils;
import org.apache.commons.dbutils.ResultSetHandler;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 驼峰Bean List结果处理器
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/18 9:44
 */
public class HumpBeanListResultHandler<T> implements ResultSetHandler<List<T>> {
    private final Class<T> thisType;

    public HumpBeanListResultHandler(Class<T> type) {
        this.thisType = type;
    }

    @Override
    public List<T> handle(ResultSet rs) throws SQLException {
        List<T> humpResult = new ArrayList<>();
        Field[] fields = ObjectUtil.getClassFields(thisType);
        while (rs.next()) {
            T instance = ObjectUtil.getClassInstance(thisType);
            if (instance == null) {
                throw new NullPointerException();
            }
            int nullCount = 0;
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
            if (nullCount == fields.length) {
                continue;
            }
            humpResult.add(instance);
        }
        return humpResult;
    }
}
