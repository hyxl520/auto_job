package com.jingge.autojob.skeleton.db.entity;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

/**
 * 配置实体
 *
 * @author Huang Yongxiang
 * @date 2023-01-04 15:00
 * @email 1158055613@qq.com
 */
@Getter
@Setter
public class AutoJobConfigEntity {
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 任务ID
     */
    private Long taskId;
    /**
     * 配置内容
     */
    private String content;
    /**
     * 配置类型
     */
    private String contentType;
    /**
     * 序列化类型
     */
    private String serializationType;
    /**
     * 是否启用 0-未启用 1-已启用
     */
    private Integer status;
    /**
     * 写入时间戳
     */
    private Long writeTimestamp;
    /**
     * 创建时间
     */
    private Timestamp createTime;
    /**
     * 删除标识
     */
    private Integer delFlag;
}
