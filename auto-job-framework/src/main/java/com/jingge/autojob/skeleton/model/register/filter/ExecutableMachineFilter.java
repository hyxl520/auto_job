package com.jingge.autojob.skeleton.model.register.filter;

import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.model.register.AbstractRegisterFilter;
import com.jingge.autojob.util.convert.RegexUtil;
import com.jingge.autojob.util.servlet.InetUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-05-19 14:25
 * @email 1158055613@qq.com
 */
@Slf4j
public class ExecutableMachineFilter extends AbstractRegisterFilter {
    @Override
    public void doHandle(AutoJobTask task) {
        if (task.getExecutableMachines() != null && task
                .getExecutableMachines()
                .size() > 0) {
            String ip = InetUtil.getLocalhostIp();
            String ipPort = InetUtil.getTCPAddress();
            for (String pattern : task.getExecutableMachines()) {
                if (pattern.contains("*")) {
                    String zPattern = RegexUtil.wildcardToRegexString(pattern);
                    if (RegexUtil.isMatch(ip, zPattern)) {
                        log.debug("任务{}的ip pattern {}与ip{}匹配成功", task.getId(), pattern, ip);
                        return;
                    }
                    if (RegexUtil.isMatch(ipPort, zPattern)) {
                        log.debug("任务{}的ip pattern {}与ip{}匹配成功", task.getId(), pattern, ipPort);
                        return;
                    }
                } else {
                    if (pattern
                            .trim()
                            .equalsIgnoreCase(ip) || pattern
                            .trim()
                            .equalsIgnoreCase(ipPort)) {
                        log.debug("任务{}的ip pattern {}与ip{}({})精确匹配成功", task.getId(), pattern, ip, ipPort);
                        return;
                    }
                }
            }
            log.debug("任务：{}的执行机器与此机器：{}不匹配", task.getId(), InetUtil.getLocalhostIp());
            task.forbidden();
        }
        if (chain != null) {
            chain.doHandle(task);
        }
    }
}
