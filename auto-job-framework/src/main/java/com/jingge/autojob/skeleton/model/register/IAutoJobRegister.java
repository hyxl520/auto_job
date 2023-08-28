package com.jingge.autojob.skeleton.model.register;

import com.jingge.autojob.skeleton.framework.task.AutoJobTask;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * 任务注册器
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/04 11:04
 */
public interface IAutoJobRegister {
    /**
     * 将一个任务注册进任务调度队列
     *
     * @param task 要注册的任务
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/7/4 12:49
     */
    boolean registerTask(AutoJobTask task);

    /**
     * 注册一个任务到调度队列
     *
     * @param task     要注册的任务
     * @param isForced 是否强制注册
     * @param waitTime 队列满了等待的时间
     * @param unit     时间单位
     * @return boolean
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/8/7 15:03
     */
    boolean registerTask(AutoJobTask task, boolean isForced, long waitTime, TimeUnit unit);

    /**
     * 从任务队列取出一个任务，如果队列为空则返回null
     *
     * @return com.example.autojob.skeleton.model.tq.Task
     * @author Huang Yongxiang
     * @date 2022/7/4 12:49
     */
    AutoJobTask takeTask();

    /**
     * 从任务队列取出一个任务，如果队列为空尝试等待一段时间，等待完成后如果依然为空则返回null
     *
     * @param waitTime 等待的时长
     * @param unit     单位
     * @return com.example.autojob.skeleton.model.tq.Task
     * @author Huang Yongxiang
     * @date 2022/7/4 12:50
     */
    AutoJobTask takeTask(long waitTime, TimeUnit unit);

    /**
     * 尝试读取任务队列头的任务但不取出，如果队列为空返回null
     *
     * @return com.example.autojob.skeleton.model.tq.Task
     * @author Huang Yongxiang
     * @date 2022/7/4 12:51
     */
    AutoJobTask readTask();

    /**
     * 按照给定谓词过滤出任务队列中的特定任务
     *
     * @param predicate 谓词
     * @return List<AutoJobTask>
     * @author Huang Yongxiang
     * @date 2022/8/8 14:11
     */
    List<AutoJobTask> filter(Predicate<AutoJobTask> predicate);


    boolean removeTask(long taskId);

    /**
     * 合并并替换任务，将新任务和taskId指定的任务进行合并然后替换原来的任务对象
     *
     * @param taskId      要替换的任务Id
     * @param newInstance 新任务
     * @return com.example.autojob.skeleton.model.tq.Task 如果替换成功返回替换后的合并对象，否则返回null
     * @author Huang Yongxiang
     * @date 2022/7/4 12:51
     */
    AutoJobTask mergeAndReplaceTaskAndGet(long taskId, AutoJobTask newInstance);

    List<AutoJobTask> removeAndGetTask(long taskId);

    AutoJobTask removeAndGetTaskByScheduleQueueID(long scheduleQueueID);

    List<AutoJobTask> getTaskById(long taskId);

    AutoJobTask getTaskByScheduleQueueID(long scheduleQueueID);

    /**
     * 注册器可以设置注册过滤器链
     *
     * @param filter 过滤器链
     * @return com.example.autojob.skeleton.model.register.IAutoJobRegister
     * @author Huang Yongxiang
     * @date 2022/8/22 17:23
     */
    IAutoJobRegister setFilter(AbstractRegisterFilter filter);

    /**
     * 注册器可以设置处理器链
     *
     * @param handler 处理器链
     * @return com.example.autojob.skeleton.model.register.IAutoJobRegister
     * @author Huang Yongxiang
     * @date 2022/8/22 17:24
     */
    IAutoJobRegister setHandler(AbstractRegisterHandler handler);

    Iterator<AutoJobTask> iterator();

    int size();

}
