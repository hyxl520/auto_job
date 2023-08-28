package com.jingge.autojob.skeleton.model.handler;

import com.jingge.autojob.skeleton.framework.config.AutoJobConfigHolder;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.framework.task.AutoJobWrapper;
import com.jingge.autojob.skeleton.model.scheduler.AutoJobScanner;
import com.jingge.autojob.util.id.IdGenerator;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Huang Yongxiang
 * @date 2022-12-03 17:46
 * @email 1158055613@qq.com
 */
@Slf4j
public class AutoJobAnnotationTaskHandler extends AbstractAnnotationTaskHandler {
    private final Class<? extends Annotation> type;

    public AutoJobAnnotationTaskHandler(AbstractAnnotationFilter filter, AutoJobWrapper wrapper, Class<? extends Annotation> type) {
        super(AbstractAnnotationFilter
                .builder()
                .addHandler(new ConditionFilter())
                .addHandler(new ClasspathFilter())
                .build()
                .add(filter), wrapper);
        this.type = type;
    }

    public AutoJobAnnotationTaskHandler(AutoJobWrapper wrapper, Class<? extends Annotation> type) {
        this(null, wrapper, type);
    }

    @Override
    public Set<Method> scanMethods(String... pattern) {
        AutoJobScanner scanner = new AutoJobScanner(pattern);
        return scanner.scanMethods(type);
    }

    @Override
    public Set<Class<?>> scanClasses(String... pattern) {
        AutoJobScanner scanner = new AutoJobScanner(pattern);
        return scanner.scanClasses(type);
    }

    @Override
    public int loadMethod(Set<Method> filteredMethods, AutoJobWrapper wrapper) {
        List<AutoJobTask> memoryTypeMethods = new ArrayList<>();
        List<AutoJobTask> dbTypeMethods = new ArrayList<>();
        for (Method method : filteredMethods) {
            AutoJobTask task = wrapper.wrapper(method, method.getDeclaringClass());
            if (task == null) {
                continue;
            }
            if (!task.isExecutable()) {
                log.error("方法：{}包装失败", method.getName());
                continue;
            }
            if (task.getSchedulingStrategy() == null) {
                throw new IllegalArgumentException("方法：" + task.getAlias() + "没有指定调度策略");
            }
            if (task.getId() == null) {
                task.setId(IdGenerator.getNextIdAsLong());
            }
            if (task.getVersionId() == null) {
                task.setVersionId((long) Math.abs(task
                        .getReference()
                        .hashCode()));
            }
            if (task.getType() == AutoJobTask.TaskType.MEMORY_TASk) {
                memoryTypeMethods.add(task);
            } else if (task.getType() == AutoJobTask.TaskType.DB_TASK) {
                dbTypeMethods.add(task);
            }
        }
        if (AutoJobConfigHolder
                .getInstance()
                .getAutoJobConfig()
                .getEnableCluster()) {
            return new DBTaskLoader().load(dbTypeMethods) + new MemoryTaskLoader().load(memoryTypeMethods
                    .stream()
                    .filter(item -> item.getIsChildTask() != null && item.getIsChildTask())
                    .collect(Collectors.toList()));
        }
        return new MemoryTaskLoader().load(memoryTypeMethods) + new DBTaskLoader().load(dbTypeMethods);
    }

    @Override
    public int loadClasses(Set<Class<?>> filterClasses, AutoJobWrapper wrapper) {
        List<AutoJobTask> memoryTypeMethods = new ArrayList<>();
        List<AutoJobTask> dbTypeMethods = new ArrayList<>();
        for (Class<?> clazz : filterClasses) {
            AutoJobTask task = wrapper.wrapper(null, clazz);
            if (task == null) {
                continue;
            }
            if (task.getType() == AutoJobTask.TaskType.MEMORY_TASk) {
                memoryTypeMethods.add(task);
            } else if (task.getType() == AutoJobTask.TaskType.DB_TASK) {
                dbTypeMethods.add(task);
            }
        }
        if (AutoJobConfigHolder
                .getInstance()
                .getAutoJobConfig()
                .getEnableCluster()) {
            return new DBTaskLoader().load(dbTypeMethods) + new MemoryTaskLoader().load(memoryTypeMethods
                    .stream()
                    .filter(item -> item.getIsChildTask() != null && item.getIsChildTask())
                    .collect(Collectors.toList()));
        }
        return new MemoryTaskLoader().load(memoryTypeMethods) + new DBTaskLoader().load(dbTypeMethods);
    }
}
