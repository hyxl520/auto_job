CREATE TABLE "aj_config"
(
    "id"                 NUMBER(20) NOT NULL,
    "task_id"            NUMBER(20),
    "content"            CLOB,
    "content_type"       VARCHAR2(255),
    "serialization_type" VARCHAR2(255),
    "status"             NUMBER(6),
    "write_timestamp"    NUMBER(20),
    "create_time"        DATE,
    "del_flag"           NUMBER(6),
    PRIMARY KEY ("id")
);
CREATE INDEX "idx_task_id"
    ON "aj_config" ("task_id" ASC);
COMMENT ON COLUMN "aj_config"."id" IS '主键ID';
COMMENT ON COLUMN "aj_config"."task_id" IS '任务ID';
COMMENT ON COLUMN "aj_config"."content" IS '配置详情';
COMMENT ON COLUMN "aj_config"."content_type" IS '配置类型';
COMMENT ON COLUMN "aj_config"."serialization_type" IS '序列化类型';
COMMENT ON COLUMN "aj_config"."status" IS '是否启用';
COMMENT ON COLUMN "aj_config"."write_timestamp" IS '写入时间戳';
COMMENT ON COLUMN "aj_config"."create_time" IS '创建时间';
COMMENT ON COLUMN "aj_config"."del_flag" IS '删除标识 0-未删除 1-已删除';
COMMENT ON TABLE "aj_config" IS '配置表';

CREATE TABLE "aj_job_logs"
(
    "id"              NUMBER(20) NOT NULL,
    "scheduling_id"   NUMBER(20),
    "task_id"         NUMBER(20) NOT NULL,
    "write_timestamp" NUMBER(20),
    "write_time"      DATE,
    "log_level"       VARCHAR2(10),
    "message"         CLOB,
    "del_flag"        NUMBER(11) NOT NULL,
    CONSTRAINT "_copy_7" PRIMARY KEY ("id")
);
CREATE INDEX "idx_task_id_s_id"
    ON "aj_job_logs" ("task_id" ASC, "scheduling_id" ASC);
COMMENT ON COLUMN "aj_job_logs"."id" IS '主键ID';
COMMENT ON COLUMN "aj_job_logs"."scheduling_id" IS '调度id';
COMMENT ON COLUMN "aj_job_logs"."task_id" IS '任务ID';
COMMENT ON COLUMN "aj_job_logs"."write_timestamp" IS '录入时间戳';
COMMENT ON COLUMN "aj_job_logs"."write_time" IS '写入时间';
COMMENT ON COLUMN "aj_job_logs"."log_level" IS '日志级别';
COMMENT ON COLUMN "aj_job_logs"."message" IS '记录信息';
COMMENT ON TABLE "aj_job_logs" IS '任务日志表';

CREATE TABLE "aj_job_table_matcher"
(
    "id"        NUMBER(20) NOT NULL,
    "task_id"   NUMBER(20) NOT NULL,
    "task_type" NUMBER(11) NOT NULL,
    "status"    NUMBER(11) NOT NULL,
    "del_flag"  NUMBER(11),
    CONSTRAINT "_copy_6" PRIMARY KEY ("id")
);
CREATE INDEX "idx_task_id_status_del"
    ON "aj_job_table_matcher" ("task_id" ASC);
COMMENT ON COLUMN "aj_job_table_matcher"."id" IS '主键ID';
COMMENT ON COLUMN "aj_job_table_matcher"."task_id" IS '任务ID';
COMMENT ON COLUMN "aj_job_table_matcher"."task_type" IS '任务类型，目前已占用的类型有：0-方法型任务 1-脚本型任务';
COMMENT ON COLUMN "aj_job_table_matcher"."status" IS '状态 0-已停用 1-已启用';
COMMENT ON TABLE "aj_job_table_matcher" IS '任务类型匹配表';

