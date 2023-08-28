package com.jingge.autojob.skeleton.model.handler;

import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;
import com.jingge.autojob.skeleton.enumerate.SchedulingStrategy;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.config.AutoJobConstant;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.model.task.method.MethodTask;
import com.jingge.autojob.skeleton.model.task.script.ScriptTask;
import com.jingge.autojob.util.convert.DateUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DB任务加载器
 *
 * @author Huang Yongxiang
 * @date 2022-12-04 10:59
 * @email 1158055613@qq.com
 */
@Slf4j
public class DBTaskLoader implements AutoJobTaskLoader {
    @Override
    public int load(List<AutoJobTask> tasks) {
        List<AutoJobTask> methodTasks = new ArrayList<>();
        List<AutoJobTask> scriptTask = new ArrayList<>();
        tasks.forEach(task -> {
            if (task instanceof MethodTask) {
                methodTasks.add(task);
            } else if (task instanceof ScriptTask) {
                scriptTask.add(task);
            }
        });
        AutoJobMapperHolder.METHOD_TASK_ENTITY_MAPPER.saveTasks(methodTasks);
        AutoJobMapperHolder.SCRIPT_TASK_ENTITY_MAPPER.saveTasks(scriptTask);
        List<AutoJobTask> soon = tasks
                .stream()
                .filter(task -> task.getSchedulingStrategy() != SchedulingStrategy.ONLY_SAVE && task.getSchedulingStrategy() != SchedulingStrategy.AS_CHILD_TASK && task
                        .getTrigger()
                        .isNearTriggeringTime(AutoJobConstant.dbSchedulerRate * 2))
                .collect(Collectors.toList());
        soon.forEach(item -> {
            log.info("任务{}-{}已提交到调度队列", item.getId(), DateUtils.formatDateTime(item
                    .getTrigger()
                    .getTriggeringTime()));
            AutoJobApplication
                    .getInstance()
                    .getRegister()
                    .registerTask(item);
        });
        return soon.size();
    }
}
