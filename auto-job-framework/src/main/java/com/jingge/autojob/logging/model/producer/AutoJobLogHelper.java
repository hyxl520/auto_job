package com.jingge.autojob.logging.model.producer;

import com.jingge.autojob.logging.domain.AutoJobLog;
import com.jingge.autojob.logging.model.AutoJobLogContainer;
import com.jingge.autojob.skeleton.framework.mq.MessageProducer;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.framework.task.AutoJobContext;
import com.jingge.autojob.util.convert.DateUtils;
import com.jingge.autojob.util.convert.DefaultValueUtil;
import com.jingge.autojob.util.convert.StringUtils;
import com.jingge.autojob.util.id.IdGenerator;
import com.jingge.autojob.util.message.MessageManager;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 日志辅助类，该类可直接实现日志打印，同时该类打印的日志会被直接加入任务日志
 * 该类支持子方法、子线程日志捕获
 * <p>为了保证项目统一使用slf4j logger，该类允许设置对slf4j logger进行代理，为了能真实记录原log输出位置，将会在原日志上增加
 * $Actual-Location - [fileName:lineNum]$</p>
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/05 14:33
 */
@Slf4j
public class AutoJobLogHelper implements IAutoJobLogProducer<AutoJobLog> {

    private volatile Logger slf4jLogger;

    private final MessageProducer<AutoJobLog> producer;

    private AutoJobTask heldTask;


    public AutoJobLogHelper() {
        this(null);
    }

    public AutoJobLogHelper(Logger slf4jLogger) {
        this.slf4jLogger = slf4jLogger;
        this.producer = new MessageProducer<>(AutoJobLogContainer
                .getInstance()
                .getMessageQueueContext(AutoJobLog.class));
        if (producer.getMessageQueueContext() == null) {
            throw new IllegalStateException("AutoJob上下文尚未初始化");
        }
    }

    public AutoJobLogHelper(Logger slf4jLogger, AutoJobTask heldTask) {
        if (heldTask == null) {
            throw new NullPointerException();
        }
        this.slf4jLogger = slf4jLogger;
        this.heldTask = heldTask;
        this.producer = new MessageProducer<>(AutoJobLogContainer
                .getInstance()
                .getMessageQueueContext(AutoJobLog.class));
    }

    /**
     * 获取一个日志辅助类，优先获取当前线程绑定的实例，如果不存在再创建一个新的实例，这是强烈推荐的仔任务方法内使用日志辅助类获取实例的方式
     *
     * @return com.jingge.autojob.logging.model.producer.AutoJobLogHelper
     * @author Huang Yongxiang
     * @date 2022/8/29 15:46
     */
    public static AutoJobLogHelper getInstance() {
        AutoJobTask task = AutoJobContext
                .getConcurrentThreadTask()
                .get();
        if (task == null || task.getLogHelper() == null) {
            return new AutoJobLogHelper();
        }
        return task.getLogHelper();
    }


    private static String now() {
        return DateUtils.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss,SSS");
    }

    private static String getThreadName() {
        return Thread
                .currentThread()
                .getName();
    }


    private static String getLogLocation() {
        StackTraceElement stackTraceElement = Thread
                .currentThread()
                .getStackTrace()[4];
        return String.format("%s - [%s:%d]", stackTraceElement.getClassName(), stackTraceElement.getFileName(), stackTraceElement.getLineNumber());
    }


    private static String getFormatMessage(String appendLogPattern, Object... appendLogArguments) {
        return MessageManager.formatMsgLikeSlf4j(appendLogPattern, appendLogArguments);
    }

    /**
     * 对slf4j的logger进行代理，日志的输出将通过slf4j logger输出，不指定代理将使用默认输出且只能输出到控制台
     *
     * @param logger slf4j logger
     * @return void
     * @author Huang Yongxiang
     * @date 2022/8/19 17:07
     */
    public AutoJobLogHelper setSlf4jProxy(Logger logger) {
        slf4jLogger = logger;
        return this;
    }