CREATE TABLE "aj_method_job"
(
    "id"                    NUMBER(20) NOT NULL,
    "alias"                 VARCHAR2(255),
    "version_id"            NUMBER(20),
    "method_class_name"     VARCHAR2(255),
    "method_name"           VARCHAR2(255),
    "params"                CLOB,
    "content"               CLOB,
    "method_object_factory" VARCHAR2(255),
    "trigger_id"            NUMBER(20),
    "type"                  NUMBER(11) NOT NULL,
    "is_child_task"         NUMBER(11),
    "is_sharding_task"      VARCHAR2(255),
    "run_lock"              NUMBER(11) NOT NULL,
    "task_level"            NUMBER(11),
    "version"               NUMBER(20),
    "running_status"        NUMBER(11) NOT NULL,
    "belong_to"             NUMBER(20),
    "status"                NUMBER(11) NOT NULL,
    "executable_machines"   VARCHAR2(255),
    "create_time"           DATE,
    "del_flag"              NUMBER(11),
    CONSTRAINT "_copy_5" PRIMARY KEY ("id")
);
CREATE INDEX "idx_schedule"
    ON "aj_method_job" ("version_id" ASC);
COMMENT ON COLUMN "aj_method_job"."id" IS '主键ID';
COMMENT ON COLUMN "aj_method_job"."alias" IS '任务别名';
COMMENT ON COLUMN "aj_method_job"."version_id" IS '注解ID';
COMMENT ON COLUMN "aj_method_job"."method_class_name" IS '任务所在类路径';
COMMENT ON COLUMN "aj_method_job"."method_name" IS '任务名称';
COMMENT ON COLUMN "aj_method_job"."params" IS '任务参数';
COMMENT ON COLUMN "aj_method_job"."content" IS '预留字段，GLUE模式';
COMMENT ON COLUMN "aj_method_job"."method_object_factory" IS '方法运行类工厂路径';
COMMENT ON COLUMN "aj_method_job"."trigger_id" IS '任务对应的触发器';
COMMENT ON COLUMN "aj_method_job"."type" IS '任务类型，目前已占用的类型有：0-方法型任务 1-脚本型任务';
COMMENT ON COLUMN "aj_method_job"."is_child_task" IS '是否是子任务';
COMMENT ON COLUMN "aj_method_job"."is_sharding_task" IS '是否是分片任务';
COMMENT ON COLUMN "aj_method_job"."run_lock" IS '启动锁 0-未上锁 1-已上锁';
COMMENT ON COLUMN "aj_method_job"."task_level" IS '任务优先级';
COMMENT ON COLUMN "aj_method_job"."version" IS '版本号';
COMMENT ON COLUMN "aj_method_job"."running_status" IS '任务运行状态';
COMMENT ON COLUMN "aj_method_job"."belong_to" IS '预留字段，所属于';
COMMENT ON COLUMN "aj_method_job"."status" IS '状态 0-已停用 1-已启用';
COMMENT ON COLUMN "aj_method_job"."executable_machines" IS '执行的机器';
COMMENT ON TABLE "aj_method_job" IS '方法型任务表';

CREATE TABLE "aj_run_logs"
(
    "id"              NUMBER(20)   NOT NULL,
    "scheduling_id"   NUMBER(20),
    "task_id"         NUMBER(20)   NOT NULL,
    "task_type"       VARCHAR2(10) NOT NULL,
    "run_status"      NUMBER(11)   NOT NULL,
    "schedule_times"  NUMBER(11),
    "message"         CLOB,
    "result"          VARCHAR2(255),
    "error_stack"     CLOB,
    "write_timestamp" NUMBER(20),
    "write_time"      DATE         NOT NULL,
    "del_flag"        NUMBER(11)   NOT NULL,
    CONSTRAINT "_copy_4" PRIMARY KEY ("id")
);
CREATE INDEX "idx_task_id_s_id_copy_1"
    ON "aj_run_logs" ("task_id" ASC, "scheduling_id" ASC);
COMMENT ON COLUMN "aj_run_logs"."id" IS '主键';
COMMENT ON COLUMN "aj_run_logs"."scheduling_id" IS '调度id';
COMMENT ON COLUMN "aj_run_logs"."task_id" IS '任务ID';
COMMENT ON COLUMN "aj_run_logs"."task_type" IS '任务类型：MEMORY_TASK：内存型任务 DB_TASK：数据库任务';
COMMENT ON COLUMN "aj_run_logs"."run_status" IS '1：运行成功 0：运行失败';
COMMENT ON COLUMN "aj_run_logs"."schedule_times" IS '调度次数';
COMMENT ON COLUMN "aj_run_logs"."message" IS '信息';
COMMENT ON COLUMN "aj_run_logs"."result" IS '任务结果';
COMMENT ON COLUMN "aj_run_logs"."error_stack" IS '错误堆栈';
COMMENT ON COLUMN "aj_run_logs"."write_timestamp" IS '录入时间戳';
COMMENT ON COLUMN "aj_run_logs"."write_time" IS '录入时间';
COMMENT ON COLUMN "aj_run_logs"."del_flag" IS '删除标识';
COMMENT ON TABLE "aj_run_logs" IS '任务调度日志表';

