package com.jingge.autojob.api.task;

import com.jingge.autojob.api.task.params.TaskEditParams;
import com.jingge.autojob.api.task.params.TriggerEditParams;
import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.framework.task.AutoJobTrigger;
import com.jingge.autojob.skeleton.framework.task.AutoJobContext;
import com.jingge.autojob.skeleton.model.task.method.MethodTask;
import com.jingge.autojob.skeleton.model.task.script.ScriptTask;

import java.util.List;

/**
 * 任务操作的API接口
 *
 * @Author Huang Yongxiang
 * @Date 2022/10/14 13:52
 */
public interface AutoJobAPI {

    List<MethodTask> pageMethodTask(int pageNum,int size);

    List<ScriptTask> pageScriptTask(int pageNum,int size);

    /**
     * 返回当前任务数目
     *
     * @return int
     * @author Huang Yongxiang
     * @date 2022/11/1 15:36
     */
    int count();

    /**
     * 注册一个任务
     *
     * @param task 要注册的任务
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/10/14 14:02
     */
    boolean registerTask(AutoJobTask task);

    /**
     * 立即执行一个任务，并且只执行一次
     *
     * @param task 要立即执行的任务
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/10/14 14:03
     */
    boolean runTaskNow(AutoJobTask task);

    /**
     * 通过任务ID查找到某个任务
     *
     * @param taskId 任务Id
     * @return com.jingge.autojob.skeleton.framework.task.AutoJobTask
     * @author Huang Yongxiang
     * @date 2022/10/14 14:03
     */
    AutoJobTask find(long taskId);

    /**
     * 对任务的调度信息进行编辑
     *
     * @param taskId            任务ID
     * @param triggerEditParams 调度器信息，存在属性将会作为修改项
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/10/14 14:04
     */
    boolean editTrigger(long taskId, TriggerEditParams triggerEditParams);

    /**
     * 绑定触发器到指定任务上，原有的触发器将会被覆盖
     *
     * @param taskId  任务ID
     * @param trigger 触发器
     * @return java.lang.Boolean
     * @author Huang Yongxiang
     * @date 2022/12/22 17:26
     */
    boolean bindingTrigger(long taskId, AutoJobTrigger trigger);

    /**
     * 对任务的基本信息进行编辑
     *
     * @param taskId         任务Id
     * @param taskEditParams 任务信息，存在的属性会作为修改项
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/10/17 14:33
     */
    boolean editTask(long taskId, TaskEditParams taskEditParams);

    /**
     * 停止一个任务的后续调度
     *
     * @param taskId 任务ID
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/10/14 14:05
     */
    boolean pause(long taskId);

    /**
     * 恢复一个任务的调度
     *
     * @param taskId 任务ID
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/10/14 14:05
     */
    boolean unpause(long taskId);

    /**
     * 删除一个任务，包含其调度器信息
     *
     * @param taskId 任务ID
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/10/14 14:06
     */
    boolean delete(long taskId);

    /**
     * 判断是否存在该任务
     *
     * @param taskId 任务ID
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/10/14 14:06
     */
    boolean isExist(long taskId);

    /**
     * 获取任务类型
     *
     * @param taskId 任务ID
     * @return AutoJobTask.TaskType
     * @author Huang Yongxiang
     * @date 2022/12/2 11:45
     */
    default AutoJobTask.TaskType getTaskType(long taskId) {
        if (AutoJobApplication
                .getInstance()
                .getMemoryTaskContainer()
                .getById(taskId) != null) {
            return AutoJobTask.TaskType.MEMORY_TASk;
        }
        if (AutoJobMapperHolder.METHOD_TASK_ENTITY_MAPPER
                .selectById(taskId)
                .getId() != null || AutoJobMapperHolder.SCRIPT_TASK_ENTITY_MAPPER
                .selectById(taskId)
                .getId() != null) {
            return AutoJobTask.TaskType.DB_TASK;
        }
        return null;
    }

    /**
     * 判断一个任务是否正在运行
     *
     * @param taskId 任务ID
     * @return boolean 任务不存在或不在运行返回false
     * @author Huang Yongxiang
     * @date 2022/10/14 14:08
     */
    default boolean isRunning(long taskId) {
        return AutoJobContext
                .getRunningTask()
                .containsKey(taskId);
    }

    /**
     * 尝试停止一个正在运行的任务
     *
     * @param taskId 任务ID
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/10/14 14:08
     */
    default boolean stopRunningTask(long scheduleID) {
        return AutoJobContext.stopRunningTask(scheduleID);
    }
}
