package com.jingge.autojob.logging.model.memory;

import java.util.List;

/**
 * 内存日志缓存的统一接口，内存存储日志在框架内部只做测试使用，建议生产上应该将日志存到数据库
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/12 16:17
 */
public interface IAutoJobLogCache<L> {
    boolean insert(String taskPath, L log);

    boolean insertAll(String taskPath, List<L> autoJobLogs);

    boolean exist(String taskPath);

    List<L> get(String taskPath);

    boolean remove(String taskPath);
}