CREATE TABLE "aj_scheduling_record"
(
    "id"                NUMBER(20) NOT NULL,
    "write_timestamp"   NUMBER(20),
    "scheduling_time"   DATE,
    "task_alias"        VARCHAR2(255),
    "task_id"           NUMBER(20) NOT NULL,
    "is_success"        NUMBER(11),
    "is_run"            NUMBER(11) NOT NULL,
    "scheduling_type"   NUMBER(11),
    "sharding_id"       NUMBER(20),
    "executing_machine" VARCHAR2(255),
    "result"            CLOB,
    "execution_time"    NUMBER(20),
    "del_flag"          NUMBER(11) NOT NULL,
    CONSTRAINT "_copy_3" PRIMARY KEY ("id")
);
CREATE INDEX "idx_task_id_copy_2"
    ON "aj_scheduling_record" ("task_id" ASC);
COMMENT ON COLUMN "aj_scheduling_record"."id" IS '主键';
COMMENT ON COLUMN "aj_scheduling_record"."write_timestamp" IS '写入时间戳';
COMMENT ON COLUMN "aj_scheduling_record"."scheduling_time" IS '调度时间';
COMMENT ON COLUMN "aj_scheduling_record"."task_alias" IS '任务别名';
COMMENT ON COLUMN "aj_scheduling_record"."task_id" IS '任务Id';
COMMENT ON COLUMN "aj_scheduling_record"."is_success" IS '是否执行成功 0-否 1-是';
COMMENT ON COLUMN "aj_scheduling_record"."is_run" IS '是否正在运行 1-是 0-否';
COMMENT ON COLUMN "aj_scheduling_record"."scheduling_type" IS '调度类型 0-普通调度 1-重试调度 2-分片调度';
COMMENT ON COLUMN "aj_scheduling_record"."sharding_id" IS '分片ID';
COMMENT ON COLUMN "aj_scheduling_record"."executing_machine" IS '执行机器';
COMMENT ON COLUMN "aj_scheduling_record"."result" IS '任务结果 JSON序列化';
COMMENT ON COLUMN "aj_scheduling_record"."execution_time" IS '执行时长:ms';
COMMENT ON COLUMN "aj_scheduling_record"."del_flag" IS '删除标识';
COMMENT ON TABLE "aj_scheduling_record" IS '调度记录表';

CREATE TABLE "aj_script_job"
(
    "id"                  NUMBER(20) NOT NULL,
    "alias"               VARCHAR2(255),
    "version_id"          NUMBER(20),
    "params"              CLOB,
    "script_content"      CLOB,
    "script_path"         CLOB,
    "script_file_name"    VARCHAR2(255),
    "script_cmd"          VARCHAR2(255),
    "trigger_id"          NUMBER(20),
    "type"                NUMBER(11) NOT NULL,
    "is_child_task"       NUMBER(11),
    "is_sharding_task"    NUMBER(11),
    "run_lock"            NUMBER(11) NOT NULL,
    "task_level"          NUMBER(11),
    "version"             NUMBER(20),
    "running_status"      NUMBER(11) NOT NULL,
    "belong_to"           NUMBER(20),
    "status"              NUMBER(11) NOT NULL,
    "executable_machines" VARCHAR2(255),
    "create_time"         DATE,
    "del_flag"            NUMBER(11),
    CONSTRAINT "_copy_2" PRIMARY KEY ("id")
);
COMMENT ON COLUMN "aj_script_job"."id" IS '主键ID';
COMMENT ON COLUMN "aj_script_job"."alias" IS '任务别名';
COMMENT ON COLUMN "aj_script_job"."version_id" IS '注解ID';
COMMENT ON COLUMN "aj_script_job"."params" IS '任务参数';
COMMENT ON COLUMN "aj_script_job"."script_content" IS '任务内容，用于存放脚本任务的脚本';
COMMENT ON COLUMN "aj_script_job"."script_path" IS '脚本路径';
COMMENT ON COLUMN "aj_script_job"."script_file_name" IS '脚本文件名';
COMMENT ON COLUMN "aj_script_job"."script_cmd" IS '脚本命令行';
COMMENT ON COLUMN "aj_script_job"."trigger_id" IS '任务对应的触发器';
COMMENT ON COLUMN "aj_script_job"."type" IS '任务类型，目前已占用的类型有：0-方法型任务 1-脚本型任务';
COMMENT ON COLUMN "aj_script_job"."is_child_task" IS '是否是子任务';
COMMENT ON COLUMN "aj_script_job"."is_sharding_task" IS '是否是分片任务';
COMMENT ON COLUMN "aj_script_job"."run_lock" IS '启动锁 0-未上锁 1-已上锁';
COMMENT ON COLUMN "aj_script_job"."task_level" IS '任务优先级';
COMMENT ON COLUMN "aj_script_job"."version" IS '版本号';
COMMENT ON COLUMN "aj_script_job"."running_status" IS '任务运行状态';
COMMENT ON COLUMN "aj_script_job"."belong_to" IS '预留字段，所属于';
COMMENT ON COLUMN "aj_script_job"."status" IS '状态 0-已停用 1-已启用';
COMMENT ON COLUMN "aj_script_job"."executable_machines" IS '执行的机器';
COMMENT ON TABLE "aj_script_job" IS '脚本型任务表';