    public void debug(String appendLogPattern, Object... appendLogArguments) {
        String message = getLevelMessage("DEBUG", appendLogPattern, appendLogArguments);
        String id = getBindingTaskId();
        if (!StringUtils.isEmpty(id)) {
            produce(producer, id, getAutoJobLog(message, "DEBUG"));
        }
        if (slf4jLogger != null) {
            slf4jLogger.debug(getActualLocation() + appendLogPattern, appendLogArguments);
        } else {
            System.out.println(message);
        }
    }

    public void info(String appendLogPattern, Object... appendLogArguments) {
        String message = getLevelMessage("INFO", appendLogPattern, appendLogArguments);
        String id = getBindingTaskId();
        if (!StringUtils.isEmpty(id)) {
            produce(producer, id, getAutoJobLog(message, "INFO"));
        }
        if (slf4jLogger != null) {
            slf4jLogger.info(getActualLocation() + appendLogPattern, appendLogArguments);
        } else {
            System.out.println(message);
        }
    }

    public void warn(String appendLogPattern, Object... appendLogArguments) {
        String message = getLevelMessage("WARN", appendLogPattern, appendLogArguments);
        String id = getBindingTaskId();
        if (!StringUtils.isEmpty(id)) {
            produce(producer, id, getAutoJobLog(message, "WARN"));
        }
        if (slf4jLogger != null) {
            slf4jLogger.warn(getActualLocation() + appendLogPattern, appendLogArguments);
        } else {
            System.out.println(message);
        }
    }

    public void error(String appendLogPattern, Object... appendLogArguments) {
        String message = getLevelMessage("ERROR", appendLogPattern, appendLogArguments);
        String id = getBindingTaskId();
        if (!StringUtils.isEmpty(id)) {
            produce(producer, id, getAutoJobLog(message, "ERROR"));
        }
        if (slf4jLogger != null) {
            slf4jLogger.error(getActualLocation() + appendLogPattern, appendLogArguments);
        } else {
            System.out.println(message);
        }
    }

    private String getActualLocation() {
        StackTraceElement stackTraceElement = Thread
                .currentThread()
                .getStackTrace()[3];
        return String.format("$Actual-Location - [%s:%s]$ - ", stackTraceElement.getFileName(), stackTraceElement.getLineNumber());
    }

    private String getBindingTaskId() {
        if (heldTask != null) {
            return heldTask
                    .getTrigger()
                    .getSchedulingRecordID() + "";
        }
        return AutoJobContext
                .getCurrentScheduleID()
                .get() + "";
    }

    private AutoJobTask getBindingTask() {
        return heldTask == null ? AutoJobContext
                .getConcurrentThreadTask()
                .get() : heldTask;
    }

    private static String getLevelMessage(String level, String appendLogPattern, Object... appendLogArguments) {
        return String.format("%s - %s - [%s] %s - %s", now(), level, getThreadName(), getLogLocation(), getFormatMessage(appendLogPattern, appendLogArguments));
    }

    private AutoJobLog getAutoJobLog(String message, String level) {
        String bind = getBindingTaskId();
        AutoJobLog autoJobLog = new AutoJobLog();
        autoJobLog.setId(IdGenerator.getNextIdAsLong());
        autoJobLog.setInputTime(DateUtils.getTime());
        autoJobLog.setLevel(level);
        autoJobLog.setTaskId(StringUtils.isEmpty(bind) || "null".equals(bind) ? -1 : Long.parseLong(bind));
        autoJobLog.setMessage(message);
        return autoJobLog;
    }


    @Override
    public void produce(MessageProducer<AutoJobLog> producer, String topic, AutoJobLog autoJobLog) {

        if (producer != null && autoJobLog != null) {
            if (!producer.hasTopic(topic)) {
                producer.registerMessageQueue(topic);
            }
            AutoJobTask concurrentTask = getBindingTask();
            if (concurrentTask != null) {
                producer.publishMessageBlock(autoJobLog, topic, concurrentTask
                        .getTrigger()
                        .getMaximumExecutionTime() * 1000 + 10000, TimeUnit.MILLISECONDS);
            } else {
                producer.publishMessageBlock(autoJobLog, topic);
            }
        }
    }

}
