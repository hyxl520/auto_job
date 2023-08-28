package com.jingge.autojob.skeleton.model.executor;

import com.jingge.autojob.skeleton.lifecycle.ITaskEventHandler;
import com.jingge.autojob.skeleton.lifecycle.event.imp.TaskFinishedEvent;
import com.jingge.autojob.util.cache.LocalCacheManager;
import lombok.extern.slf4j.Slf4j;
import net.jodah.expiringmap.ExpirationPolicy;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

/**
 * 对method对象进行缓存，减少反射用时
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-07-13 17:02
 * @email 1158055613@qq.com
 */
@Slf4j
public class MethodObjectCache implements ITaskEventHandler<TaskFinishedEvent> {
    private final LocalCacheManager<Long, Method> cache;
    private final int total;
    private final DecimalFormat format = new DecimalFormat("#.####");

    public MethodObjectCache(int maxLength, long expiringTime, TimeUnit unit) {
        cache = LocalCacheManager
                .builder()
                .setExpiringTime(expiringTime, unit)
                .setEntriesExpiration(false)
                .setPolicy(ExpirationPolicy.ACCESSED)
                .setMaxLength(maxLength)
                .build();
        total = maxLength;
    }

    /**
     * 返回已使用的百分比
     *
     * @return java.lang.String
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/7/14 9:27
     */
    public String use() {
        return format.format((cache.size() * 100.0) / total);
    }

    public void set(long taskID, Method method) {
        cache.set(taskID, method);
    }

    public void remove(long taskID) {
        cache.remove(taskID);
    }

    public boolean exist(long taskID) {
        return cache.exist(taskID);
    }

    public Method get(long taskID) {
        return cache.get(taskID);
    }

    @Override
    public void doHandle(TaskFinishedEvent event) {
        remove(event
                .getTask()
                .getId());
    }
}