CREATE TABLE "aj_trigger"
(
    "id"                     NUMBER(20) NOT NULL,
    "cron_expression"        VARCHAR2(255),
    "last_run_time"          NUMBER(20),
    "last_triggering_time"   NUMBER(20),
    "next_triggering_time"   NUMBER(20),
    "is_last_success"        NUMBER(11),
    "repeat_times"           NUMBER(11),
    "finished_times"         NUMBER(11),
    "current_repeat_times"   NUMBER(11),
    "cycle"                  NUMBER(20),
    "task_id"                NUMBER(20),
    "child_tasks_id"         VARCHAR2(255),
    "maximum_execution_time" NUMBER(20),
    "is_run"                 NUMBER(11) NOT NULL,
    "is_pause"               NUMBER(11) NOT NULL,
    "create_time"            DATE,
    "del_flag"               NUMBER(11),
    CONSTRAINT "_copy_1" PRIMARY KEY ("id")
);
CREATE INDEX "idx_task_id_copy_1"
    ON "aj_trigger" ("task_id" ASC);
CREATE INDEX "idx_schedule_copy_1"
    ON "aj_trigger" ("next_triggering_time" ASC);
COMMENT ON COLUMN "aj_trigger"."id" IS '主键ID';
COMMENT ON COLUMN "aj_trigger"."cron_expression" IS 'cronlike表达式';
COMMENT ON COLUMN "aj_trigger"."last_run_time" IS '上次运行时长';
COMMENT ON COLUMN "aj_trigger"."last_triggering_time" IS '上次触发时间';
COMMENT ON COLUMN "aj_trigger"."next_triggering_time" IS '下次触发时间';
COMMENT ON COLUMN "aj_trigger"."is_last_success" IS '上次调度是否成功 0-否 1-是';
COMMENT ON COLUMN "aj_trigger"."repeat_times" IS '重复次数';
COMMENT ON COLUMN "aj_trigger"."finished_times" IS '已完成次数';
COMMENT ON COLUMN "aj_trigger"."current_repeat_times" IS '当前重试次数';
COMMENT ON COLUMN "aj_trigger"."cycle" IS '任务周期';
COMMENT ON COLUMN "aj_trigger"."task_id" IS '任务Id';
COMMENT ON COLUMN "aj_trigger"."child_tasks_id" IS '子任务ID，多个逗号分割';
COMMENT ON COLUMN "aj_trigger"."maximum_execution_time" IS '最大运行时长';
COMMENT ON COLUMN "aj_trigger"."is_run" IS '是否正在运行 0-否 1-是';
COMMENT ON COLUMN "aj_trigger"."is_pause" IS '是否暂停调度 0-否 1-是';
COMMENT ON COLUMN "aj_trigger"."create_time" IS '创建时间';
COMMENT ON TABLE "aj_trigger" IS '触发器表';

