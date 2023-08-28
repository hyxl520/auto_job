drop table if exists aj_config;
CREATE TABLE `aj_config`
(
    `id`                 bigint   NOT NULL COMMENT '主键ID',
    `task_id`            bigint   NULL DEFAULT NULL COMMENT '任务ID',
    `content`            text COMMENT '配置详情',
    `content_type`       varchar(255)  DEFAULT NULL COMMENT '配置类型',
    `serialization_type` varchar(255)  DEFAULT NULL COMMENT '序列化类型',
    `status`             tinyint  NULL DEFAULT 1 COMMENT '是否启用',
    `write_timestamp`    bigint   NULL DEFAULT NULL COMMENT '写入时间戳',
    `create_time`        datetime NULL DEFAULT NULL COMMENT '创建时间',
    `del_flag`           tinyint  NULL DEFAULT 0 COMMENT '删除标识 0-未删除 1-已删除',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_task_id` (`task_id` ASC) USING BTREE
) COMMENT = '配置表';

drop table if exists aj_job_logs;
CREATE TABLE `aj_job_logs`
(
    `id`              bigint   NOT NULL COMMENT '主键ID',
    `scheduling_id`   bigint   NULL     DEFAULT NULL COMMENT '调度id',
    `task_id`         bigint   NOT NULL COMMENT '任务ID',
    `write_timestamp` bigint   NULL     DEFAULT NULL COMMENT '录入时间戳',
    `write_time`      datetime NULL     DEFAULT NULL COMMENT '写入时间',
    `log_level`       varchar(10)       DEFAULT NULL COMMENT '日志级别',
    `message`         text COMMENT '记录信息',
    `del_flag`        int      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_task_id_s_id` (`task_id` ASC, `scheduling_id` ASC) USING BTREE
) COMMENT = '任务日志表';

drop table if exists aj_job_table_matcher;
CREATE TABLE `aj_job_table_matcher`
(
    `id`        bigint NOT NULL COMMENT '主键ID',
    `task_id`   bigint NOT NULL COMMENT '任务ID',
    `task_type` int    NOT NULL DEFAULT 0 COMMENT '任务类型，目前已占用的类型有：0-方法型任务 1-脚本型任务',
    `status`    int    NOT NULL DEFAULT 1 COMMENT '状态 0-已停用 1-已启用',
    `del_flag`  int    NULL     DEFAULT 0,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_task_id_status_del` (`task_id` ASC) USING BTREE
) COMMENT = '任务类型匹配表';

drop table if exists aj_method_job;
CREATE TABLE `aj_method_job`
(
    `id`                    bigint   NOT NULL COMMENT '主键ID',
    `alias`                 varchar(255)      DEFAULT NULL COMMENT '任务别名',
    `version_id`            bigint   NULL     DEFAULT NULL COMMENT '注解ID',
    `method_class_name`     varchar(255)      DEFAULT NULL COMMENT '任务所在类路径',
    `method_name`           varchar(255)      DEFAULT NULL COMMENT '任务名称',
    `params`                text COMMENT '任务参数',
    `content`               mediumtext COMMENT '预留字段，GLUE模式',
    `method_object_factory` varchar(255)      DEFAULT NULL COMMENT '方法运行类工厂路径',
    `trigger_id`            bigint   NULL     DEFAULT NULL COMMENT '任务对应的触发器',
    `type`                  int      NOT NULL DEFAULT 0 COMMENT '任务类型，目前已占用的类型有：0-方法型任务 1-脚本型任务',
    `is_child_task`         int      NULL     DEFAULT NULL COMMENT '是否是子任务',
    `is_sharding_task`      varchar(255)      DEFAULT NULL COMMENT '是否是分片任务',
    `run_lock`              int      NOT NULL DEFAULT 0 COMMENT '启动锁 0-未上锁 1-已上锁',
    `task_level`            int      NULL     DEFAULT -1 COMMENT '任务优先级',
    `version`               bigint   NULL     DEFAULT NULL COMMENT '版本号',
    `running_status`        int      NOT NULL DEFAULT 0 COMMENT '任务运行状态',
    `belong_to`             bigint   NULL     DEFAULT NULL COMMENT '预留字段，所属于',
    `status`                int      NOT NULL DEFAULT 1 COMMENT '状态 0-已停用 1-已启用',
    `executable_machines`   varchar(255)      DEFAULT NULL COMMENT '执行的机器',
    `create_time`           datetime NULL     DEFAULT NULL,
    `del_flag`              int      NULL     DEFAULT 0,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_schedule` (`version_id` ASC) USING BTREE
) COMMENT = '方法型任务表';

