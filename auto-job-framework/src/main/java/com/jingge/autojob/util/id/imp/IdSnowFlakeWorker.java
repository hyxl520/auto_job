package com.jingge.autojob.util.id.imp;

import com.jingge.autojob.util.id.IdWorker;
import com.jingge.autojob.util.id.SystemClock;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ID雪花生成器
 *
 * @Auther Huang Yongxiang
 * @Date 2022/01/24 10:05
 */
@Slf4j
public class IdSnowFlakeWorker implements IdWorker {
    /**
     * 时间初值
     */
    long twepoch = 1585644268888L;
    /**
     * 机器ID
     */
    private long workerId;

    /**
     * 一毫秒内的ID序号，该算法目前认为是12位的最大值
     */
    private static AtomicLong sequence = new AtomicLong(0);
    /**
     * 机器ID所占位数
     */
    private final long workerIdBits = 10L;
    /**
     * 序列号所占位数
     */
    private final long sequenceBits = 12L;
    /**
     * workerIdBits位的数最大值
     */
    private final long maxWorkerId = ~(-1L << workerIdBits);
    /**
     * 序列号最大值
     */
    private final long sequenceMask = ~(-1L << sequenceBits);
    /**
     * 机器ID移位数
     */
    private final long workerIdShift = sequenceBits;
    /**
     * 时间戳移位数
     */
    private final long timestampLeftShift = sequenceBits + workerIdBits;

    /**
     * 记录上次产生ID的时间戳
     */
    private long lastTimestamp = -1L;


    @Override
    public synchronized Long nextId() {
        // 这儿就是获取当前时间戳，单位是毫秒
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {
            log.error("时间被回调，小于上次生成时间戳：{}，将会出现冲突异常.", lastTimestamp);
            throw new RuntimeException(String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }
        //如果机器ID为0或超过了最大值，则以随机数替代
        if (workerId == 0 || workerId > maxWorkerId) {
            workerId = new Random().nextInt((int) maxWorkerId + 1);
        }

        //如果多个请求位于一个毫秒内则对序列号自增
        if (lastTimestamp == timestamp) {
            //序列号自增，达到4096后与4095相与归零
            sequence.set(sequence.incrementAndGet() & sequenceMask);
            if (sequence.get() == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }

        } else {
            sequence.set(0);
        }
        // 这儿记录一下最近一次生成id的时间戳，单位是毫秒
        lastTimestamp = timestamp;
        return ((timestamp - twepoch) << timestampLeftShift) | (workerId << workerIdShift) | sequence.get();
    }

    @Override
    public long getAsLong() {
        long nextId;
        try {
            nextId = nextId();
            return nextId;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public String getAsString() {
        String nextId;
        try {
            nextId = String.valueOf(nextId());
            return "null".equalsIgnoreCase(nextId) ? null : nextId;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public int getAsInteger() {
        int nextId;
        try {
            long id = nextId();
            nextId = (int) (id == 0 ? -1 : id);
            return nextId;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public BigDecimal getAsBigDecimal() {
        BigDecimal nextId = null;
        try {
            long id = nextId();
            nextId = new BigDecimal(id == 0 ? -1 : id);
            return nextId;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new BigDecimal("-1");
    }

    /**
     * 某一毫秒的时间，产生的id数 超过4095，系统会进入等待，直到下一毫秒，系统继续产生ID
     *
     * @param lastTimestamp 时间戳
     * @return long
     * @author Huang Yongxiang
     * @date 2022/1/24 10:21
     */
    private long tilNextMillis(long lastTimestamp) {

        long timestamp = timeGen();

        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 获取当前时间戳
     *
     * @return long
     * @author Huang Yongxiang
     * @date 2022/1/24 10:22
     */
    private long timeGen() {
        return SystemClock.now();
    }

    /**
     * main 测试类
     */
    public static void main(String[] args) {
        IdSnowFlakeWorker idGenerate = new IdSnowFlakeWorker();
        int bits = 12;
        long value1 = ~(-1 << bits);
        long value2 = ~(-1 << bits);
        System.out.println(idGenerate.nextId());
        System.out.println(idGenerate.nextId());
        System.out.println(idGenerate.nextId());
        System.out.println(idGenerate.nextId());
        System.out.println(idGenerate.nextId());
        System.out.println(idGenerate.nextId());

    }


}
