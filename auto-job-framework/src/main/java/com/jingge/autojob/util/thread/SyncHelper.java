package com.jingge.autojob.util.thread;


import java.util.concurrent.TimeUnit;

/**
 * 线程同步工具
 *
 * @Author Huang Yongxiang
 * @Date 2022/06/07 17:43
 */

public class SyncHelper {

    @FunctionalInterface
    public interface Predicate {
        boolean test();
    }


    /**
     * 阻塞等待退出谓词值为true，该方法不会忽略中断异常
     *
     * @param overPredicate 结束谓词
     * @return void
     * @author Huang Yongxiang
     * @date 2022/6/7 17:48
     */
    public static void aWait(Predicate overPredicate) throws InterruptedException {
        while (!overPredicate.test()) {
            Thread.sleep(1);
        }
    }

    /**
     * 阻塞等待退出谓词值为true，该方法会忽略中断异常，请确保谓词最终会变成true，否则程序将会一直阻塞
     *
     * @param overPredicate 谓词，false时阻塞直到true
     * @return void
     * @author Huang Yongxiang
     * @date 2022/8/26 14:30
     */
    public static void aWaitQuietly(Predicate overPredicate) {
        while (!overPredicate.test()) {
            sleepQuietly(1, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 中断等待一段时间，期间谓词值变为true或者等待时间结束该方法随之跳出，该方法会忽略中断异常，注意调用该方法一定要确保谓词能在期待时间内为true，否则很可能导致程序死等
     *
     * @param predicate 谓词，false时阻塞，反之跳出
     * @param waitTime  等待时长
     * @param unit      时间单位
     * @return void
     * @author Huang Yongxiang
     * @date 2022/8/26 14:27
     */
    public static void aWaitQuietly(Predicate predicate, long waitTime, TimeUnit unit) {
        if (predicate == null || waitTime <= 0) {
            throw new IllegalArgumentException("等待时间应该为非负数且谓词不能为空");
        }
        long waitMills = unit.toMillis(waitTime);
        do {
            sleepQuietly(1, TimeUnit.MILLISECONDS);
        } while (--waitMills > 0 && !predicate.test());
    }

    /**
     * 静默的休眠一段时间，期间抛出的中断异常将会被直接忽略
     *
     * @param sleep 休眠时长
     * @param unit  时间单位
     * @return void
     * @author Huang Yongxiang
     * @date 2022/8/26 14:25
     */
    public static void sleepQuietly(long sleep, TimeUnit unit) {
        try {
            Thread.sleep(unit.toMillis(sleep));
        } catch (InterruptedException ignored) {
        }
    }

    public static void sleep(long sleep, TimeUnit unit) throws InterruptedException {
        Thread.sleep(unit.toMillis(sleep));
    }


}
