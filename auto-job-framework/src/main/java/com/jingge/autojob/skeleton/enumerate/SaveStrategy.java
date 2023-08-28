package com.jingge.autojob.skeleton.enumerate;

import com.jingge.autojob.skeleton.framework.task.AutoJobTask;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 保存策略
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-08-22 10:43
 * @email 1158055613@qq.com
 */
public enum SaveStrategy {
    /**
     * 如果不存在则保存，存在不作任何操作
     */
    SAVE_IF_ABSENT {
        @Override
        public List<AutoJobTask> filterTasks(List<AutoJobTask> tasks) {
            return tasks
                    .stream()
                    .filter(item -> item.getSaveStrategy() == SAVE_IF_ABSENT)
                    .collect(Collectors.toList());
        }
    },
    /**
     * 新建一个版本，覆盖原有的版本，每次调度将以新的版本调度
     */
    NEW_VERSION {
        @Override
        public List<AutoJobTask> filterTasks(List<AutoJobTask> tasks) {
            return tasks
                    .stream()
                    .filter(item -> item.getSaveStrategy() == NEW_VERSION)
                    .collect(Collectors.toList());
        }
    },
    /**
     * 不存在则保存，存在则更新
     */
    UPDATE {
        @Override
        public List<AutoJobTask> filterTasks(List<AutoJobTask> tasks) {
            return tasks
                    .stream()
                    .filter(item -> item.getSaveStrategy() == UPDATE)
                    .collect(Collectors.toList());
        }
    };

    public List<AutoJobTask> filterTasks(List<AutoJobTask> tasks) {
        throw new UnsupportedOperationException();
    }
}
