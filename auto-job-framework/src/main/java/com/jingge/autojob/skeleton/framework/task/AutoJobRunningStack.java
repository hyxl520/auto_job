package com.jingge.autojob.skeleton.framework.task;

import com.jingge.autojob.skeleton.lang.Deserializer;
import com.jingge.autojob.skeleton.lang.Serializer;
import com.jingge.autojob.util.convert.ProtoStuffUtil;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Optional;

/**
 * 任务运行堆栈，保留每次运行的上下文便于重试恢复，该堆栈信息可以序列化和反序列化
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-03-21 14:23
 * @email 1158055613@qq.com
 */
public class AutoJobRunningStack implements Serializable, Serializer<byte[], AutoJobRunningStack> {
    /**
     * 运行堆栈
     */
    private final ArrayDeque<RunningStackEntry> stack = new ArrayDeque<>(8);
    /**
     * 栈的最大深度，超过将会移除最先的
     */
    private int maximumDeep = 32;

    /**
     * 任务ID
     */
    private final long taskID;

    private static final RunStackDeserializer DESERIALIZER = new RunStackDeserializer();

    public AutoJobRunningStack(long taskID, int maximumDeep) {
        this.taskID = taskID;
        this.maximumDeep = maximumDeep >= 0 ? maximumDeep : this.maximumDeep;
    }

    public AutoJobRunningStack(long taskID) {
        this.taskID = taskID;
    }

    public Optional<RunningStackEntry> lastStackEntry() {
        if (depth() < 2) {
            return Optional.empty();
        }
        Iterator<RunningStackEntry> iterator = stack.descendingIterator();
        iterator.next();
        return Optional.of(iterator.next());
    }

    public RunningStackEntry currentStackEntry() {
        return stack.peekLast();
    }

    public RunningStackEntry findBySchedulingRecordID(long schedulingID) {
        return stack
                .stream()
                .filter(item -> item.schedulingRecordID == schedulingID)
                .findAny()
                .orElse(null);
    }

    public int depth() {
        return stack.size();
    }

    RunningStackEntry add(RunningStackEntry stackEntry) {
        //如果栈深已经达到最大深度，移除最底部的栈
        RunningStackEntry remove = null;
        if (stack.size() >= maximumDeep) {
            remove = stack.pollFirst();
        }
        stack.offerLast(stackEntry);
        return remove;
    }

    void clear() {
        stack.clear();
    }

    public static AutoJobRunningStack deserialize(byte[] content) {
        return DESERIALIZER.deserialize(content);
    }

    @Override
    public byte[] serialize(AutoJobRunningStack source) {
        return ProtoStuffUtil.serialize(source);
    }

    @Override
    public byte[] serialize() {
        return ProtoStuffUtil.serialize(this);
    }

    private static class RunStackDeserializer implements Deserializer<AutoJobRunningStack, byte[]> {
        @Override
        public AutoJobRunningStack deserialize(byte[] source) {
            return ProtoStuffUtil.deserialize(source, AutoJobRunningStack.class);
        }
    }
}
