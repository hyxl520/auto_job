package com.jingge.autojob.skeleton.model.task.functional;

import com.jingge.autojob.skeleton.framework.task.AutoJobRunningContextHolder;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.model.task.TaskExecutable;

/**
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-08-24 15:35
 * @email 1158055613@qq.com
 */
public class FunctionExecutable implements TaskExecutable {
    private final FunctionTask functionTask;

    public FunctionExecutable(FunctionTask functionTask) {
        this.functionTask = functionTask;
    }

    @Override
    public AutoJobTask getAutoJobTask() {
        return functionTask;
    }

    @Override
    public boolean isExecutable() {
        return functionTask != null && functionTask.getFunction() != null;
    }

    @Override
    public Object execute(Object... params) {
        functionTask
                .getFunction()
                .run(AutoJobRunningContextHolder.currentTaskContext());
        return null;
    }

    @Override
    public Object[] getExecuteParams() {
        return null;
    }
}
