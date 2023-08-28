package com.jingge.spring;

import com.jingge.autojob.skeleton.annotation.AutoJobRPCServiceScan;
import com.jingge.autojob.skeleton.annotation.AutoJobScan;
import com.jingge.autojob.skeleton.framework.boot.AutoJobBootstrap;
import com.jingge.autojob.skeleton.framework.config.AutoJobRetryConfig;
import com.jingge.autojob.skeleton.framework.config.RetryStrategy;
import com.jingge.autojob.skeleton.model.builder.AutoJobMethodTaskBuilder;
import com.jingge.autojob.skeleton.model.task.functional.FunctionFuture;
import com.jingge.autojob.skeleton.model.task.functional.FunctionTask;
import com.jingge.autojob.util.thread.SyncHelper;
import com.jingge.spring.job.Jobs;

import java.util.concurrent.TimeUnit;

/**
 * 测试Server
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-04-12 16:46
 * @email 1158055613@qq.com
 */
@AutoJobScan("com.jingge.spring")
public class Server {
    public static void main(String[] args) {
        new AutoJobBootstrap(Server.class, args)
                .withAutoScanProcessor()
                .build()
                .run();
        /*=================测试=================>*/
        FunctionTask functionTask = new FunctionTask(context -> {
            context
                    .getLogHelper()
                    .info("测试一下");
            SyncHelper.sleepQuietly(5, TimeUnit.SECONDS);
        });
        FunctionFuture future = functionTask.submit();
        System.out.println("执行完啦：" + future.get());
        functionTask
                .getLogs(3, TimeUnit.SECONDS)
                .forEach(System.out::println);
        System.out.println("日志输出完成");
        /*=======================Finished======================<*/
    }
}
