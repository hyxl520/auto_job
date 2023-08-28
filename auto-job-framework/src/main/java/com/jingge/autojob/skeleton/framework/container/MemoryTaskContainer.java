package com.jingge.autojob.skeleton.framework.container;

import com.jingge.autojob.skeleton.framework.task.AutoJobRunningStatus;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lang.WithDaemonThread;
import com.jingge.autojob.util.cache.LocalCacheManager;
import com.jingge.autojob.util.convert.ProtoStuffUtil;
import com.jingge.autojob.util.convert.StringUtils;
import com.jingge.autojob.util.thread.ScheduleTaskUtil;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import net.jodah.expiringmap.ExpirationPolicy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 内存任务容器
 *
 * @Author Huang Yongxiang
 * @Date 2022/10/14 14:17
 */
@Slf4j
public class MemoryTaskContainer implements WithDaemonThread {
    private final Map<Long, AutoJobTask> idContainer = new ConcurrentHashMap<>();
    private final Map<Long, AutoJobTask> annotationIDContainer = new ConcurrentHashMap<>();
    private final Map<String, AutoJobTask> aliasContainer = new ConcurrentHashMap<>();
    private final AtomicInteger size = new AtomicInteger(0);
    private LocalCacheManager<String, AutoJobTask> finishedTaskCache;
    private CleanStrategy cleanStrategy;
    private int limitSize;

    private MemoryTaskContainer() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public void insert(AutoJobTask task) {
        if (size.get() >= limitSize) {
            throw new AutoJobContainerException("已超出内存任务容器容量：" + limitSize);
        }
        if (task == null || task.getId() == null) {
            throw new IllegalArgumentException("任务为空或任务ID不存在");
        }
        if (task.getId() == null && StringUtils.isEmpty(task.getAlias()) && task.getVersionId() == null) {
            throw new IllegalArgumentException("任务ID、版本ID、任务别名必须存在一个");
        }
        if (task.getType() != AutoJobTask.TaskType.MEMORY_TASk) {
            throw new IllegalArgumentException("只能插入内存任务");
        }
        boolean flag = false;
        if (task.getId() != null && !idContainer.containsKey(task.getId())) {
            idContainer.put(task.getId(), task);
            flag = true;
        }
        if (task.getVersionId() != null && !annotationIDContainer.containsKey(task.getVersionId())) {
            annotationIDContainer.put(task.getVersionId(), task);
            flag = true;
        }
        if (!StringUtils.isEmpty(task.getAlias()) && !aliasContainer.containsKey(task.getAlias())) {
            aliasContainer.put(task.getAlias(), task);
            flag = true;
        }
        if (flag) {
            size.incrementAndGet();
        }
    }

    public boolean updateStatus(int finishedTimes, int currentRepeatTimes, long lastTriggeringTime, long nextTriggeringTime, long lastRunTime, boolean isLastSuccess, long taskId) {
        AutoJobTask task = getByIdDirect(taskId);
        if (task == null) {
            return false;
        }
        task
                .getTrigger()
                .setFinishedTimes(finishedTimes);
        task
                .getTrigger()
                .getCurrentRepeatTimes()
                .set(currentRepeatTimes);
        task
                .getTrigger()
                .setTriggeringTime(nextTriggeringTime);
        task
                .getTrigger()
                .setLastTriggeringTime(lastTriggeringTime);
        task
                .getTrigger()
                .setLastRunTime(lastRunTime);
        task
                .getTrigger()
                .setIsLastSuccess(isLastSuccess);
        return true;
    }

    public boolean updateRunningStatus(long taskID, AutoJobRunningStatus runningStatus) {
        if (runningStatus == null) {
            return false;
        }
        AutoJobTask task = getByIdDirect(taskID);
        if (task == null) {
            return false;
        }
        task.setRunningStatus(runningStatus);
        return true;
    }

    public boolean updateTriggeringTime(long taskId, long nextTriggeringTime) {
        AutoJobTask task = getByIdDirect(taskId);
        if (task == null) {
            return false;
        }
        task
                .getTrigger()
                .setTriggeringTime(nextTriggeringTime);
        return true;
    }

    public AutoJobTask getById(long taskId) {
        return AutoJobTask.deepCopyFrom(idContainer.get(taskId));
    }

    public AutoJobTask getByAlias(String alias) {
        return AutoJobTask.deepCopyFrom(aliasContainer.get(alias));
    }

    public AutoJobTask getByVersionId(long versionID) {
        return AutoJobTask.deepCopyFrom(annotationIDContainer.get(versionID));
    }

    public AutoJobTask getByIdDirect(long taskId) {
        return idContainer.get(taskId);

    }

    public AutoJobTask getByAliasDirect(String alias) {
        return aliasContainer.get(alias);
    }

    public AutoJobTask getByVersionIdDirect(long versionID) {
        return annotationIDContainer.get(versionID);
    }

