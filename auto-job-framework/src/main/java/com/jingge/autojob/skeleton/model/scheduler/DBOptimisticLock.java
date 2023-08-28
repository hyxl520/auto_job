package com.jingge.autojob.skeleton.model.scheduler;

import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;

/**
 * 基于DB的乐观锁实现，这是默认实现
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-07-26 15:14
 * @email 1158055613@qq.com
 */
public class DBOptimisticLock implements AutoJobRunningLock {
    @Override
    public boolean lock(long taskID) {
        try {
            if (!AutoJobMapperHolder
                    .getMatchTaskMapper(taskID)
                    .lock(taskID)) {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean unlock(long taskID) {
        return AutoJobMapperHolder
                .getMatchTaskMapper(taskID)
                .unLock(taskID);
    }
}
