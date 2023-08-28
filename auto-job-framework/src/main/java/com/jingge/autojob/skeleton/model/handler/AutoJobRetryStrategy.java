package com.jingge.autojob.skeleton.model.handler;

import com.jingge.autojob.skeleton.framework.task.AutoJobTask;

/**
 * 重试策略的抽象接口
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-07-24 17:15
 * @email 1158055613@qq.com
 */
public interface AutoJobRetryStrategy {
    boolean retry(AutoJobTask task);
}