    /**
     * 通过id移除任务
     *
     * @param taskId 要移除的任务ID
     * @return com.example.autojob.skeleton.framework.task.AutoJobTask
     * @author Huang Yongxiang
     * @date 2022/11/15 17:48
     */
    public AutoJobTask removeById(long taskId) {
        AutoJobTask removed = getById(taskId);
        if (removed == null) {
            return null;
        }
        if (removed.getAlias() != null) {
            aliasContainer.remove(removed.getAlias());
        }
        if (removed.getVersionId() != null) {
            annotationIDContainer.remove(removed.getVersionId());
        }
        size.addAndGet(-1);
        return removed;
    }

    /**
     * 通过别名移除任务
     *
     * @param alias 别名
     * @return com.example.autojob.skeleton.framework.task.AutoJobTask
     * @author Huang Yongxiang
     * @date 2022/11/15 17:54
     */
    public AutoJobTask removeByAlias(String alias) {
        AutoJobTask removed = getByAlias(alias);
        if (removed == null) {
            return null;
        }
        if (removed.getId() != null) {
            idContainer.remove(removed.getId());
        }
        if (removed.getVersionId() != null) {
            annotationIDContainer.remove(removed.getVersionId());
        }
        size.addAndGet(-1);
        return removed;
    }


    /**
     * 通过版本ID移除任务
     *
     * @param versionId 版本ID
     * @return com.example.autojob.skeleton.framework.task.AutoJobTask
     * @author Huang Yongxiang
     * @date 2022/11/15 17:54
     */
    public AutoJobTask removeByVersionId(long versionId) {
        AutoJobTask removed = getByVersionId(versionId);
        if (removed == null) {
            return null;
        }
        if (removed.getId() != null) {
            idContainer.remove(removed.getId());
        }
        if (removed.getAlias() != null) {
            aliasContainer.remove(removed.getAlias());
        }
        size.addAndGet(-1);
        return removed;
    }


    public List<AutoJobTask> getFutureRun(long futureTime, TimeUnit unit) {
        return idContainer
                .values()
                .stream()
                .filter(task -> task.getTrigger() != null)
                .filter(task -> !task
                        .getTrigger()
                        .getIsPause())
                .filter(task -> task.getIsFinished() == null || !task.getIsFinished())
                .filter(task -> task.getRunningStatus() == AutoJobRunningStatus.SCHEDULING || task.getRunningStatus() == AutoJobRunningStatus.RETRYING)
                .filter(task -> task
                        .getTrigger()
                        .getIsRunning() == null || !task
                        .getTrigger()
                        .getIsRunning())
                .filter(task -> task
                        .getTrigger()
                        .isNearTriggeringTime(unit.toMillis(futureTime)))
                .collect(Collectors.toList());
    }

    public List<AutoJobTask> list() {
        List<AutoJobTask> tasks = idContainer
                .values()
                .stream()
                .distinct()
                .collect(Collectors.toList());
        if (finishedTaskCache != null) {
            tasks.addAll(finishedTaskCache.values());
        }
        return ProtoStuffUtil.cloneObject(tasks);
    }

    public int size() {
        return size.get() + (finishedTaskCache == null ? 0 : finishedTaskCache.size());
    }

    public LocalCacheManager<String, AutoJobTask> getFinishedTaskCache() {
        return finishedTaskCache;
    }

    @Override
    public void startWork() {
        ScheduleTaskUtil
                .build(true, "memoryContainerDaemon")
                .EFixedRateTask(() -> {
                    for (Map.Entry<Long, AutoJobTask> entry : idContainer.entrySet()) {
                        AutoJobTask task = entry.getValue();
                        if (task.getIsFinished() != null && task.getIsFinished()) {
                            if (cleanStrategy == CleanStrategy.KEEP_FINISHED_TASK) {
                                finishedTaskCache.set(entry.getKey() + "", entry.getValue());
                            }
                            removeById(task.getId());
                        }
                    }
                }, 0, 1, TimeUnit.MILLISECONDS);
    }

    @Setter
    @Accessors(chain = true)
    public static class Builder {
        /**
         * 容器大小
         */
        private int limitSize = 100;

        /**
         * 清理策略
         */
        private CleanStrategy cleanStrategy = CleanStrategy.CLEAN_FINISHED_TASK;

        public MemoryTaskContainer build() {
            MemoryTaskContainer container = new MemoryTaskContainer();
            container.limitSize = limitSize;
            container.cleanStrategy = cleanStrategy;
            if (cleanStrategy == CleanStrategy.KEEP_FINISHED_TASK) {
                container.finishedTaskCache = LocalCacheManager
                        .builder()
                        .setExpiringTime(24, TimeUnit.HOURS)
                        .setEntriesExpiration(true)
                        .setMaxLength(limitSize)
                        .setPolicy(ExpirationPolicy.CREATED)
                        .build();
            }
            container.startWork();
            return container;
        }
    }
}
