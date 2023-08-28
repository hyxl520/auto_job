package com.jingge.autojob.skeleton.model.scheduler;

/**
 * 分布式锁的接口，提供分布式锁的拓展
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-07-26 15:13
 * @email 1158055613@qq.com
 */
public interface AutoJobRunningLock {
    boolean lock(long taskID);

    boolean unlock(long taskID);
}
