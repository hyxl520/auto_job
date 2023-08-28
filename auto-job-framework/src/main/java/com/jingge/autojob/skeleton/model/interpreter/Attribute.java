package com.jingge.autojob.skeleton.model.interpreter;

import lombok.*;

/**
 * 参数对象
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/06 15:35
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Attribute {
    /**
     * 参数类型
     */
    private Class<?> type;
    /**
     * 参数值
     */
    private Object value;
    /**
     * 参数下标
     */
    private int pos;
}
