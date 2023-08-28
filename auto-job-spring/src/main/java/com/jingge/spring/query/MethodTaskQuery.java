package com.jingge.spring.query;

import lombok.Data;

/**
 * @Author Huang Yongxiang
 * @Date 2022/11/02 16:50
 * @Email 1158055613@qq.com
 */
@Data
public class MethodTaskQuery {
    private String taskClass;
    private String taskName;
    private String attributes;
    private Integer repeatTimes;
    private Long cycle;
}
