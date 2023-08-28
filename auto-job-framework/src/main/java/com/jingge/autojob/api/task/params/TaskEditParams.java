package com.jingge.autojob.api.task.params;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Huang Yongxiang
 * @date 2022-12-01 14:10
 * @email 1158055613@qq.com
 */
@Getter
@Setter
public class TaskEditParams {
    /**
     * 任务别名
     */
    protected String alias;
    /**
     * 归属于
     */
    protected Long belongTo;
    /**
     * 任务优先级
     */
    protected Integer taskLevel;
}
