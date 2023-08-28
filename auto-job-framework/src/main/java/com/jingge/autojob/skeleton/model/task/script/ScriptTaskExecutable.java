package com.jingge.autojob.skeleton.model.task.script;

import com.jingge.autojob.logging.model.producer.AutoJobLogHelper;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.model.task.TaskExecutable;
import com.jingge.autojob.util.convert.StringUtils;
import com.jingge.autojob.util.thread.ThreadHelper;
import com.jingge.autojob.util.thread.SyncHelper;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 脚本任务可执行对象
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/21 11:05
 */
@Slf4j
public class ScriptTaskExecutable implements TaskExecutable {
    private transient final ScriptTask scriptTask;
    private transient final AutoJobLogHelper logHelper = new AutoJobLogHelper();

    public ScriptTaskExecutable(ScriptTask scriptTask) {
        this.scriptTask = scriptTask;
    }

    @Override
    public AutoJobTask getAutoJobTask() {
        return scriptTask;
    }

    @Override
    public boolean isExecutable() {
        if (scriptTask.isCmd()) {
            return !StringUtils.isEmpty(scriptTask.getCmd());
        }
        File file = new File(scriptTask.getPath());
        return file.exists();
    }

    @Override
    public Object execute(Object... params) throws Exception {
        Process process = null;
        List<String> cmdList = new ArrayList<>();
        ProcessBuilder processBuilder = new ProcessBuilder().redirectErrorStream(true);
        if (scriptTask.isScriptFile() && (!scriptTask.isNeedWrite() || scriptTask.write())) {
            cmdList.add(scriptTask.getCmd());
            cmdList.add(scriptTask.getPath());
        } else if (scriptTask.isCmd()) {
            cmdList.addAll(Arrays.asList(scriptTask
                    .getCmd()
                    .split(" ")));
        }
        if (params != null) {
            cmdList.addAll(Arrays
                    .stream(params)
                    .filter(param -> param instanceof String)
                    .map(String::valueOf)
                    .collect(Collectors.toList()));
        }
        if (scriptTask.isEnableSharding()) {
            cmdList.add("-total \"" + scriptTask
                    .getShardingConfig()
                    .getTotal() + "\"");
            cmdList.add("-current \"" + scriptTask
                    .getShardingConfig()
                    .getCurrent() + "\"");
        }
        process = processBuilder
                .command(cmdList)
                .start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), AutoJobApplication
                .getInstance()
                .getConfigHolder()
                .getLogConfig()
                .getScriptTaskLogCharset()));
        do {
            try {
                SyncHelper.sleep(1, TimeUnit.MILLISECONDS);
                String log = reader.readLine();
                if (ThreadHelper.isInterrupt()) {
                    throw new InterruptedException();
                }
                if (!StringUtils.isEmpty(log)) {
                    logHelper.info(log);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                logHelper.warn("任务已被中断");
                process.destroyForcibly();
            }
        } while (process.isAlive() || reader.ready());
        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            logHelper.warn("任务已被中断");
            process.destroyForcibly();
        }
        return null;
    }

    @Override
    public Object[] getExecuteParams() {
        return scriptTask.getParams();
    }
}
