package com.jingge.spring.controller;

import com.jingge.autojob.api.log.AutoJobLogDBAPI;
import com.jingge.autojob.logging.domain.AutoJobLog;
import com.jingge.autojob.logging.domain.AutoJobRunLog;
import com.jingge.autojob.logging.domain.AutoJobSchedulingRecord;
import com.jingge.autojob.skeleton.framework.boot.AutoJobApplication;
import com.jingge.autojob.util.message.MessageManager;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 测试使用Rest接口查询日志
 *
 * @author Huang Yongxiang
 * @date 2022-12-27 15:10
 * @email 1158055613@qq.com
 */
@RestController("autoJobSpringLogControoller")
@RequestMapping("/auto_job_spring_log")
public class AutoJobLogController {
    @GetMapping(value = "/page_scheduling_record/{taskId}", produces = "application/json;charset=UTF-8")
    public String pageSchedulingRecords(@PathVariable("taskId") Long taskId, @RequestParam(value = "PAGE_COUNT", required = false) Integer pageCount, @RequestParam(value = "SIZE", required = false) Integer size) {
        if (taskId == null) {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "任务ID为空");
        }
        if (pageCount == null || size == null) {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "请指定分页信息");
        }
        AutoJobLogDBAPI api = AutoJobApplication
                .getInstance()
                .getLogDbAPI();
        List<AutoJobSchedulingRecord> records = api.page(pageCount, size, taskId);
        int count = api.count(taskId);
        return MessageManager
                .newMessageBuilder()
                .code(MessageManager.Code.OK)
                .message("查找成功")
                .addMsg("totalNum", count)
                .data(records)
                .getFinalMessage();
    }

    @GetMapping(value = "/find_log/{schedulingId}", produces = "application/json;charset=UTF-8")
    public String findLog(@PathVariable("schedulingId") Long schedulingId) {
        if (schedulingId == null) {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "任务ID为空");
        }
        AutoJobLogDBAPI api = AutoJobApplication
                .getInstance()
                .getLogDbAPI();
        List<AutoJobLog> logList = api.findLogsBySchedulingId(schedulingId);
        return MessageManager
                .newMessageBuilder()
                .code(MessageManager.Code.OK)
                .message("查找成功")
                .addMsg("totalNum", logList.size())
                .data(logList)
                .getFinalMessage();
    }

    @GetMapping(value = "/find_run_log/{schedulingId}", produces = "application/json;charset=UTF-8")
    public String findRunLog(@PathVariable("schedulingId") Long schedulingId) {
        if (schedulingId == null) {
            return MessageManager.getJsonMessage(MessageManager.Code.BAD_REQUEST, "任务ID为空");
        }
        AutoJobLogDBAPI api = AutoJobApplication
                .getInstance()
                .getLogDbAPI();
        List<AutoJobRunLog> logList = api.findRunLogsBySchedulingId(schedulingId);
        return MessageManager
                .newMessageBuilder()
                .code(MessageManager.Code.OK)
                .message("查找成功")
                .addMsg("totalNum", logList.size())
                .data(logList)
                .getFinalMessage();
    }

}
