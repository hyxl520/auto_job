package com.jingge.autojob.api.task.params;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 脚本任务修改参数对象
 *
 * @author Huang Yongxiang
 * @date 2022-12-01 14:17
 * @email 1158055613@qq.com
 */
@Setter
@Getter
public class ScriptTaskEditParams extends TaskEditParams {
    private List<String> attributes;
}
