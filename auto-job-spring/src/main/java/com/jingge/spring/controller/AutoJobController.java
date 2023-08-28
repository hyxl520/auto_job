package com.jingge.spring.controller;

import com.jingge.autojob.api.task.AutoJobAPI;
import com.jingge.autojob.api.task.params.MethodTaskEditParams;
import com.jingge.autojob.api.task.params.ScriptTaskEditParams;
import com.jingge.autojob.api.task.params.TriggerEditParams;
import com.jingge.autojob.skeleton.enumerate.ScriptType;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.framework.task.AutoJobTrigger;
import com.jingge.autojob.skeleton.model.builder.AutoJobMethodTaskBuilder;
import com.jingge.autojob.skeleton.model.builder.AutoJobScriptTaskBuilder;
import com.jingge.autojob.skeleton.model.builder.AutoJobTriggerFactory;
import com.jingge.autojob.skeleton.model.task.method.MethodTask;
import com.jingge.autojob.skeleton.model.task.script.ScriptTask;
import com.jingge.autojob.util.bean.ObjectUtil;
import com.jingge.autojob.util.convert.DefaultValueUtil;
import com.jingge.autojob.util.convert.StringUtils;
import com.jingge.autojob.util.id.IdGenerator;
import com.jingge.autojob.util.message.MessageManager;
import com.jingge.spring.query.MethodTaskQuery;
import com.jingge.spring.query.ScriptTaskQuery;
import com.jingge.spring.query.TriggerQuery;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 测试使用Rest接口操作任务
 *
 * @Author Huang Yongxiang
 * @Date 2022/11/02 16:48
 * @Email 1158055613@qq.com
 */
@RestController("autoJobSpringController")
@RequestMapping("/auto_job_spring")
public class AutoJobController {

