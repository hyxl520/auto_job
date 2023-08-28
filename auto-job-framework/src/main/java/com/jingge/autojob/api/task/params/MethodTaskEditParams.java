package com.jingge.autojob.api.task.params;

import lombok.Getter;
import lombok.Setter;

/**
 * 方法型任务修改参数对象
 *
 * @author Huang Yongxiang
 * @date 2022-12-01 14:17
 * @email 1158055613@qq.com
 */
@Getter
@Setter
public class MethodTaskEditParams extends TaskEditParams {
    /**
     * 参数字符串
     */
    private String paramsString;
    /**
     * 任务运行类工厂
     */
    private String methodObjectFactory;
}
