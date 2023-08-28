package com.jingge.autojob.skeleton.lifecycle.manager;

import com.jingge.autojob.skeleton.lifecycle.event.TaskEvent;
import com.jingge.autojob.skeleton.lifecycle.listener.TaskListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 统一的事件管理类
 *
 * @Auther Huang Yongxiang
 * @Date 2021/12/15 15:26
 */
public class TaskEventManager {
    private final Map<String, List<TaskListener<TaskEvent>>> taskListenerListMap;
    private static final Logger logger = LoggerFactory.getLogger(TaskEventManager.class);
    private final ThreadPoolExecutor eventHandlerThreadPool;

    private TaskEventManager() {
        taskListenerListMap = new ConcurrentHashMap<>();
        eventHandlerThreadPool = new ThreadPoolExecutor(3, 8, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(200));
    }


    public static TaskEventManager getInstance() {
        return InstanceHolder.MANAGER;
    }

    /**
     * 添加监听器到事件管理器
     *
     * @param listener    监听器
     * @param genericType 该监听器要监听的事件的class对象
     * @return void
     * @author Huang Yongxiang
     * @date 2021/12/16 16:14
     */
    @SuppressWarnings("unchecked")
    public void addTaskEventListener(TaskListener<? extends TaskEvent> listener, Class<? extends TaskEvent> genericType) {
        if (genericType == null) {
            return;
        }
        String key = genericType.getName();
        List<TaskListener<TaskEvent>> listeners = taskListenerListMap.get(key);
        if (listeners == null) {
            listeners = new LinkedList<>();
        }
        listeners.add((TaskListener<TaskEvent>) listener);
        taskListenerListMap.put(key, listeners);
    }

    /**
     * 同步的发布事件，事件的后续操作将同步执行
     *
     * @param event       事件对象
     * @param eventType   事件类型
     * @param allowBubble 是否允许事件冒泡
     * @return void
     * @author Huang Yongxiang
     * @date 2022/5/24 12:21
     */
    @SuppressWarnings("unchecked")
    public void publishTaskEventSync(TaskEvent event, Class<? extends TaskEvent> eventType, boolean allowBubble) {
        try {
            if (event == null || taskListenerListMap.size() == 0) {
                return;
            }
            String key = eventType.getName();
            List<TaskListener<TaskEvent>> listeners = taskListenerListMap.get(key);
            //执行事件
            if (listeners != null) {
                for (TaskListener<TaskEvent> listener : listeners) {
                    if (listener != null) {
                        listener.onTaskEvent(event);
                    }
                }
            }

            if (allowBubble) {
                Class<?> superClass = eventType.getSuperclass();
                if (superClass != null && superClass.newInstance() instanceof TaskEvent) {
                    publishTaskEventSync(event, (Class<? extends TaskEvent>) superClass, true);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("执行监听器时发生异常：{}", e.getMessage());
        }
    }

    /**
     * 异步的发布事件，事件的后续操作将异步进行，主线程将不会等待事件的处理
     *
     * @param event       事件
     * @param eventType   事件对象
     * @param allowBubble 是否允许事件冒泡
     * @return void
     * @author Huang Yongxiang
     * @date 2022/5/24 12:22
     */
    @SuppressWarnings("unchecked")
    public void publishTaskEvent(TaskEvent event, Class<? extends TaskEvent> eventType, boolean allowBubble) {
        try {
            if (event == null || taskListenerListMap.size() == 0) {
                return;
            }
            String key = eventType.getName();
            List<TaskListener<TaskEvent>> listeners = taskListenerListMap.get(key);
            if (listeners != null) {
                for (TaskListener<TaskEvent> listener : listeners) {
                    if (listener != null) {
                        eventHandlerThreadPool.submit(() -> {
                            try {
                                listener.onTaskEvent(event);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }
            }
            if (allowBubble) {
                Class<?> superClass = eventType.getSuperclass();
                if (superClass != null && TaskEvent.class.isAssignableFrom(superClass)) {
                    publishTaskEvent(event, (Class<? extends TaskEvent>) superClass, true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("执行监听器时发生异常：{}，事件类型：{}", e.getMessage(), eventType.getName());
        }
    }

    private static class InstanceHolder {
        private static final TaskEventManager MANAGER = new TaskEventManager();
    }
}
