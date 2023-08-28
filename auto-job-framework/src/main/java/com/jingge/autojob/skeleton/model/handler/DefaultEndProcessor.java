package com.jingge.autojob.skeleton.model.handler;

import com.jingge.autojob.logging.model.producer.AutoJobLogHelper;
import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.processor.IAutoJobEnd;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.framework.task.AutoJobContext;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 默认关闭处理器
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/21 15:27
 */
public class DefaultEndProcessor implements IAutoJobEnd {
    @Override
    public void end() {
        AutoJobLogHelper logger = AutoJobLogHelper.getInstance();
        //退出前释放该节点持有的任务锁
        logger.info("释放{}个DB任务锁成功", AutoJobApplication.getInstance().getAutoJobContext().unlock());
        /*=================将正在运行的DB任务运行态更新为结束态=================>*/
        List<Long> ids = AutoJobContext
                .getRunningTask()
                .values()
                .stream()
                .filter(item -> item.getType() == AutoJobTask.TaskType.DB_TASK)
                .map(AutoJobTask::getId)
                .collect(Collectors.toList());
        if (ids.size() > 0) {
            int count = AutoJobMapperHolder.TRIGGER_ENTITY_MAPPER.updateOperatingStatuses(false, ids);
            logger.info("更新{}个任务的运行状态为结束态", count);
        }
        /*=======================Finished======================<*/
    }
}
