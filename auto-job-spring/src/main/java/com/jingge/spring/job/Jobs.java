package com.jingge.spring.job;

import com.jingge.autojob.logging.model.producer.AutoJobLogHelper;
import com.jingge.autojob.skeleton.annotation.AutoJob;
import com.jingge.autojob.skeleton.annotation.ScriptJob;
import com.jingge.autojob.skeleton.annotation.ShardingConfig;
import com.jingge.autojob.skeleton.enumerate.SaveStrategy;
import com.jingge.autojob.skeleton.enumerate.SchedulingStrategy;
import com.jingge.autojob.skeleton.enumerate.StartTime;
import com.jingge.autojob.skeleton.framework.mail.IMailClient;
import com.jingge.autojob.skeleton.framework.task.*;
import com.jingge.autojob.skeleton.model.builder.ScriptJobConfig;
import com.jingge.autojob.util.convert.DateUtils;
import com.jingge.autojob.util.thread.SyncHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 测试任务
 *
 * @Author Huang Yongxiang
 * @Date 2022/11/02 9:53
 * @Email 1158055613@qq.com
 */
@Slf4j
@Component
public class Jobs {

    //@ScriptJob(versionID = 2, value = "ping {}", taskType = AutoJobTask.TaskType.DB_TASK, saveStrategy = SaveStrategy.UPDATE, shardingConfig = @ShardingConfig(enable = true, enableShardingRetry = true, total = 12))
    public ScriptJobConfig scriptJob() {
        return ScriptJobConfig
                .builder()
                .addASimpleTrigger(System.currentTimeMillis() + 7000, -1, 7, TimeUnit.SECONDS)
                .addValue("www.baidu.com")
                .build();
    }

    //@AutoJob(id = 1, attributes = "{'hello'}", asType = AutoJobTask.TaskType.MEMORY_TASk, shardingConfig = @ShardingConfig(enable = true, total = 23))
    public void hello(String str) {
        //获取当前的上下文
        Random random = new Random();
        int flag = random.nextInt(100) + 1;
        AutoJobRunningContext context = AutoJobRunningContextHolder.currentTaskContext();
        AutoJobLogHelper logHelper = context.getLogHelper();
        logHelper.setSlf4jProxy(log);
        logHelper.info("参数：{}", str);
        logHelper.info("总分片：{}", context.getShardingTotal());
        logHelper.info("当前分片：{}", context.getCurrentSharding());
        logHelper.info("本次执行上下文参数：{}", flag);
        logHelper.info("当前调度记录ID：{}", context.getSchedulingRecordID());
        logHelper.info("当前任务ID：{}", context.getTaskId());
        logHelper.info("本次调度启动时间：{}", DateUtils.formatDateTime(new Date(context.getStartTime())));
        logHelper.info("当前任务类型：{}", context
                .getTaskType()
                .toString());
        //获取当前任务运行栈
        AutoJobRunningStack stack = context.getCurrentStack();
        logHelper.info("当前栈深：{}", stack.depth());
        //获取本次运行的栈帧
        RunningStackEntry current = stack.currentStackEntry();
        //添加本次运行的一下上下文参数，以便本次执行失败重试能直接恢复
        current.addParam("runningPos", flag);
        //也可以获取上次执行的上下文参数
        Optional<RunningStackEntry> last = stack.lastStackEntry();
        last.ifPresent(stackEntry -> logHelper.info("上次执行上下文参数{}", stackEntry.getParam("runningPos")));
        last.ifPresent(stackEntry -> logHelper.info("上次是否执行成功：{}", stackEntry.isSuccess()));
        last.ifPresent(stackEntry -> logHelper.info("上次执行参数：{}", stackEntry.getArgs()));
        //获取当前任务具有的邮件客户端
        IMailClient mailClient = context.getMailClient();

        if (flag % 3 == 0 && flag > 53) {
            throw new RuntimeException("模拟异常发生");
        }
        SyncHelper.sleepQuietly(5, TimeUnit.SECONDS);
    }
}