drop table if exists aj_run_logs;
CREATE TABLE `aj_run_logs`
(
    `id`              bigint      NOT NULL COMMENT '主键',
    `scheduling_id`   bigint      NULL     DEFAULT NULL COMMENT '调度id',
    `task_id`         bigint      NOT NULL COMMENT '任务ID',
    `task_type`       varchar(10) NOT NULL COMMENT '任务类型：MEMORY_TASK：内存型任务 DB_TASK：数据库任务',
    `run_status`      int         NOT NULL COMMENT '1：运行成功 0：运行失败',
    `schedule_times`  int         NULL     DEFAULT 1 COMMENT '调度次数',
    `message`         text COMMENT '信息',
    `result`          varchar(255)         DEFAULT NULL COMMENT '任务结果',
    `error_stack`     text COMMENT '错误堆栈',
    `write_timestamp` bigint      NULL     DEFAULT NULL COMMENT '录入时间戳',
    `write_time`      datetime    NOT NULL COMMENT '录入时间',
    `del_flag`        int         NOT NULL DEFAULT 0 COMMENT '删除标识',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_task_id_s_id` (`task_id` ASC, `scheduling_id` ASC) USING BTREE
) COMMENT = '任务调度日志表';

drop table if exists aj_scheduling_record;
CREATE TABLE `aj_scheduling_record`
(
    `id`                bigint   NOT NULL COMMENT '主键',
    `write_timestamp`   bigint   NULL     DEFAULT NULL COMMENT '写入时间戳',
    `scheduling_time`   datetime NULL     DEFAULT NULL COMMENT '调度时间',
    `task_alias`        varchar(255)      DEFAULT NULL COMMENT '任务别名',
    `task_id`           bigint   NOT NULL COMMENT '任务Id',
    `is_success`        int      NULL     DEFAULT 1 COMMENT '是否执行成功 0-否 1-是',
    `is_run`            int      NOT NULL DEFAULT 0 COMMENT '是否正在运行 1-是 0-否',
    `scheduling_type`   int      NULL     DEFAULT NULL COMMENT '调度类型 0-普通调度 1-重试调度 2-分片调度',
    `sharding_id`       bigint   NULL     DEFAULT NULL COMMENT '分片ID',
    `executing_machine` varchar(255)      DEFAULT NULL COMMENT '执行机器',
    `result`            text COMMENT '任务结果 JSON序列化',
    `execution_time`    bigint   NULL     DEFAULT NULL COMMENT '执行时长:ms',
    `del_flag`          int      NOT NULL DEFAULT 0 COMMENT '删除标识',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_task_id` (`task_id` ASC) USING BTREE
) COMMENT = '调度记录表';

drop table if exists aj_script_job;
CREATE TABLE `aj_script_job`
(
    `id`                  bigint   NOT NULL COMMENT '主键ID',
    `alias`               varchar(255)      DEFAULT NULL COMMENT '任务别名',
    `version_id`          bigint   NULL     DEFAULT NULL COMMENT '注解ID',
    `params`              text COMMENT '任务参数',
    `script_content`      mediumtext COMMENT '任务内容，用于存放脚本任务的脚本',
    `script_path`         text COMMENT '脚本路径',
    `script_file_name`    varchar(255)      DEFAULT NULL COMMENT '脚本文件名',
    `script_cmd`          varchar(255)      DEFAULT NULL COMMENT '脚本命令行',
    `trigger_id`          bigint   NULL     DEFAULT NULL COMMENT '任务对应的触发器',
    `type`                int      NOT NULL DEFAULT 0 COMMENT '任务类型，目前已占用的类型有：0-方法型任务 1-脚本型任务',
    `is_child_task`       int      NULL     DEFAULT NULL COMMENT '是否是子任务',
    `is_sharding_task`    int      NULL     DEFAULT NULL COMMENT '是否是分片任务',
    `run_lock`            int      NOT NULL DEFAULT 0 COMMENT '启动锁 0-未上锁 1-已上锁',
    `task_level`          int      NULL     DEFAULT -1 COMMENT '任务优先级',
    `version`             bigint   NULL     DEFAULT NULL COMMENT '版本号',
    `running_status`      int      NOT NULL DEFAULT 0 COMMENT '任务运行状态',
    `belong_to`           bigint   NULL     DEFAULT NULL COMMENT '预留字段，所属于',
    `status`              int      NOT NULL DEFAULT 1 COMMENT '状态 0-已停用 1-已启用',
    `executable_machines` varchar(255)      DEFAULT NULL COMMENT '执行的机器',
    `create_time`         datetime NULL     DEFAULT NULL,
    `del_flag`            int      NULL     DEFAULT 0,
    PRIMARY KEY (`id`) USING BTREE
) COMMENT = '脚本型任务表';

drop table if exists aj_trigger;
CREATE TABLE `aj_trigger`
(
    `id`                     bigint   NOT NULL COMMENT '主键ID',
    `cron_expression`        varchar(255)      DEFAULT NULL COMMENT 'cronlike表达式',
    `last_run_time`          bigint   NULL     DEFAULT NULL COMMENT '上次运行时长',
    `last_triggering_time`   bigint   NULL     DEFAULT NULL COMMENT '上次触发时间',
    `next_triggering_time`   bigint   NULL     DEFAULT NULL COMMENT '下次触发时间',
    `is_last_success`        int      NULL     DEFAULT NULL COMMENT '上次调度是否成功 0-否 1-是',
    `repeat_times`           int      NULL     DEFAULT 1 COMMENT '重复次数',
    `finished_times`         int      NULL     DEFAULT 0 COMMENT '已完成次数',
    `current_repeat_times`   int      NULL     DEFAULT NULL COMMENT '当前重试次数',
    `cycle`                  bigint   NULL     DEFAULT NULL COMMENT '任务周期',
    `task_id`                bigint   NULL     DEFAULT NULL COMMENT '任务Id',
    `child_tasks_id`         varchar(255)      DEFAULT NULL COMMENT '子任务ID，多个逗号分割',
    `maximum_execution_time` bigint   NULL     DEFAULT NULL COMMENT '最大运行时长',
    `is_run`                 int      NOT NULL DEFAULT 0 COMMENT '是否正在运行 0-否 1-是',
    `is_pause`               int      NOT NULL DEFAULT 0 COMMENT '是否暂停调度 0-否 1-是',
    `create_time`            datetime NULL     DEFAULT NULL COMMENT '创建时间',
    `del_flag`               int      NULL     DEFAULT 0,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_task_id` (`task_id` ASC) USING BTREE,
    INDEX `idx_schedule` (`next_triggering_time` ASC) USING BTREE
) COMMENT = '触发器表';

