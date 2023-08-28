package com.jingge.autojob.skeleton.model.handler;

import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import lombok.extern.slf4j.Slf4j;

/**
 * 故障转移重试策略
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-07-24 17:20
 * @email 1158055613@qq.com
 */
@Slf4j
public class FailoverStrategy implements AutoJobRetryStrategy {
    @Override
    public boolean retry(AutoJobTask task) {
        if (AutoJobApplication
                .getInstance()
                .getConfigHolder()
                .getAutoJobConfig()
                .getEnableCluster()) {
            task
                    .getTrigger()
                    .getCurrentRepeatTimes()
                    .incrementAndGet();
            return AutoJobApplication
                    .getInstance()
                    .getTransferManager()
                    .addTask(task);
        }
        log.warn("系统未开启集群模式，不支持故障转移重试");
        return false;
    }
}
