package com.jingge.autojob.skeleton.model.task.script;

import com.jingge.autojob.skeleton.enumerate.ScriptType;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.model.builder.AutoJobScriptTaskBuilder;
import com.jingge.autojob.skeleton.model.task.TaskExecutable;
import com.jingge.autojob.util.convert.DateUtils;
import com.jingge.autojob.util.convert.DefaultValueUtil;
import com.jingge.autojob.util.convert.StringUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 脚本型任务，脚本默认维护在项目同级目录下的auto-job/data/script目录下，为了你能创建一个能直接参与调度的ScriptTask，请使用{@link AutoJobScriptTaskBuilder}对象构建完整的脚本型任务对象
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/21 9:46
 */
@Getter
@Setter
public class ScriptTask extends AutoJobTask {
    private static final String BASE_SCRIPT_PATH = "auto-job"
            .concat(File.separator)
            .concat("data")
            .concat(File.separator)
            .concat("script")
            .concat(File.separator);
    /**
     * 脚本内容
     */
    private String scriptContent;

    /**
     * 脚本路径
     */
    private String scriptPath;

    /**
     * 脚本文件名，如果不带后缀，请勿在命名中包含.字符
     */
    private String scriptFilename;

    /**
     * 脚本后缀
     */
    private String scriptFileSuffix;

    /**
     * 启动命令
     */
    private String cmd;

    /**
     * 是否是脚本文件
     */
    private boolean isScriptFile;

    /**
     * 是否需要写入
     */
    private boolean isNeedWrite;

    /**
     * 是否是命令
     */
    private boolean isCmd;

    /**
     * 是否已写入
     */
    private volatile boolean isWrote;

    /**
     * 可执行类
     */
    private transient TaskExecutable executable;

    public boolean write() {
        if (!isScriptFile) {
            throw new UnsupportedOperationException("该任务非脚本文件");
        }
        if (!isNeedWrite) {
            throw new UnsupportedOperationException("该任务脚本内容不支持写入");
        }
        if (isWrote) {
            return true;
        }
        String fatherPath = StringUtils.isEmpty(scriptPath) || "/".equals(scriptPath) ? BASE_SCRIPT_PATH : scriptPath;
        File file = new File(fatherPath);
        if (!file.exists() && !file.mkdirs()) {
            throw new RuntimeException("脚本文件夹无法被创建");
        }
        if (!StringUtils.isEmpty(scriptFilename)) {
            String path = getPath();
            File scriptFile = new File(path);
            FileOutputStream outputStream = null;
            try {
                if (!scriptFile.exists() && !scriptFile.createNewFile()) {
                    throw new RuntimeException("脚本文件无法被创建");
                }
                outputStream = new FileOutputStream(scriptFile);
                IOUtils.write(scriptContent, outputStream, StandardCharsets.UTF_8);
                isWrote = true;
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (outputStream != null) {
                    IOUtils.closeQuietly(outputStream);
                }
            }
        }
        return false;
    }

    public ScriptTask setIsCmd(boolean cmd) {
        isCmd = cmd;
        return this;
    }

    /**
     * 使用枚举的脚本类型创建一个脚本任务
     *
     * @param scriptType 脚本类型
     * @param content    脚本内容
     * @author Huang Yongxiang
     * @date 2022/10/29 15:51
     */
    public ScriptTask(ScriptType scriptType, String content) {
        scriptContent = content;
        cmd = scriptType.getCmd();
        scriptFileSuffix = scriptType.getSuffix();
        scriptFilename = DateUtils
                .getDateTime()
                .replace(" ", "_")
                .replace(":", "_")
                .concat("_")
                .concat(scriptType.getName())
                .concat(scriptType.getSuffix());
        isScriptFile = true;
        isCmd = false;
        isNeedWrite = true;
    }

    public boolean delete() {
        if (isScriptFile) {
            File file = new File(getPath());
            return file.exists() && file.delete();
        }
        return false;
    }


    /**
     * 使用给定路径的脚本文件创建一个脚本型任务
     *
     * @param scriptType     脚本类型
     * @param scriptPath     脚本路径
     * @param scriptFilename 脚本文件名
     * @author Huang Yongxiang
     * @date 2022/10/29 15:52
     */
    public ScriptTask(ScriptType scriptType, String scriptPath, String scriptFilename) {
        if (scriptType == null) {
            throw new NullPointerException();
        }
        cmd = scriptType.getCmd();
        scriptFileSuffix = scriptType.getSuffix();
        this.scriptFilename = scriptFilename;
        this.scriptPath = StringUtils.isEmpty(scriptPath) || "/".equals(scriptPath) ? BASE_SCRIPT_PATH : scriptPath;
        isScriptFile = true;
        isNeedWrite = false;
        isCmd = false;
        isWrote = false;
    }

    /**
     * 创建一个自定义类型的可执行脚本任务
     *
     * @param cmd              脚本的启动命令，如python、mvn、java、npm等
     * @param scriptPath       脚本路径
     * @param scriptFilename   脚本文件名：“test”
     * @param scriptFileSuffix 脚本后缀：“.py”
     * @author Huang Yongxiang
     * @date 2022/10/29 16:08
     */
    public ScriptTask(String cmd, String scriptPath, String scriptFilename, String scriptFileSuffix) {
        this.cmd = cmd;
        this.scriptPath = StringUtils.isEmpty(scriptPath) || "/".equals(scriptPath) ? BASE_SCRIPT_PATH : scriptPath;
        this.scriptFilename = scriptFilename;
        if (scriptFileSuffix.charAt(0) != '.') {
            this.scriptFileSuffix = "." + scriptFileSuffix;
        } else {
            this.scriptFileSuffix = scriptFileSuffix;
        }
        isScriptFile = true;
        isNeedWrite = false;
        isWrote = true;
    }

    /**
     * 创建一个简单命令行任务
     *
     * @param cmd 执行的命令
     * @author Huang Yongxiang
     * @date 2022/10/29 16:12
     */
    public ScriptTask(String cmd) {
        isScriptFile = false;
        isCmd = true;
        this.cmd = cmd;
        isNeedWrite = false;
        isWrote = true;
    }

    public ScriptTask() {
    }

    public String getPath() {
        String fatherPath = StringUtils.isEmpty(scriptPath) || "/".equals(scriptPath) ? BASE_SCRIPT_PATH : scriptPath;
        int index = scriptFilename.lastIndexOf(".");
        String path;
        if (index != -1) {
            path = fatherPath
                    .concat(File.separator)
                    .concat(scriptFilename);
        } else {
            path = fatherPath
                    .concat(File.separator)
                    .concat(this.scriptFilename)
                    .concat(".")
                    .concat(DefaultValueUtil
                            .defaultStringWhenEmpty(scriptFileSuffix, "")
                            .replace(".", ""));
        }
        return path;
    }

    @Override
    public TaskExecutable getExecutable() {
        if (executable != null) {
            return executable;
        }
        this.executable = new ScriptTaskExecutable(this);
        return this.executable;
    }

    @Override
    public String getReference() {
        if (isScriptFile) {
            return getPath();
        }
        return "cmd:" + cmd;
    }

    public boolean isCmd() {
        return isCmd;
    }


}