    @PostMapping(value = "/run_method_task_now", produces = "application/json;charset=UTF-8")
    public String runMethodTaskNow(@RequestBody(required = false) MethodTaskQuery query) {
        if (query == null) {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "参数为空");
        }
        Class<?> methodClass = ObjectUtil.classPath2Class(query.getTaskClass());
        if (methodClass == null) {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, query.getTaskClass() + "不存在");
        }
        MethodTask task = new AutoJobMethodTaskBuilder(methodClass, query.getTaskName())
                .setTaskAlias(query.getTaskName())
                .setTaskId(IdGenerator.getNextIdAsLong())
                .setParams(query.getAttributes())
                .build();
        if (!task.isExecutable()) {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "运行失败");
        }
        boolean flag = AutoJobApplication
                .getInstance()
                .getMemoryTaskAPI()
                .runTaskNow(task);
        if (flag) {
            return MessageManager.getJsonMessage(MessageManager.Code.OK, "运行成功", task);
        }
        return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "运行失败");
    }

    @PostMapping(value = "/run_script_task_now", produces = "application/json;charset=UTF-8")
    public String runScriptTaskNow(@RequestBody(required = false) ScriptTaskQuery query) {
        if (query == null) {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "请求参数为空");
        }
        if (query.getType() == null) {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "请指定脚本类型");
        }
        if (StringUtils.isEmpty(query.getRunCmd()) && StringUtils.isEmpty(query.getContent()) && StringUtils.isEmpty(query.getPath())) {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "所要执行的脚本不存在");
        }
        AutoJobScriptTaskBuilder builder = new AutoJobScriptTaskBuilder()
                .setTaskId(IdGenerator.getNextIdAsLong())
                .setTaskType(AutoJobTask.TaskType.MEMORY_TASk);
        ScriptTask task = null;
        if (query.getType() == 0) {
            task = builder.createNewWithCmd(query.getContent());
        } else if (query.getType() == 1) {
            ScriptType type = ScriptType.findByName(query.getFileType());
            if (type == null) {
                return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "未知的脚本文件类型：" + query.getFileType());
            }
            task = builder.createNewWithContent(type, query.getContent());
        } else if (query.getType() == 2) {
            if (!StringUtils.isEmpty(query.getRunCmd())) {
                task = builder.createNew(query.getRunCmd(), query.getPath(), query.getScriptFileName(), query.getSuffix());
            } else {
                ScriptType type = ScriptType.findByName(query.getFileType());
                if (type == null) {
                    return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "未知的脚本文件类型：" + query.getFileType());
                }
                task = builder.createNewWithExistScriptFile(type, query.getPath(), query.getScriptFileName());
            }
        }
        if (task == null || !task.isExecutable()) {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "创建脚本任务失败，请检查参数");
        }
        boolean flag = AutoJobApplication
                .getInstance()
                .getMemoryTaskAPI()
                .runTaskNow(task);
        if (flag) {
            return MessageManager.getJsonMessage(MessageManager.Code.OK, "运行成功", task);
        }
        return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "运行失败");
    }

    @PostMapping(value = "/binding_trigger", produces = "application/json;charset=UTF-8")
    public String bindingTrigger(@RequestBody(required = false) TriggerQuery query, @RequestParam(value = "TASK_ID", required = false) Long taskId) {
        if (ObjectUtil.isNull(query) || taskId == null) {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "请求参数为空");
        }
        if (query.getType() == null) {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "请指定触发器类型");
        }
        AutoJobAPI api = AutoJobApplication
                .getInstance()
                .getMatchedAPI(taskId);
        if (api == null) {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "没有匹配的任务：" + taskId);
        }
        AutoJobTrigger trigger = null;
        if (query.getType() == 0) {
            if (StringUtils.isEmpty(query.getCronExpression())) {
                return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "cron like表达式为空");
            }
            trigger = AutoJobTriggerFactory.newCronExpressionTrigger(query.getCronExpression(), query.getRepeatTimes());
            trigger.refresh();
        } else if (query.getType() == 1) {
            if (query.getRepeatTimes() == null || query.getCycle() == null) {
                return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "重复次数或周期不存在");
            }
            trigger = AutoJobTriggerFactory.newSimpleTrigger(DefaultValueUtil.defaultValue(query.getTriggeringTime(), System.currentTimeMillis() + 5000), query.getRepeatTimes(), query.getCycle(), TimeUnit.SECONDS);
        } else if (query.getType() == 2) {
            trigger = AutoJobTriggerFactory.newChildTrigger();
        } else if (query.getType() == 3) {
            if (query.getDelay() == null) {
                return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "延迟时间不存在");
            }
            trigger = AutoJobTriggerFactory.newDelayTrigger(query.getDelay(), TimeUnit.SECONDS);
        }
        if (trigger == null) {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "错误的类型：" + query.getType());
        }
        if (query.getMaximumExecutionTime() != null && query.getMaximumExecutionTime() > 0) {
            trigger.setMaximumExecutionTime(query.getMaximumExecutionTime());
        }
        if (!StringUtils.isEmpty(query.getChildTasksId())) {
            String[] ids = query
                    .getChildTasksId()
                    .trim()
                    .split(",");
            trigger.setChildTask(Arrays
                    .stream(ids)
                    .map(Long::parseLong)
                    .collect(Collectors.toList()));
        }
        trigger.setTaskId(taskId);
        if (api.bindingTrigger(taskId, trigger)) {
            return MessageManager.getJsonMessage(MessageManager.Code.OK, "绑定成功", trigger);
        }
        return MessageManager.getJsonMessage(MessageManager.Code.ERROR, "绑定失败");
    }


    @GetMapping(value = "/page_method_task", produces = "application/json;charset=UTF-8")
    public String pageMethodTask(@RequestParam(value = "pageNum", required = false) Integer pageNum, @RequestParam(value = "pageSize", required = false) Integer pageSize, @RequestParam(value = "type", required = false) Integer type) {
        if (pageNum == null || pageSize == null) {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "分页参数为空");
        }
        AutoJobAPI api;
        if (type == null || type == 1) {
            api = AutoJobApplication
                    .getInstance()
                    .getMemoryTaskAPI();
        } else if (type == 2) {
            api = AutoJobApplication
                    .getInstance()
                    .getDbTaskAPI();
        } else {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "错误操作类型");
        }
        List<MethodTask> taskAttributes = api.pageMethodTask(pageNum, pageSize);
        int count = api.count();
        return MessageManager
                .newMessageBuilder()
                .code(MessageManager.Code.OK)
                .message("查找成功")
                .data(taskAttributes)
                .addMsg("totalNum", count)
                .getFinalMessage();
    }

    @GetMapping(value = "/page_script_task", produces = "application/json;charset=UTF-8")
    public String pageScriptTask(@RequestParam(value = "pageNum", required = false) Integer pageNum, @RequestParam(value = "pageSize", required = false) Integer pageSize, @RequestParam(value = "type", required = false) Integer type) {
        if (pageNum == null || pageSize == null) {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "分页参数为空");
        }
        AutoJobAPI api;
        if (type == null || type == 1) {
            api = AutoJobApplication
                    .getInstance()
                    .getMemoryTaskAPI();
        } else if (type == 2) {
            api = AutoJobApplication
                    .getInstance()
                    .getDbTaskAPI();
        } else {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "错误操作类型");
        }
        List<ScriptTask> taskAttributes = api.pageScriptTask(pageNum, pageSize);
        int count = api.count();
        return MessageManager
                .newMessageBuilder()
                .code(MessageManager.Code.OK)
                .message("查找成功")
                .data(taskAttributes)
                .addMsg("totalNum", count)
                .getFinalMessage();
    }


    @PostMapping(value = "/edit_method_task", produces = "application/json;charset=UTF-8")
    public String editMethodTask(@RequestBody(required = false) MethodTaskEditParams params, @RequestParam(required = false, value = "TASK_ID") Long taskId) {
        if (params == null || ObjectUtil.isNull(params) || taskId == null) {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "请求参数为空");
        }
        AutoJobAPI autoJobAPI = AutoJobApplication
                .getInstance()
                .getMatchedAPI(taskId);
        if (autoJobAPI == null) {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "不存在任务：" + taskId);
        }
        if (autoJobAPI.editTask(taskId, params)) {
            return MessageManager.getJsonMessage(MessageManager.Code.OK, "修改成功");
        }
        return MessageManager.getJsonMessage(MessageManager.Code.ERROR, "修改失败");
    }

    @PostMapping(value = "/edit_script_task", produces = "application/json;charset=UTF-8")
    public String editScriptTask(@RequestBody(required = false) ScriptTaskEditParams params, @RequestParam(required = false, value = "TASK_ID") Long taskId) {
        if (params == null || ObjectUtil.isNull(params) || taskId == null) {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "请求参数为空");
        }
        AutoJobAPI autoJobAPI = AutoJobApplication
                .getInstance()
                .getMatchedAPI(taskId);
        if (autoJobAPI == null) {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "不存在任务：" + taskId);
        }
        if (autoJobAPI.editTask(taskId, params)) {
            return MessageManager.getJsonMessage(MessageManager.Code.OK, "修改成功");
        }
        return MessageManager.getJsonMessage(MessageManager.Code.ERROR, "修改失败");
    }

    @GetMapping(value = "/pause/{taskId}", produces = "application/json;charset=UTF-8")
    public String pause(@PathVariable("taskId") Long taskId) {
        if (taskId == null) {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "任务ID为空");
        }
        AutoJobAPI autoJobAPI = AutoJobApplication
                .getInstance()
                .getMatchedAPI(taskId);
        if (autoJobAPI == null) {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "不存在任务：" + taskId);
        }
        if (autoJobAPI.pause(taskId)) {
            return MessageManager.getJsonMessage(MessageManager.Code.OK, "暂停成功");
        }
        return MessageManager.getJsonMessage(MessageManager.Code.ERROR, "暂停失败");
    }

    @GetMapping(value = "/unpause/{taskId}", produces = "application/json;charset=UTF-8")
    public String unpause(@PathVariable("taskId") Long taskId) {
        if (taskId == null) {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "任务ID为空");
        }
        AutoJobAPI autoJobAPI = AutoJobApplication
                .getInstance()
                .getMatchedAPI(taskId);
        if (autoJobAPI == null) {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "不存在任务：" + taskId);
        }
        if (autoJobAPI.unpause(taskId)) {
            return MessageManager.getJsonMessage(MessageManager.Code.OK, "取消暂停成功");
        }
        return MessageManager.getJsonMessage(MessageManager.Code.ERROR, "取消暂停失败");
    }

    @PostMapping(value = "/edit_trigger", produces = "application/json;charset=UTF-8")
    public String editTrigger(@RequestBody(required = false) TriggerEditParams params, @RequestParam(required = false, value = "TASK_ID") Long taskId) {
        if (ObjectUtil.isNull(params) || taskId == null) {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "任务ID为空");
        }
        AutoJobAPI autoJobAPI = AutoJobApplication
                .getInstance()
                .getMatchedAPI(taskId);
        if (autoJobAPI == null) {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "不存在任务：" + taskId);
        }
        if (autoJobAPI.editTrigger(taskId, params)) {
            return MessageManager.getJsonMessage(MessageManager.Code.OK, "修改触发器成功");
        }
        return MessageManager.getJsonMessage(MessageManager.Code.ERROR, "修改触发器失败");
    }


}
