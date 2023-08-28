package com.jingge.autojob.skeleton.lang;

/**
 * 守护线程
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/26 11:20
 */
public interface WithDaemonThread {
    /**
     * 守护线程做的事情定义在该接口内部
     *
     * @return void
     * @author Huang Yongxiang
     * @date 2022/8/8 11:05
     */
    void startWork();

    default void endWork(){

    }
}
