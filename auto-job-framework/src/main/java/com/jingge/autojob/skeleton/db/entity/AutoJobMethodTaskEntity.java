package com.jingge.autojob.skeleton.db.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-03-15 14:53
 * @email 1158055613@qq.com
 */
@Getter
@Setter
public class AutoJobMethodTaskEntity  extends AutoJobTaskBaseEntity{
    /**
     * 预留字段，GLUE模式
     */
    private String content;

    /**
     * 任务所在类路径
     */
    private String methodClassName;

    /**
     * 任务名称
     */
    private String methodName;

    /**
     * 任务运行类工厂
     */
    private String methodObjectFactory;
}
