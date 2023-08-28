package com.jingge.autojob.skeleton.model.task.functional;

import com.jingge.autojob.skeleton.framework.task.AutoJobRunningContext;

/**
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-08-24 15:37
 * @email 1158055613@qq.com
 */
@FunctionalInterface
public interface Function {
    void run(AutoJobRunningContext context);
}
