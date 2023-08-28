package com.jingge.autojob.skeleton.framework.task;

/**
 * 任务状态
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-03-29 15:44
 * @email 1158055613@qq.com
 */
public enum AutoJobRunningStatus {
    CREATED(0, false), SCHEDULING(1, true), LOCKED(2, true), WAITING(3, true), RUNNING(4, true), RETRYING(5, true);
    private final int flag;
    private final boolean isUpdateDB;

    public static AutoJobRunningStatus findByFlag(int flag) {
        for (AutoJobRunningStatus status : values()) {
            if (status.flag == flag) {
                return status;
            }
        }
        return null;
    }

    AutoJobRunningStatus(int flag, boolean isUpdateDB) {
        this.flag = flag;
        this.isUpdateDB = isUpdateDB;
    }

    public int getFlag() {
        return flag;
    }

    public boolean isUpdateDB() {
        return isUpdateDB;
    }
}
