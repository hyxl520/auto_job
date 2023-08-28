package com.jingge.autojob.util.thread;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 可中断任务辅助类
 *
 * @Auther Huang Yongxiang
 * @Date 2022/03/23 11:07
 */
public class ThreadHelper {
    public static Thread thisThread() {
        return Thread.currentThread();
    }

    public static void printCurrentThreadStackTrace() {
        Arrays
                .stream(thisThread().getStackTrace())
                .forEach(System.out::println);
    }

    public List<StackTraceElement> getStackTrace(){
        return Arrays.asList(thisThread().getStackTrace());
    }

    /**
     * 当前线程是否已被终止
     *
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/4/15 10:21
     */
    public static boolean isInterrupt() {
        return thisThread().isAlive() && thisThread().isInterrupted();
    }

    /**
     * 添加一个中断标记，当尝试中断该线程时将抛出异常，终止该线程运行，调用者应该对异常进行处理，防止程序终止
     *
     * @return void
     * @author Huang Yongxiang
     * @date 2022/3/23 11:12
     */
    public static void addInterruptMark() throws InterruptedException {
        if (isInterrupt()) {
            throw new InterruptedException("中断标记触发");
        }
    }
}
