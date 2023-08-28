package com.jingge.autojob.skeleton.db.mapper;

import com.jingge.autojob.skeleton.db.entity.AutoJobConfigEntity;
import com.jingge.autojob.skeleton.framework.config.DBTableConstant;

/**
 * 配置mapper
 *
 * @author Huang Yongxiang
 * @date 2023-01-04 15:04
 * @email 1158055613@qq.com
 */
public class AutoJobConfigEntityMapper extends BaseMapper<AutoJobConfigEntity> {
    private static final String ALL_COLUMNS = "id, task_id, content, content_type, serialization_type, status, write_timestamp, create_time, del_flag";

    public AutoJobConfigEntityMapper() {
        super(AutoJobConfigEntity.class);
    }

    public AutoJobConfigEntity selectByTaskIdAndType(String contentType, long taskId) {
        String sql = getSelectExpression() + " where del_flag = 0 and task_id = ? and content_type = ? and status = 1";
        return queryOne(sql, taskId, contentType);
    }

    @Override
    public String getAllColumns() {
        return ALL_COLUMNS;
    }

    @Override
    public String getTableName() {
        return DBTableConstant.CONFIG_TABLE_NAME;
    }
}
