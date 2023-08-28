package com.jingge.autojob.logging.model.handler;

import com.jingge.autojob.skeleton.lang.WithDaemonThread;
import com.jingge.autojob.util.thread.ScheduleTaskUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 日志处理器轮询
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-03-24 10:44
 * @email 1158055613@qq.com
 */
@Slf4j
public class AutoJobLogLoop implements WithDaemonThread {
    private volatile boolean isRun = false;
    private Map<Long, AutoJobLogHandler> logHandlerMap;

    public static AutoJobLogLoop getInstance() {
        return InstanceHolder.LOG_LOOP;
    }

    public void bound(Map<Long, AutoJobLogHandler> logHandlerMap) {
        this.logHandlerMap = logHandlerMap;
        startWork();
    }

    @Override
    public void startWork() {
        if (!isRun) {
            synchronized (AutoJobLogLoop.class) {
                if (isRun) {
                    return;
                }
            }
        }
        log.info("log handler loop start");
        isRun = true;
        ScheduleTaskUtil
                .build(true, "handlerLoop")
                .EFixedRateTask(() -> {
                    if (logHandlerMap != null && logHandlerMap.size() > 0) {
                        for (Map.Entry<Long, AutoJobLogHandler> entry : logHandlerMap.entrySet()) {
                            if (entry.getValue() != null && entry
                                    .getValue()
                                    .reachSavingTime()) {
                                entry
                                        .getValue()
                                        .save();
                            }
                        }
                    }
                }, 0, 1, TimeUnit.SECONDS);
    }

    static class InstanceHolder {
        private static final AutoJobLogLoop LOG_LOOP = new AutoJobLogLoop();
    }
}
