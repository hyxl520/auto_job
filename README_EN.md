# Auto-Job Task Scheduling Framework v0.9.6

## <span style="color:#FFFE91;"> Version update</span>

**2022-11-22：** First version 0.9.1

------



**2022-12-9：** 0.9.2

Optimize the API.

The core scheduling module stops using the cache timestamp to solve the problem of low time accuracy.

Reconstruct the annotation scheduler.

Reconstruct the executor pool and add the thread pool encapsulation `TimerThreadPoolExecutorHelper` based on dynamic adjustment of time.

Modify the known BUG.

Adjust the project structure.

**2022-12-26：** New Rest Interface: Link: https://www.apifox.cn/apidoc/shared-05120000-4d19-4c75-a7f6-dc56105972cb  Access Password: autojob

**2022-12-27：** Optimize subtask processing logic and add MissFire event

**2022-12-29**: Multi-database adaptation is added. Currently MySQL and postresql are supported.

**2023-1-3：** 

Optimize the retry mechanism. Retry logic can be configured separately for each task

Optimize the mail alert mechanism. Each task can configure the mailbox separately.

Add Task Retry Event `TaskRetryEvent`

An attribute is added `is_retry` to the scheduling record table, indicating whether the scheduling record is retried due to an exception

Other known bugs

------



**2023-3-14：**  0.9.3

The built-in RPC framework codec was changed to use ProtoStuff, simplifying the API.

The configuration supports nested configuration, and the content can be given through VMOptions to protect the system security.

Support for environment variables for easy switching between test and production environments. See: “IV. Environment Variables”

------



**2023-3-28：** 0.9.4

The task table is split into AJ _ method _ job and AJ _ script _ job to facilitate the expansion of task types in the future. **Note that version 0.9.4 is not compatible with the database structure of previous versions.**

Optimize the log processing logic from the processor private processing thread to the loop polling to avoid thread resource exhaustion caused by too many concurrent tasks.

Task running context and task running stack are added to facilitate fault recovery. See “IX. Task Running Context; XI. All Configurations” for details.

New template tasks to develop tasks more elegantly. For details, see “XII. Advanced Usage-Advanced Application of Annotation Task Development- `@TemplateAutoJob`”.

------



**2023-7-12：** 0.9.5

Solve the log BUG: the log message queue is not emptied. Resolve the issue of repeated startup during cluster deployment. New task attribute: executableMachines, which is used to specify the execution machine. Only the machine with matching address can be executed. **If the database structure is changed (the executable _ machines column is added to AJ _ method _ job and AJ _ script _ job), it can be added according to the SQL script. Do not execute the script directly, otherwise it will cause data loss.**

------



0.9.6 **2023-7-12：** (<span style="color:#7DF46A;"> latest </span>)

**What’s New:**

- Tasks run concurrently: The same task can run multiple instances at the same time.
- Task failover: a new failover policy is added to the retry policy, which is valid when the cluster mode is enabled. An abnormal task will select a “better” node for failover execution.
- Dynamic task fragmentation: The fragmentation task is supported in the cluster mode. The node will automatically sense the number of nodes in the current cluster and dynamically fragment according to the cluster situation.
- Task saving policy: support saving when it does not exist, creating a new version, and updating the policy when it exists.
- Annotated Script Tasks: You can use `@ScriptJob` annotations to define a script task directly on a Java method, supporting dynamic command lines.
- Function task: Some business logics can be managed by AutoJob in a very simple way. AutoJob will provide the functions of fault retry, long interrupt and log storage for these business logics.

Other performance optimization and BUG resolution.

**Description of the update:**

<span style="color:#FFFD55;">This update involves changes in the database structure. You need to re-execute the SQL script. Please back up the data first.</span>


## I. Background

In daily life, we will encounter many scenarios related to job scheduling in business, such as issuing coupons at 52 o’clock every week, or warming up the cache in the early morning every day, or regularly drawing numbers from third-party systems every month, etc. Spring and Java also have native timing task support at present, but they all have some disadvantages, as follows:

- **Does not support clustering and does not avoid the problem of repeated execution of tasks**
- **Does not support unified lifecycle management**
- **Fragmented tasks are not supported: when processing sequential data, multiple machines execute tasks in fragments to process different data**
- **Failure retry is not supported: abnormal task termination occurs, and task re-execution cannot be controlled according to the execution status**
- **Not well integrated with enterprise systems, such as not well integrated with the front end of enterprise systems and not well embedded into back-end services**
- **Dynamic adjustment is not supported: task parameters cannot be modified without restarting the service**
- **No alarm mechanism: no alarm notification (email, SMS) after task failure**
- **No good execution log and schedule log tracking**

Based on these disadvantages of the native timed task, AutoJob was born. AutoJob provides a new idea and solution for distributed job scheduling.

## 2. Characteristics

**Simple** Simplicity includes simplicity of integration, simplicity of development and simplicity of use.

Simple integration: The framework can be easily integrated into Spring projects and non-Spring projects. Thanks to the fact that AutoJob does not depend on the Spring container environment and the MyBatis environment, you don’t have to build a Spring application to use the framework.

Simple development: The original intention of AutoJob development is to have the characteristics of low code intrusion and rapid development. As follows, in any class, you only need to add annotations on a task that needs to be scheduled, and the task will be dynamically scheduled by the framework:


```java
	@AutoJob(attributes = "{'我爱你，心连心',12.5,12,true}", cronExpression = "5/7 * * * * ?")
    public void formatAttributes(String string, Double decimal, Integer num, Boolean flag) {
        //attributes init
        AutoJobLogHelper logger = new AutoJobLogHelper();//use log
        logger.setSlf4jProxy(log);//proxy slf4j
        logger.info("string={}", string);
        logger.warn("decimal={}", decimal);
        logger.debug("num={}", num);
        logger.error("flag={}", flag);
        //use mapper
        mapper.selectById(21312L);
        //...
    }
```

Simple to use: You don’t need to pay much attention to the configuration when using the framework. The whole framework only needs to be **One line of code** started, as follows:


```java
//config scan path
@AutoJobScan({"com.yourpackage"})
//enable processor scan
@AutoJobProcessorScan({"com.yourpackage"})
public class AutoJobMainApplication {
    public static void main(String[] args) {
    //start the framework
    	new AutoJobBootstrap(AutoJobMainApplication.class)
                .build()
                .run();
        System.out.println("==================================>system start success");
 	}

}
```

Thanks to good system architecture and coding design, your application doesn’t need much configuration to start, just one line of code.

**Dynamic:** The framework provides API to support dynamic CURD operation of tasks, which takes effect immediately.

**Multiple database support:** Provide multi-type database support, currently support MySQL, PostgreSQL, Oracle, DamengSQL, and theoretically support all databases of SQL standard.

**Task dependency:** Support the configuration of subtasks. When the parent task is completed and executed successfully, it will actively trigger the execution of a subtask.

**Consistency:** The framework uses DB optimistic lock to achieve the consistency of the task. In the cluster mode, the scheduler will try to obtain the lock before scheduling the task, and will schedule the task after obtaining the lock successfully.

**HA<span style="color:#FFFE91;">（new）</span>：** The framework supports decentralized cluster deployment, where cluster nodes communicate via RPC encryption. Failover between cluster nodes occurs automatically.

**Elastic shrinkage capacity<span style="color:#FFFE91;">（new）</span>：** Support the dynamic online and offline of the node, and at the same time, the node supports to open the protection mode to prevent the node from leaving the cluster in the harsh network environment.

**Task failure retry**<span style="color:#FFFE91;">（new）</span>：Failover and local retry are  supported.

**Complete life cycle:** The framework provides a complete life cycle event of the task, which can be captured and processed by the business.

**Dynamically dispatch thread pool:** The framework uses a self-developed dynamic thread pool, which can flexibly and dynamically adjust the core thread and maximum thread parameters of the thread pool according to the task flow, save the system thread resources, and provide a default rejection processor to prevent the task from being missFire.

**Asynchronous non-blocking log processing:** The log uses the producer-consumer model, based on the self-developed memory message queue, the task method as the producer of the log, the production log is put into the message queue, and the framework starts the corresponding log consumption thread for log processing.

**Real-time log:** The log will be saved in real time for easy tracking.

The task white list function is **Task whitelist:** provided. Only the tasks in the white list are allowed to be registered and scheduled to ensure system security.

**Extensible log storage strategy:** The log supports multiple saving strategies, such as memory Cache, database, etc., and can flexibly add saving strategies according to the needs of the project, such as Redis, file, etc.

**Rich scheduling mechanism:** Support Cron like expression, repeat-cycle scheduling, subtask triggering, delay triggering, etc. Thanks to good coding design, users can easily add custom schedulers as follows:


```java
/**
 * your scheduler
 * @Author Huang Yongxiang
 * @Date 2022/08/18 14:56
 */
public class YourScheduler extends AbstractScheduler{
    public YourScheduler(AutoJobTaskExecutorPool executorPool, IAutoJobRegister register, AutoJobConfigHolder configHolder) {
        super(executorPool, register, configHolder);
    }
    
    //...logic
}

@AutoJobScan("com.example.autojob.job")
@AutoJobProcessorScan("com.example.autojob")
public class AutoJobMainApplication {
    public static void main(String[] args) {
        new AutoJobLauncherBuilder(AutoJobMainApplication.class)
                .withAutoScanProcessor()
            	//config
                .addScheduler(YourScheduler.class)
                .build()
                .run();
        System.out.println("==================================>system start success");
    }
}
```

**Task alarm:** The framework supports mail alarm. Currently, it supports QQ mailbox, 163 mailbox, GMail, etc. It also supports customized mailbox SMTP server.

![1668580284754](https://gitee.com/hyxl-520/auto-job/raw/master/doc/%E9%82%AE%E4%BB%B6%E6%8A%A5%E8%AD%A6.png)

At present, the system provides: task failure alarm, task rejection alarm, node open protection mode alarm, node close protection mode alarm. Of course, the user can also simply expand the email alarm.

**Abundant task input parameters:** The framework supports task input parameters of basic data types and object types, such as Boolean, String, Long, Integer, and Double. For object input parameters, the framework uses JSON to serialize input parameters by default.

**Good front-end integration:** The framework provides relevant APIs, and users can flexibly develop Restful interfaces to access enterprise projects without occupying an additional process or machine to run the scheduling center separately.

**Memory tasks:** The framework provides two types of DB tasks and memory tasks. The DB tasks are persisted to the database, and the declaration cycle is recorded in the database. In addition to the log, the entire life cycle of the memory tasks is completed in the memory. Compared with the DB tasks, it has the characteristics of no lock and fast scheduling.

**Script Tasks:** Provide the execution of script tasks, such as Python, Shell, SQL, etc.

**Dynamic sharding**<span style="color:#FFFE91;">（new）</span>：Under the  cluster mode, the framework supports task fragmentation and multi-machine operation.

**The Fully asynchronous:** task scheduling process is fully asynchronous, such as asynchronous scheduling, asynchronous execution, asynchronous log, etc., which can effectively reduce the peak flow of intensive scheduling and theoretically support the operation of tasks of any duration.

## 3. Quick use

### 1. Project import

The framework does not depend on the Spring container environment and persistence layer frameworks such as MyBatis. You can import it into your project as a Maven module, which you can download from the code cloud: https://gitee.com/hyxl-520/auto-job.git, you can use maven to import:

```xml
<dependency>
  <groupId>io.github.hyxl520</groupId>
  <artifactId>auto-job-framework</artifactId>
  <version>0.9.6</version>
</dependency>
```

The project is divided into two modules: auto-job-framework and auto-job-spring. The former is the core part of the framework, and the latter is the use of integration with Spring, which may be used to develop related consoles based on Spring web.

### 2. Project configuration

Project configuration mainly includes framework configuration and data source configuration. The framework configuration reads the and `auto-job.properties` files under `auto-job.yml` the classpath by default. See “All Configurations” for specific configuration items; Data source configuration. The framework uses Druid as the connection pool by default. You only need to configure the data source in the `druid.properties` file. Of course, you can customize the data source. The specific method is in `AutoJobBootstrap`. Related table-building scripts can be found in the db directory. The framework uses the MySQL database by default, and theoretically supports other databases of the SQL standard.

### 3. Task development

#### 3.1. Annotation-based

Developing an annotation-based task is very simple, and you only need to care about your business, except for the log output, which uses the built-in log auxiliary class `AutoJobLogHelper` of the framework. Of course, `AutoJobLogHelper` it’s almost indistinguishable from slf4j in that it provides four levels of log output: debug, info, warn, error, and you can use `AutoJobLogHelper` to proxy your slf4j. In this way, the logs output during the execution of these tasks will be output directly using slf4j. Here is a simple demonstration:


```java
 @AutoJob(attributes = "{'我爱你，心连心',12.5,12,true}", cronExpression = "5/7 * * * * ?", id = 2, alias = "test task")
    public void formatAttributes(String string, Double decimal, Integer num, Boolean flag) {
        AutoJobLogHelper logger=new AutoJobLogHelper();
        //log is object of org.slf4j.Logger，proxy it
        logger.setSlf4jProxy(log);
        logger.info("string={}", string);
        logger.warn("decimal={}", decimal);
        logger.debug("num={}", num);
        logger.error("flag={}", flag);
    }
```

 `@AutoJob` Annotate the task you are developing, configure something, and the task is developed. `@AutoJob` It is used to identify that a method is an AutoJob task. Of course, there are other annotations, which will not be described here. Careful students will find that this task has parameters. Yes, the AutoJob framework supports parameters. The configuration of more parameters will be explained in detail later.

#### 3.2. Based on construction

Manually creating tasks is more flexible than creating annotations. The framework provides builder objects for creating tasks, such as `AutoJobMethodTaskBuilder` and `AutoJobScriptTaskBuilder` objects. The former is used to build method tasks, and the latter is used to build script tasks.


```java
MethodTask task = new AutoJobMethodTaskBuilder(Jobs.class, "hello") 
          .setTaskId(IdGenerator.getNextIdAsLong())
          .setTaskAlias("测试任务") //alias
    	  .setParams("{'我爱你，心连心',12.5,12,true}") //attributes
          .setTaskType(AutoJobTask.TaskType.MEMORY_TASk)
          .setMethodObjectFactory(new DefaultMethodObjectFactory())
          .addACronExpressionTrigger("* 5 7 * * * ?", -1) //add a cron-like trigger
          .build();

AutoJobApplication
         .getInstance()
         .getMemoryTaskAPI() //get global api
         .registerTask(new AutoJobMethodTaskAttributes(task)); //register it
```

#### 3.3. Based on Functional Interface

In actual development, our business logic is not a scheduled task, but we hope to be able to use some functions provided by AutoJob, such as failure retry, long interruption, and log DB storage. Before 0.9.6, the tasks of AutoJob were only method type tasks and script type tasks. It was very troublesome to encapsulate each piece of business logic into a method. Therefore, 0.9.6 provides `FunctionTask` support for writing tasks to be managed by AutoJob at runtime, as shown in the following example:


```java
@AutoJobScan("com.jingge.spring")
public class Server {
    public static void main(String[] args) {
        new AutoJobBootstrap(Server.class, args)
                .withAutoScanProcessor()
                .build()
                .run();
        /*=================test=================>*/
        //create a FunctionTask，the content is lambda
        FunctionTask functionTask = new FunctionTask(context -> {
            context
                    .getLogHelper()
                    .info("test");
            //sleep 5 seconds
            SyncHelper.sleepQuietly(5, TimeUnit.SECONDS);
        });
        //use submit method to submit the task
        FunctionFuture future = functionTask.submit();
        //waitting 
        System.out.println("finished" + future.get());
        //use getLogs method to get logs
        functionTask
                .getLogs(3, TimeUnit.SECONDS)
                .forEach(System.out::println);
        System.out.println("log ouput finished");
        /*=======================Finished======================<*/
    }
}
```

### 4. Start the frame

Thanks to good design, the framework can be launched from any main method, as shown below for a launch


```java
import AutoJobProcessorScan;
import AutoJobScan;
import com.example.autojob.skeleton.framework.boot.AutoJobLauncherBuilder;

@AutoJobScan("com.example.autojob.job")
@AutoJobProcessorScan("com.example.autojob")
public class AutoJobMainApplication {
    public static void main(String[] args) {
        new AutoJobBootstrap(AutoJobSpringApplication.class)
                .withAutoScanProcessor()
                .build()
                .run();
        System.out.println("==================================>AutoJob application start success");
    }
}
```

Line 5 is used to configure the classpath of task scanning, which supports sub-package scanning. When it is not configured, the whole project will be scanned, which takes a long time.

Line 6 is processor scanning. The processor mainly performs some processing before and after the framework is started. The default is to scan the entire project. Note that this annotation can only take effect if withAutoScanProcessor is set. For example, in line 10 of the code, the framework’s own processor is automatically loaded and does not need to be configured.

Lines 9-12 are the startup code for the framework, `AutoJobBootstrap` which is the application bootstrap builder, through which you can add many custom configurations. After line 11, the AutoJob application is created, and line 12 calls the run method to start the entire application.

### 5. Dynamic modification

The framework itself is not a Web application and does not provide a corresponding modified Rest interface, but the framework provides many APIs for operating tasks, which you can find in `AutoJobAPI` and `AutoJobLogAPI`. You can refer to the example provided in the auto-job-spring module to develop the corresponding Rest interface. With the change of version, autojob will support the console in the future.

## IV. Environment Variables

In actual development, an application generally has different environments, such as a test environment and a production environment, and different environments use different data sources. In order to adapt to the above scenarios, AutoJob has supported environment variables since 0.9.4, and the main purpose of introduction is to adapt to data sources in different scenarios. Uch as what the current environment variable would read `druid-dev.proterties` if it were `dev`. The default is `AUTO_JOB_ENV`, of course, you can customize the environment variable KEYNAME, as follows:


```java
public static void main(String[] args) {
        new AutoJobBootstrap(AutoJobRunner.class)
            	//如果设置的env格式是"${name}"的格式则会去读取key=name的值作为环境变量，反之给定的字符串就认定是环境变量，如.setEnv("dev")，则dev就是环境变量。
                .setEnv("${env}")
                .build()
                .run();
}
```

## V. Types of tasks

### Classified by function

Tasks can be divided into method tasks and script tasks according to their functions.

The method type task corresponds to a method in Java. The method can have a return value and parameters. For the injection of parameters, see “Task Parameters”. Log output inside a method must be output using `AutoJobLogHelper`, or the log may not be saved.

Scripted tasks correspond to a script file on disk or a cmd command. See the section “Advanced Usage-Script Tasks” for details.

### Classified according to dispatching mode

Tasks can be divided into memory tasks and DB tasks according to the scheduling mode.

The life cycle of memory tasks is completed in memory, which has the characteristics of rapid scheduling, lock-free and real-time, and is suitable for short cycle, limited times and temporary tasks.

DB jobs will be saved to the database, and the database related status will be updated every time they are scheduled. Optimistic lock is adopted for DB type tasks, which can be executed only after obtaining the lock before each execution. It has the characteristics of long-term, easy maintenance and modification, and is suitable for tasks that will be used in a long time, such as regular data synchronization and timing cache preheating.

## VI. Task parameters

**Methodological tasks**

Method type task supports two parameter formats, one is FULL-type parameter, and the other is SIMPLE-type parameter. The specific differences are as follows:


```java
void exampleMethod1(String str, Integer num, Double decimal, Boolean flag);

void exampleMethod2(String str, Integer num, Double decimal, Boolean flag, Long count, Param param);

class param{
    private int id;
    private String num;
    //...
}
```

As in the above method: `exampleMethod1`, use the SIMPLE parameter:


```java
MethodTask task = new AutoJobMethodTaskBuilder(Jobs.class, "hello") 
          .setTaskId(IdGenerator.getNextIdAsLong())
          .setTaskAlias("test task")
    	  .setParams("{'我是字符串参数',12,12.5,true}")
          .setTaskType(AutoJobTask.TaskType.MEMORY_TASk)
          .setMethodObjectFactory(new DefaultMethodObjectFactory()) 
    	  .build();
//{'我是字符串参数',12,12.5,true}
```

Use a FULL-type parameter


```java
MethodTask task = new AutoJobMethodTaskBuilder(Jobs.class, "hello")
                .setTaskId(IdGenerator.getNextIdAsLong())
                .setTaskAlias("test task")
                .setParams("[{\"values\":{\"value\":\"字符串参数\"},\"type\":\"string\"},{\"values\":{\"value\":12},\"type\":\"integer\"},{\"values\":{\"value\":12.5},\"type\":\"decimal\"},{\"values\":{\"value\":false},\"type\":\"boolean\"}]")
                .setTaskType(AutoJobTask.TaskType.MEMORY_TASk)
                .setMethodObjectFactory(new DefaultMethodObjectFactory())
                .build();

/*
[
  {
    "values": {
      "value": "string attributes"
    },
    "type": "string"
  },
  {
    "values": {
      "value": 12
    },
    "type": "integer"
  },
  {
    "values": {
      "value": 12.5
    },
    "type": "decimal"
  },
  {
    "values": {
      "value": false
    },
    "type": "boolean"
  }
]
*/
```

We can see that the SIMPLE parameter is very simple `"{a1,a2,a3,...}"`, the parameter expression itself is a string, wrapped in large quotation marks, and the order of the parameters is matched from left to right. The SIMPLE parameter supports four types of parameters

 `'字符串参数'`, single quotation mark package, corresponding type `String`;

 `12` : Integer type parameter, corresponding type: `Integer` packaging type. If the value exceeds the shaping range, the type will be automatically matched `Long`.

 `12.5` : Decimal parameter, corresponding type: `Double` packaging type.

 `true|false` : Boolean parameter, corresponding type: `Boolean` package type.

The FULL type parameter is much more complicated than the FULL type parameter. It is a JSON array string. Each JSON object represents a parameter. Each object has two attributes, type and value. Literally, the type and value. The FULL type supports object type in addition to the four SIMPLE type parameters. Object-based parameters use JSON for serialization and deserialization. Because the FULL-type parameter is too complex, the framework provides an `AttributesBuilder` object that can generate the FULL-type parameter very simply. For `exampleMethod2` example:


```java
Param param = new Param();
        param.setId(1);
        param.setNum("12");
System.out.println(new AttributesBuilder()
        .addParams(AttributesBuilder.AttributesType.STRING, "string attributes")
        .addParams(AttributesBuilder.AttributesType.INTEGER, 12)
        .addParams(AttributesBuilder.AttributesType.DECIMAL, 12.5)
        .addParams(AttributesBuilder.AttributesType.BOOLEAN, false)
        .addParams(Param.class, param)
        .getAttributesString());
/*
[
  {
    "values": {
      "value": "字符串参数"
    },
    "type": "string"
  },
  {
    "values": {
      "value": 12
    },
    "type": "integer"
  },
  {
    "values": {
      "value": 12.5
    },
    "type": "decimal"
  },
  {
    "values": {
      "value": false
    },
    "type": "boolean"
  },
  {
    "values": {
      "id": 1,
      "num": "12"
    },
    "type": "com.example.autojob.job.Param"
  }
]
*/
```

In general, we prefer to use SIMPLE-type parameters for annotation-based task development, which are simple and clear; we prefer FULL-type parameters for construction-based task development, which are rich in types.

**Scripted tasks**

The parameters of the script type task are given through the start command, such as `python/script.test.py -a 12 -b`, where `-a 12` and `-b` are two parameters, so the script type task only supports string type parameters.

## VII. Task Running Object Factory

The task running object factory is an attribute of the method type task, because the method type task corresponds to a method in a Java class, so the execution of the method may depend on the context of the object instance, especially when the framework is integrated with Spring, it is likely to use the Bean in the Spring container. Therefore, you can specify the factory that creates the object that the method depends on: `IMethodObjectFactory`. By default, the framework uses the class’s no-argument constructor to create the object instance. Of course, you can create a custom factory:.


```java
public class SpringMethodObjectFactory implements IMethodObjectFactory {
    public Object createMethodObject(Class<?> methodClass) {
        // SpringUtil持有Spring的容器，获取Spring容器中的Bean
        return SpringUtil.getBean(JobBean.class);
    }
}
```

So how to make our task running object factory work, as shown in the following column:


```java
// 基于注解的任务开发只需要指定methodObjectFactory属性即可，框架将会调用指定工厂的无参构造方法创建一个工厂实例
@AutoJob
            (
                    id = 1
                    , attributes = "{'hello autoJob'}"
                    , defaultStartTime = StartTime.NOW
                    , repeatTimes = -1, cycle = 5
                    , methodObjectFactory = SpringMethodObjectFactory.class
            )
public void hello(String str) {
    logHelper.info(str);
}

//基于构建的任务开发时将工厂实例配置进去即可
public static void main(String[] args) {
    MethodTask methodTask = new AutoJobMethodTaskBuilder(Jobs.class, "hello")
            .setMethodObjectFactory(new SpringMethodObjectFactory())
            .build();
    AutoJobApplication
         .getInstance()
         .getMemoryTaskAPI() //获取全局的内存任务的API
         .registerTask(new AutoJobMethodTaskAttributes(task)); //注册任务
}
```

## VIII. Task Log

As a task scheduling framework, a detailed log must be essential. The framework provides three types of logging: scheduling log, execution log, and run log

**Scheduling log**

Every time a task is started and completed, the task is scheduled. The scheduling log records the basic information, scheduling time, running status, execution duration, and task result of the scheduled task in detail (the method task corresponding to the task result is the return value, which is serialized by JSON, and the script task is the return value of the script). The scheduling log corresponds to the database table `aj_scheduling_record`, and its ID is associated with the running log and the execution log generated in this scheduling.

**Run the log**

The running log is the log of internal output during the task running, the method type task is the log of using `AutoJobLogHelper` output, and the script type task is the output of script or cmd command on the console. Run the database table corresponding to the log `aj_job_logs`.

**Execution log**

The execution log records the execution of a scheduled task, such as when to start, when to complete, whether to run successfully, task results, task exceptions, etc. The execution log corresponds to the library table `aj_run_logs`.

The task logs are updated in real time, and if you are using the framework’s default log retention strategy (database storage), you can get the logs through `AutoJobLogDBAPI`. Both the running log and the execution log are bound with the scheduling ID. The running log and the execution log generated by this scheduling can be found through the scheduling ID.

## IX. Task Running Context

AutoJob has supported the running context since 0.9.4, and the content related to the current task running can be obtained through the context inside the task. The following is a usage example column:


```java
public void hello(String str) {
        Random random = new Random();
        int flag = random.nextInt(100) + 1;
    	//获取当前的上下文
        AutoJobRunningContext context = AutoJobRunningContextHolder.currentTaskContext();
        AutoJobLogHelper logHelper = context.getLogHelper();
        logHelper.setSlf4jProxy(log);
        logHelper.info("参数：{}", str);
        logHelper.info("本次执行上下文参数：{}", flag);
        logHelper.info("当前调度记录ID：{}", context.getSchedulingRecordID());
        logHelper.info("当前任务ID：{}", context.getTaskId());
        logHelper.info("本次调度启动时间：{}", DateUtils.formatDateTime(new Date(context.getStartTime())));
        logHelper.info("当前任务类型：{}", context
                .getTaskType()
                .toString());
        //get current task running stack
        AutoJobRunningStack stack = context.getCurrentStack();
        logHelper.info("当前栈深：{}", stack.depth());
        //get running stack entry
        RunningStackEntry current = stack.currentStackEntry();
        //添加本次运行的一下上下文参数，以便本次执行失败重试能直接恢复
        current.addParam("runningPos", flag);
        //也可以获取上次执行的上下文参数
        Optional<RunningStackEntry> last = stack.lastStackEntry();
        last.ifPresent(stackEntry -> logHelper.info("上次执行上下文参数{}", stackEntry.getParam("runningPos")));
        last.ifPresent(stackEntry -> logHelper.info("上次是否执行成功：{}", stackEntry.isSuccess()));
        last.ifPresent(stackEntry -> logHelper.info("上次执行参数：{}", stackEntry.getArgs()));
        //获取当前任务具有的邮件客户端
        IMailClient mailClient = context.getMailClient();

        if (flag % 3 == 0) {
            throw new RuntimeException("模拟异常发生");
        }
}
```

It is recommended to turn on the stack trace. The task uses the context to keep the state of each run. If the exception causes the task to retry, it can continue to run at the location of the last run.

** Attention! It is recommended to set the maximum depth of the task stack at a reasonable value to avoid too deep stack occupying a large amount of memory, especially when the number of concurrent tasks is particularly large. See “XI. All Configurations- `autoJob.running.stackTrace` **” for configuration contents.

## X. Framework

<img src="https://gitee.com/hyxl-520/auto-job/raw/master/doc/%E6%9E%B6%E6%9E%84%E5%9B%BE-v0.9.1.jpg">

The components in the left part of the framework diagram are the core components of the framework.

**Task container module**

The task container module comprises a DB task container and a memory task container, which are respectively used for storing DB tasks and memory tasks.

**Scheduling module**

The scheduling module consists of a scheduler, a task scheduling queue, a register, a time wheel scheduler and a time wheel. The memory task scheduler `AutoJobMemoryTaskScheduler` and the DB task scheduler `AutoJobDBScheduler` are responsible for scheduling the task to be executed (< = 5 seconds) from the task container into the task scheduling queue cache `AutoJobTaskQueue`. The time round scheduler `AutoJobTimeWheelScheduler` schedules the tasks in the task scheduling queue through the register `AutoJobRegister` to enter the time round and prepare for execution. The time wheel is rolled by seconds, and the executed task is submitted to the task executor pool for execution. The scheduler `AutoJobRunSuccessScheduler` of successful operation executes relevant operations after successful operation, such as updating the status and the next trigger time. The scheduler `AutoJobRunErrorScheduler` of failed operation executes relevant operations after failed operation, such as updating the status, updating the trigger time according to the configured retry policy, and failover.

**Task Executor Pool Module**

The task executor pool contains two dynamic thread pools, namely, fast-pool and slow-pool. By default, the first execution of a task is submitted to the fast-pool, and the second execution will determine whether to downgrade according to the duration of the last execution. A dynamic thread pool is a thread pool that is dynamically adjusted according to traffic. For the specific configuration, see “X. All Configurations: Executor Pool Configuration”.

**Log module**

The log module and the core scheduling module are completely decoupled. The running log is generated and issued to the memory message queue when the task is executed. The log module monitors the message issuing event and takes out the message and puts it into the message buffer. The log processing thread alone stores the log regularly and quantitatively. Running logs are saved by listening to task events. Log modules are designed asynchronously to minimize the impact of log IO on scheduling.

In addition to the above core components, the framework also has some functional expansion components.

**Lifecycle processor**

The life cycle processor can also be understood as a life cycle hook. Specifically, it is the life cycle hook of a task. See the life cycle event diagram below for details.

<img src="https://gitee.com/hyxl-520/auto-job/raw/master/doc/%E7%94%9F%E5%91%BD%E5%91%A8%E6%9C%9F%E5%9B%BE.jpg">

To use a lifecycle hook is also very simple, here is a sample:


```java
//方式一（子事件处理器）
public class TaskBeforeRunHandle implements ITaskEventHandler<TaskBeforeRunEvent> {
    @Override
    public void doHandle(TaskBeforeRunEvent event) {
        System.out.println("任务：" + event
                .getTask()
                .getAlias() + "即将开始运行");
    }

    @Override
    public int getHandlerLevel() {
        return 0;
    }
}
```

The above column represents an output on the console before the task is executed: “Task: { task alias} is about to start running”. To implement an event handler, you only need to implement `ITaskEventHandler` the interface. The generic represents the event you need to handle. Of course, the same functions as those shown above can also be realized in the following manner


```java
//方式二（父事件处理器）
public class TaskBeforeRunHandle implements ITaskEventHandler<TaskEvent> {
    @Override
    public void doHandle(TaskEvent event) {
        if (event instanceof TaskBeforeRunEvent) {
            System.out.println("任务：" + event
                    .getTask()
                    .getAlias() + "即将开始运行");
        }
    }

    @Override
    public int getHandlerLevel() {
        //数字越大，级别越高
        return 0;
    }
}
```

 `TaskEvent` It is the parent class of all task events. When the processor of the parent class event is implemented, all task-related events will execute the processor. The event type can be determined to complete related operations. When a processor needs to process multiple event types, it can be used as above. Each event handler can be assigned a level through the override `getHandlerLevel` method. The higher the number, the higher the level, and the execution will take precedence. Parent Event Processor High > Parent Event Processor Low > Child Event Processor High > Child Event Processor Low. Of course, just declaring the handler does not take effect without adding it to the application. Here’s how to make the event handler take effect.


```java
public class TaskEventHandlerLoader implements IAutoJobLoader {
    @Override
    public void load() {
        //方式一（子事件处理器）
        TaskEventHandlerDelegate
                .getInstance()
                .addHandler(TaskBeforeRunEvent.class, new TaskBeforeRunHandle());
        
		//方式二（父事件处理器）
        TaskEventHandlerDelegate
                .getInstance()
                .addHandler(TaskEvent.class, new TaskBeforeRunHandle());
    }
}
//将启动处理器添加进应用上下文
public static void main(String[] args) {
        new AutoJobBootstrap(AutoJobMainApplication.class)
                .addProcessor(new TaskEventHandlerLoader()) //添加到上下文
                .build()
                .run();
}
```

The above code demonstrates how to add a processor to the context. In `AutoJob`, the processor that performs certain operations before the framework starts and before the framework shuts down is `Processor`, the processor that executes before the framework starts is `IAutoJobLoader`, and the processor that executes before the framework shuts down is `IAutoJobEnd`. Add the event handler to the “event delegator”: `TaskEventHandlerDelegate` via the launch handler, and then manually add the launch handler to the application context at application build time. Of course, if you `Processor` have a lot, you can automatically scan `Processor` through annotations `@AutoJobProcessorScan`, you can specify the package to be scanned, support sub-package scanning, and default to full project scanning when not specified. After the scan, the context is automatically injected after the instance is created by calling `Processor` the no-argument constructor. The columns are as follows:


```java
@AutoJobProcessorScan("com.example.autojob")
public class AutoJobMainApplication {
    public static void main(String[] args) {
        new AutoJobBootstrap(AutoJobMainApplication.class) //指定入口类
                .withAutoScanProcessor() //手动开启处理器自动扫描，默认是关闭的，以防全项目扫描耗时较长
                .build()
                .run();
        System.out.println("==================================>系统创建完成");
}
```

## XI. All configurations

The framework provides rich configurations, which are loaded from `auto-job.yml` or `auto-job.properties` files by default. Of course, you can dynamically load from the database to achieve dynamic configuration. All configurations are as follows:


```yaml
# Configuration content V0.9.6
autoJob:
  debug:
    # debug model
    enable: false
  context:
    # the size of scheduleing queue, if you have many tasks please use bigger numer.
    schedulingQueue:
      length: 1000
    # memory container configuration
    memoryContainer:
      length: 200
      # the finished memory tasks's clean strategy：CLEAN_FINISHED, KEEP_FINISHED-keep in the cache
      cleanStrategy: KEEP_FINISHED
    running:
      # open stackTracke
      stackTrace:
        enable: true
        depth: 16
  # annotation scan configuration
  annotation:
    # filter
    filter:
      enable: true
      classPattern: "**.**"
    enable: true
    defaultDelayTime: 30
  # database type，supported MySQL,PostgreSQL,Oracle,DamengSQL and so on.
  database:
    type: mysql
  # excutor configuration
  executor:
    fastPool:
      update:
        # enable auto update with flow
        enable: true
        allowTaskMaximumResponseTime: 1
        trafficUpdateCycle: 5
        # when the real response time over the value(percent),update the numer of thread.
        adjustedThreshold: 0.5
      coreThread:
        initial: 5
        min: 5
        max: 50
        keepAliveTime: 60
      maxThread:
        initial: 5
        min: 10
        max: 50
    slowPool:
      update:
        enable: true
        allowTaskMaximumResponseTime: 1
        trafficUpdateCycel: 5
        adjustedThreshold: 0.5
      coreThread:
        initial: 10
        min: 5
        max: 50
        keepAliveTime: 60
      maxThread:
        initial: 20
        min: 10
        max: 50
    relegation:
      # Degradation threshold (min)
      threshold: 3
  register:
    filter:
      enable: true
      classPath: "**.**"
  scheduler:
    finished:
      error:
        retry:
          # repeat strategy  LOCAL_RETRY,FAILOVER（useful when open cluster model）
          strategy: FAILOVER
          enable: true
          retryCount: 5
          interval: 0.5
  emailAlert:
    enable: false
    # email server type, only support SMTP
    serverType: SMTP
    interval: 5000
    auth:
      sender: "1158055613@qq.com"
      receiver: "XXXXXX@qq.com"
      token: "XXXXXX"
      # supported email type,QQMail,gMail(google email),163Mail,outLookMail,customize
      type: QQMail
      customize:
        customMailServerAddress:
        customMailServerPort:
    config:
      taskRunError: true
      taskRefuseHandle: true
      clusterOpenProtectedMode: true
      clusterCloseProtectedMode: true
  logging:
    # log's save strategy
    strategy:
      saveWhenBufferReachSize: 10
      saveWhenOverTime: 10
    scriptTask:
      encoding: GBK
  cluster:
    # open cluster model
    enable: true
    # binding tcp port
    port: 9501
    auth:
      enable: true
      # aes decipher
      publicKey: "autoJob!@#=123.#"
      token: "hello"
    client:
      # remote address
      remoteNodeAddress: "localhost:9502"
      pool:
        size: 10
        getTimeout: 1
        getDataTimeout: 3
        connectTimeout: 2
        keepAliveTimeout: 10
      allowMaxJetLag: 3000
      nodeSync:
        cycle: 10
        offLineThreshold: 3
    config:
      protectedMode:
        enable: true
        threshold: 0.3
```

Of course, not all of the above configurations need to be configured by you. Basically, all configurations of the framework are set with default values, which can ensure the scheduling in regular scenarios. The configuration of AutoJob is not only given through the local configuration file, but also introduced through the external input stream.

Since Version 0.9.3, AutoJob supports nested configuration, that is, you can use ${ key } instead of value in the configuration file, and the key can be specified through VMOptions or Program ar guments to protect system security, such as database-related configuration. Uch as the following database configuration


```properties
driverClassName=com.mysql.cj.jdbc.Driver
# 端口使用插值替代
url=jdbc:mysql://localhost:${db.port}/auto_job_plus?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior\
  =convertToNull&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true

username=root
# 密码使用插值替代
password=${db.password}
connectionProperties=config.decrypt=true;config.decrypt.key=MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAK7DIr13aXSDUzKJqC33a/uRwdxKQTn/tDDX0yW0FgY4rvMOFzYsoXEArFUjOQFivv3GUirY6z0jXUOVTmJpq28CAwEAAQ==
#初始化个数
initialSize=5
#最大连接数
maxActive=30
#等待时间，毫秒
maxWait=10000
#最少连接数
minIdle=3
validationQuery=SELECT 1 FROM DUAL
testWhileIdle=true
testOnBorrow=false
testOnReturn=false
filters=config
```

The port and password of DB are injected by configuring VMOptions at startup to avoid password disclosure caused by configuration file disclosure. AutoJob also supports environment variables `auto.job.env` since 0.9.3. Currently, environment variables are mainly used to read database configuration files. For example, if an environment variable `-Dauto.job.env=dev` is specified, the relevant database configuration will be read `druid-dev.peroperties`.

## XII. Advanced Usage

### 1. Dynamic fragmentation <span style="color:#FFFE91;"> (new)</span>

When the cluster mode is enabled, AutoJob supports task fragmentation and multi-machine operation. Here are two examples:


```java
@ScriptJob
(
    versionID = 2, 
    value = "ping {}", 
    taskType = AutoJobTask.TaskType.DB_TASK, 
    saveStrategy = SaveStrategy.UPDATE, 
    //分片配置，启用分片，启用分片故障重试，总分片数为12
    shardingConfig = @ShardingConfig(enable = true, enableShardingRetry = true, total = 12)
)
    public ScriptJobConfig scriptJob() {//七秒后启动任务，执行ping www.baidu.com命令
        return ScriptJobConfig
                .builder()
                .addASimpleTrigger(System.currentTimeMillis() + 7000, -1, 7, TimeUnit.SECONDS)
                .addValue("www.baidu.com")
                .build();
    }

    @AutoJob
(
    id = 1, 
    attributes = "{'hello'}", 
    asType = AutoJobTask.TaskType.MEMORY_TASk, 
    //启用分片，默认不允许分片执行异常重试，总分片数23
    shardingConfig = @ShardingConfig(enable = true, total = 23)
)
    public void hello(String str) {
        //获取当前的上下文
        Random random = new Random();
        int flag = random.nextInt(100) + 1;
        AutoJobRunningContext context = AutoJobRunningContextHolder.currentTaskContext();
        AutoJobLogHelper logHelper = context.getLogHelper();
        logHelper.setSlf4jProxy(log);
        logHelper.info("参数：{}", str);
        //运行上下文可以获取到总分片和当前分片
        logHelper.info("总分片：{}", context.getShardingTotal());
        logHelper.info("当前分片：{}", context.getCurrentSharding());
        logHelper.info("本次执行上下文参数：{}", flag);
        logHelper.info("当前调度记录ID：{}", context.getSchedulingRecordID());
        logHelper.info("当前任务ID：{}", context.getTaskId());
        logHelper.info("本次调度启动时间：{}", DateUtils.formatDateTime(new Date(context.getStartTime())));
        logHelper.info("当前任务类型：{}", context
                .getTaskType()
                .toString());
        //获取当前任务运行栈
        AutoJobRunningStack stack = context.getCurrentStack();
        logHelper.info("当前栈深：{}", stack.depth());
        //获取本次运行的栈帧
        RunningStackEntry current = stack.currentStackEntry();
        //添加本次运行的一下上下文参数，以便本次执行失败重试能直接恢复
        current.addParam("runningPos", flag);
        //也可以获取上次执行的上下文参数
        Optional<RunningStackEntry> last = stack.lastStackEntry();
        last.ifPresent(stackEntry -> logHelper.info("上次执行上下文参数{}", stackEntry.getParam("runningPos")));
        last.ifPresent(stackEntry -> logHelper.info("上次是否执行成功：{}", stackEntry.isSuccess()));
        last.ifPresent(stackEntry -> logHelper.info("上次执行参数：{}", stackEntry.getArgs()));
        //获取当前任务具有的邮件客户端
        IMailClient mailClient = context.getMailClient();

        if (flag % 3 == 0 && flag > 53) {
            throw new RuntimeException("模拟异常发生");
        }
        SyncHelper.sleepQuietly(5, TimeUnit.SECONDS);
    }
```

Annotation-based shard configuration is demonstrated above, including method tasks and script tasks. The shards of method tasks can be obtained from `AutoJobRunningContext`, and the script tasks are given through `-total` and `-current`.

The fragmentation is created by a node and broadcasted to each node in the cluster, each node directly executes the fragmentation after receiving the fragmentation, if the fragmentation execution is abnormal and the fragmentation fault retry is allowed to be started, the fragmentation will be retried at the execution node, the node broadcasting the fragmentation will not sense the execution of each fragmentation, and the execution of the fragmentation is in the charge of the receiving node. If an exception occurs during the broadcast of the fragment, such as node downtime, network exception, etc., the fragment will be fused to the current node.

By default, the fragmentation strategy is based on integer fragmentation. Of course, you can customize the fragmentation strategy:


```java
public class YourShardingStrategy implements ShardingStrategy {
    @Override
    public boolean isAvailable(AutoJobShardingConfig config) {
        return config != null && config.isEnable() && config.getTotal() != null;
    }

    @Override
    public Map<ClusterNode, Object> executionSharding(AutoJobShardingConfig config, List<ClusterNode> clusterNodes) {
        //分片逻辑
    }
}

//可以在基于构建的方式创建任务过程中配置
new AutoJobMethodTaskBuilder(Job.class,"hello")
	//...
	.setShardingStrategy(new YourShardingStrategy())
	build();

//也可以在`TemplateJob`中定义
@TemplateAutoJob
public class TemplateJob extends AutoJobTemplate {
    @Override
    public Object run(Object[] params, AutoJobRunningContext context) throws Exception {
        context.getLogHelper().info("测试一下");
        return null;
    }

    @Override
    public Object[] params() {
        return new Object[0];
    }

    @Override
    public String cron() {
        return "0 0 7 * * ?";
    }

    @Override
    public ShardingStrategy shardingStrategy() {
        return new NumericalShardingStrategy();
    }

    @Override
    public AutoJobShardingConfig shardingConfig() {
        return new AutoJobShardingConfig(true, 24, true);
    }
}
```

### 2. Failover <span style="color:#FFFE91;"> (new)</span>

The task supports failover when Cluster-Mode is enabled, and the global retry configuration is as follows:


```yaml
autoJob:
    scheduler:
        finished:
          error:
            retry:
              # 重试策略 LOCAL_RETRY-本机重试 FAILOVER-故障转移（开启集群时有效）
              strategy: FAILOVER
              enable: true
              retryCount: 5
              interval: 0.5
```

It can also be configured separately when creating a task based on a build:


```java
new AutoJobMethodTaskBuilder(Jobs.class, "hello")
    		    //...
                .setRetryConfig(new AutoJobRetryConfig(true, RetryStrategy.FAILOVER, 3, 3))
                .build();
```

### 3. Script task

The framework supports scripting tasks, supports Python, Shell, PHP, NodeJs and PowerShell natively, and provides other script type extensions. The object corresponding to the script task is `ScriptTask`. The script is saved on the disk as a script file on the server. It is very simple to build a script task. The framework provides `AutoJobScriptTaskBuilder` assistance to build a complete script task. Here are a few examples:


```java
		ScriptTask task = new AutoJobScriptTaskBuilder()
                .setTaskId(IdGenerator.getNextIdAsLong()) //设置任务ID，任务ID作为区分任务的键，不指定时将会随机分配
                .setTaskAlias("测试脚本任务1") //任务别名
                .setTaskType(AutoJobTask.TaskType.MEMORY_TASk) //任务类型，有内存型任务和DB型任务，内存型任务的所有生命周期都在内存完成，除了日志外不会保留到数据库
                .setBelongTo(1L) //保留拓展字段，用于说明该任务所属
                .addACronExpressionTrigger("* 15 7 * * * ?", -1) //添加一个cron-like触发器，两个参数分别是：cron-like表达式、重复次数。不指定触发器时将会在默认延迟后执行一次，-1表示该任务为永久执行，如果只需执行n次，重复次数为n-1
                .createNewWithContent(ScriptType.PYTHON, "print('hello auto-job')"); // 使用脚本类型和脚本内容构建一个脚本任务对象

        ScriptTask task1 = new AutoJobScriptTaskBuilder()
                .setTaskId(IdGenerator.getNextIdAsLong())
                .setTaskAlias("测试脚本任务2")
                .setTaskType(AutoJobTask.TaskType.MEMORY_TASk)
                .setBelongTo(1L)
                .addASimpleTrigger(SystemClock.now(), 3, 10, TimeUnit.SECONDS) //添加一个简单触发器，四个参数分别是：启动时间、重复次数、周期、周期时间单位，该触发器表示立即执行，并且重复执行三次，总共执行四次，周期为10秒
                .createNew("python", "/script", "test", "py"); // 使用给定路径的脚本文件创建一个脚本任务，四个参数分别是：启动命令、脚本路径、脚本文件名、脚本后缀，该方法能够创建除框架原生脚本类型以外的脚本任务

        ScriptTask task2 = new AutoJobScriptTaskBuilder()
                .setTaskId(IdGenerator.getNextIdAsLong())
                .setTaskAlias("测试脚本任务3")
                .setTaskType(AutoJobTask.TaskType.MEMORY_TASk)
                .setBelongTo(1L)
                .addAChildTaskTrigger()  // 添加一个子任务触发器，该任务不会自动触发，只有当有任务主动关联该任务作为其子任务且父任务完成一次调度时才会触发该任务
                .createNewWithCmd("ping www.baidu.com"); // 创建一个cmd脚本任务

        ScriptTask task3 = new AutoJobScriptTaskBuilder()
                .setTaskId(IdGenerator.getNextIdAsLong())
                .setTaskAlias("测试脚本任务4")
                .setTaskType(AutoJobTask.TaskType.MEMORY_TASk)
                .setBelongTo(1L)
                .addADelayTrigger(3, TimeUnit.MINUTES) // 添加一个延迟触发器，任务将在给定延迟后自动触发一次，默认使用该类型触发器，延迟时间可以在框架配置中配置
                .createNewWithExistScriptFile(ScriptType.PYTHON, "/script", "test"); // 使用已存在的脚本创建一个脚本任务，三个参数分别是：脚本类型、脚本路径、脚本文件名
```

In addition to demonstrating how to create a script task, the above example also introduces triggers. The framework provides four types of triggers, namely, the cron-like trigger, the simple trigger, the parent-child task trigger, and the delay trigger. The introduction of the specific triggers is basically explained in the above code notes, and will not be described here.

** Script tasks have supported annotation creation since version 0.9.6. For details, see 2. Advanced Application of Annotation Task Development- `@ScriptJob` ** `

### 4. Advanced application of annotation task development

The use of annotations `@AutoJob` is briefly demonstrated in Chapter 3-Subsection 3-Based on Annotations. The AutoJob framework also provides other annotations, such as `@FactoryAutoJob`, `@Conditional`, etc., which are explained below.

####  `@AutoJob` Annotation 

 `@Autojob` Annotation is the most frequently used annotation in the framework. When it is annotated on a method and the scheduling information is configured, the method will wrap it into a method type task and put it into the corresponding task container when the application is started. You can refer to the following list.


```java
@Slf4j
public class Jobs {
    private static final AutoJobLogHelper logHelper = new AutoJobLogHelper();

    static {
        logHelper.setSlf4jProxy(log);
    }

    //立即启动，重复无限次，周期为5秒，使用自定义方法运行对象工厂，参数为"hello autoJob"
    @AutoJob(id = 1, attributes = "{'hello autoJob'}", defaultStartTime = StartTime.NOW, repeatTimes = -1, cycle = 5, methodObjectFactory = SpringMethodObjectFactory.class)
    public void hello(String str) {
        logHelper.info(str);
    }

    //2022-11-21 12:00:00启动，重复3次，总共执行4次，周期为10秒，作为DB任务调度，最长允许运行时长5秒
   @AutoJob(id = 2, startTime = "2022-11-21 12:00:00", repeatTimes = 3, cycle = 10, asType = AutoJobTask.TaskType.DB_TASK, maximumExecutionTime = 5000)
    public void longTask() {
        logHelper.info("long task start");
        SyncHelper.sleepQuietly(10, TimeUnit.SECONDS);
        logHelper.info("long task end");
    }

    //作为子任务调度
    @AutoJob(id = 3, schedulingStrategy = SchedulingStrategy.AS_CHILD_TASK)
    public void childTask() {
        logHelper.info("child task start");
        SyncHelper.sleepQuietly(3, TimeUnit.SECONDS);
        logHelper.info("child task end");
    }

    //按照cron like表达式调度，重复无限次，子任务为3
    @AutoJob(id = 4, alias = "获取随机字符串", cronExpression = "* * 0/5 17 * * ?", repeatTimes = -1, childTasksId = "3")
    public String getRandomString() {
        return StringUtils.getRandomStr(16);
    }
    
    //仅保存到数据库
    @AutoJob(id = 4, schedulingStrategy = SchedulingStrategy.ONLY_SAVE)
    public void error() {
        String str = null;
        str.length();
    }	
}

```

####  `@FactoryAutoJob` Annotation 

Because `@AutoJob` the configuration is fixed, you may want to be able to dynamically configure some attributes of the task, so `@FactoryAutoJob` in order to solve this kind of scenario, of course, you can also use the build-based approach to develop the task to achieve dynamic. Here is an example:


```java
@FactoryAutoJob(RandomStringMethodFactory.class)
public String getRandomString() {
    return StringUtils.getRandomStr(16);
}

public class RandomStringMethodFactory implements IMethodTaskFactory {
    @Override
    public MethodTask newTask(AutoJobConfigHolder configHolder, Method method) {
        return new AutoJobMethodTaskBuilder(method.getDeclaringClass(), method.getName())
                .setTaskId(IdGenerator.getNextIdAsLong())
            	//...
                .build();
    }
}
```

As shown above, `getRandomString` the packaging will be performed by `RandomStringMethodFactory`.

####  `@Conditional` Annotation 

It is believed that people who often use Spring should be familiar with this annotation. In Spring, this annotation is used to implement conditional injection, that is, when the condition is met, the Bean will be injected into the container. In AutoJob, the functionality is similar, and only methods that meet the criteria specified by the annotation can be wrapped as a task.

####  `@TemplateAutoJob` Annotation 

Based on `@AutoJob` the rigid development task, it is impossible to specify some parameters flexibly and dynamically. Based on the flexible construction and `@FactoryAutoJob` development task, it needs to configure a large number of parameters, which is more troublesome. Therefore, AutoJob has been introduced `@TemplateAutoJob` since 0.9.4: To implement a template task, you only need to specify a class inheritance `AutoJobTemplate`, as shown below:


```java
//模板任务上加上该注解，启动时将会被自动扫描注册
@TemplateAutoJob
public class TemplateJob extends AutoJobTemplate {
    /**
     * 任务的主要内容，子类必须实现该方法
     *
     * @param params  任务执行的参数，通过方法{@link #params()}返回
     * @param context 任务执行上下文
     * @return java.lang.Object
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/3/29 10:51
     */
    @Override
    public Object run(Object[] params, AutoJobRunningContext context) throws InterruptedException {
        context
                .getLogHelper()
                .info("你好呀");
        SyncHelper.sleep(5, TimeUnit.SECONDS);
        return "hello template job" + params[0];
    }
    
    /**
     * 有时该任务的一些行为需要通过配置文件给出，此时子类可以覆盖该方法返回类路径下的配置文件路径，模板将会创建一个{@link PropertiesHolder}对象
     *
     * @return java.lang.String
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/3/29 10:56
     */
    @Override
    public String withProfile() {
        return "template-job-config.properties";
    }

    /**
     * 有时配置不来自于配置文件，此时可以通过输入流导入
     *
     * @return java.util.List<java.io.InputStream>
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/3/29 17:55
     */
    @Override
    public List<InputStream> withProfilesInputStream() {
        return super.withProfilesInputStream();
    }

    /**
     * 子类可以控制任务执行与否，这里通过从配置文件读取
     *
     * @return boolean
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/3/29 10:57
     */
    @Override
    public boolean enable() {
        return propertiesHolder.getProperty("enable.job", Boolean.class, "true");
    }

    /**
     * 任务的参数，子类必须实现该方法
     *
     * @return java.lang.Object[]
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/3/29 17:49
     */
    @Override
    public Object[] params() {
        return new Object[]{"你好"};
    }

    /**
     * cron like表达式，子类必须实现该方法
     *
     * @return java.lang.String
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/3/29 17:49
     */
    @Override
    public String cron() {
        return "30 27 10 * * ?";
    }

    /**
     * 任务类型，子类可以选择性覆盖实现，默认是Memory方法
     *
     * @return AutoJobTask.TaskType
     * @author JingGe(* ^ ▽ ^ *)
     * @date 2023/3/29 17:50
     */
    @Override
    public AutoJobTask.TaskType taskType() {
        return AutoJobTask.TaskType.DB_TASK;
    }
}
```

Subclasses must implement `run` the, `cron` and `params` methods, and other methods can be selectively overridden as needed to support the current scenario. Template tasks are both `@AutoJob` convenient and `@FactoryAutoJob` flexible based on the construction method. Templates provide template functions such as task configuration reading, task startup and shutdown control, and task interrupt logic. However, when the task volume increases, the number of classes will also increase significantly.

#### `@ScriptJob` <span style="color:#FFFE91;"> (New)</span>

In the original version, if you want to create a script task, you can only use `AutoJobScriptTaskBuilder` to create it, and you need to configure a large number of parameters, which is very troublesome. Therefore, version 0.9.6 introduces `@ScriptJob` annotations to realize the declarative creation of script tasks. The specific usage is as follows:


```java
//可以配置任务的versionID（即原来的annotationID），任务类型，命令行模板，子任务，保存策略等
@ScriptJob(versionID = 2, value = "ping {}", taskType = AutoJobTask.TaskType.DB_TASK, childTasksId = "1", saveStrategy = SaveStrategy.UPDATE)
public ScriptJobConfig scriptJob() {//方法必须返回ScriptJobConfig对象，对脚本任务进一步配置
     return ScriptJobConfig
             .builder()
             .addASimpleTrigger(System.currentTimeMillis() + 7000, -1, 7, TimeUnit.SECONDS)
         	 //添加插值的覆盖，给出顺序和'{}'的顺序一致
             .addValue("www.baidu.com")
             .build();
 }
```

The command line template uses { } as an interpolation, similar to the way slf4j logs output, based on which you can implement a dynamic command line.

### 5. Use the built-in RPC framework

The goal of AutoJob is a distributed task scheduling framework, so we have developed a communication framework internally: RPC, which is only briefly introduced here. Each AutoJob has a server and a client. The server can be opened in the configuration `cluster.enable=true` file. To use the RPC framework, you first need to develop a service provider class, such as the API that comes with the framework:


```java
@AutoJobRPCService("MemoryTaskAPI") //通过该注解声明该类是一个RPC服务提供方
@Slf4j
public class MemoryTaskAPI implements AutoJobAPI {
    //...细节省略
    @Override
    @RPCMethod("count") //声明该方法对外提供的方法名
    public int count() {
        //...
    }
}
```

How other AutoJob nodes call this service is also very simple, as shown in the following column:


```java
@AutoJobRPCClient("MemoryTaskAPI") //声明该接口是一个RPC客户端
public class MemoryTaskAPIClient{
    //方法名同服务对外提供方法名相同
    int count();
}

RPCClientProxy<MemoryTaskAPIClient> proxy = new RPCClientProxy<>("localhost", 7777, MemoryTaskAPIClient.class); //创建接口代理
MemoryTaskAPIClient client = proxy.clientProxy(); //获取代理实例
System.out.println(client.count()); //像本地方法一样使用
```

The embedded RPC framework is based on netty and uses ProtoStuff for serialization and deserialization. Currently RPC is for learning only.

### 6. Use thread pool encapsulation based on dynamic adjustment of time.

The execution pool `AutoJobTaskExecutorPool` of the framework is where tasks are executed, which contains a fast pool and a slow pool for executing tasks with short and long running times, respectively. Framework task execution uses two thread pools that are dynamically updated based on traffic `FlowThreadPoolExecutorHelper`. In order to better meet business needs, a thread pool that is dynamically adjusted based on time is provided `TimerThreadPoolExecutorPool`.


```java
TimerThreadPoolExecutorHelper.TimerEntry entry = new TimerThreadPoolExecutorHelper.TimerEntry("0 0 7 * * ?", 10, 20, 60, TimeUnit.SECONDS);//配置调整项，<0的项不作调整
		//添加一个触发监听器
        entry.setTriggerListener((cronExpression, threadPoolExecutor) -> {
            System.out.println("日间线程池调整");
        });
        TimerThreadPoolExecutorHelper fastPool = TimerThreadPoolExecutorHelper
                .builder()
                .setInitialCoreTreadCount(3)
                .setInitialMaximizeTreadCount(5)
                .setTaskQueueCapacity(100)
                .addTimerEntry("0 0 22 * * ?", 0, 1, -1, null)
                .addTimerEntry(entry)
                .build();
        new AutoJobBootstrap(AutoJobSpringApplication.class)
                .withAutoScanProcessor()
            	//自定义执行池
                .setExecutorPool(new AutoJobTaskExecutorPool(null, fastPool, FlowThreadPoolExecutorHelper
                        .builder()
                        .build()))
                .build()
                .run();
        System.out.println("==================================>AutoJob应用已启动完成");
```

As shown above, the fast pool uses the thread pool encapsulation based on dynamic adjustment of time, which will expand the thread pool to core 10 threads at 7:00 a.m. every day, with a maximum of 20 threads. The core idle time is updated to 60 seconds. At 10:00 p.m. every night, the thread pool is reduced to core 0 threads, with a maximum of 1 threads, and a trigger listener is added. The slow pool uses thread pool encapsulation based on flow adjustment.

### 7. Designated execution machine

Sometimes our AutoJob is deployed on multiple different services, but the same database is used. When running a method task, it may be captured by other services without this method, resulting in execution failure. Therefore, it can be solved by specifying the execution machine, as shown below:


```java
@AutoJob(id = 1, defaultStartTime = StartTime.NOW, attributes = "{'hello'}", asType = AutoJobTask.TaskType.DB_TASK, executableMachines = "192.168.10.31")//通过注解指定，ip为192.168.10.31的机器执行
    public void hello(String str) {
        //获取当前的上下文
        Random random = new Random();
        int flag = random.nextInt(100) + 1;
        AutoJobRunningContext context = AutoJobRunningContextHolder.currentTaskContext();
        AutoJobLogHelper logHelper = context.getLogHelper();
        logHelper.setSlf4jProxy(log);
        logHelper.info("参数：{}", str);
        logHelper.info("本次执行上下文参数：{}", flag);
        logHelper.info("当前调度记录ID：{}", context.getSchedulingRecordID());
        logHelper.info("当前任务ID：{}", context.getTaskId());
        logHelper.info("本次调度启动时间：{}", DateUtils.formatDateTime(new Date(context.getStartTime())));
        logHelper.info("当前任务类型：{}", context
                .getTaskType()
                .toString());
        //获取当前任务运行栈
        AutoJobRunningStack stack = context.getCurrentStack();
        logHelper.info("当前栈深：{}", stack.depth());
        //获取本次运行的栈帧
        RunningStackEntry current = stack.currentStackEntry();
        //添加本次运行的一下上下文参数，以便本次执行失败重试能直接恢复
        current.addParam("runningPos", flag);
        //也可以获取上次执行的上下文参数
        Optional<RunningStackEntry> last = stack.lastStackEntry();
        last.ifPresent(stackEntry -> logHelper.info("上次执行上下文参数{}", stackEntry.getParam("runningPos")));
        last.ifPresent(stackEntry -> logHelper.info("上次是否执行成功：{}", stackEntry.isSuccess()));
        last.ifPresent(stackEntry -> logHelper.info("上次执行参数：{}", stackEntry.getArgs()));
        //获取当前任务具有的邮件客户端
        IMailClient mailClient = context.getMailClient();

        if (flag % 3 == 0) {
            throw new RuntimeException("模拟异常发生");
        }
    }
```

Of course, fuzzy matching is also possible.


```java
@AutoJob(id = 2, defaultStartTime = StartTime.NOW, asType = AutoJobTask.TaskType.DB_TASK, executableMachines = "192.168.**")//指定IP包含192.168.的机器执行
    public void createLogs() {
        AutoJobLogHelper logHelper = AutoJobLogHelper.getInstance();
        for (int i = 0; i < 1000; i++) {
            for (int j = 0; j < 100; j++) {
                logHelper.info("测试日志{}-{}", i, j);
            }
            SyncHelper.sleepQuietly(500, TimeUnit.MILLISECONDS);
        }
    }
```

You can also create tasks using the build-based approach:


```java
new AutoJobMethodTaskBuilder(Jobs.class, "hello")
                .addExecutableMachine("local")//可以使用local或者localhost，表示仅由创建该任务的机器执行
    		   //...
                .build();
```

## XIII. Customized development

### 1. Custom scheduler

The concept of scheduler has been explained in Section 9: Framework Architecture, so how to define your own scheduler? Here is a simple example:


```java
/**
 * 你的自定义调度器
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/18 14:56
 */
public class YourScheduler extends AbstractScheduler{
    //调度器默认构造方法
    public YourScheduler(AutoJobTaskExecutorPool executorPool, IAutoJobRegister register, AutoJobConfigHolder configHolder) {
        super(executorPool, register, configHolder);
    }
    
    //...调度逻辑
}

@AutoJobScan("com.example.autojob.job")
@AutoJobProcessorScan("com.example.autojob")
public class AutoJobMainApplication {
    public static void main(String[] args) {
        new AutoJobLauncherBuilder(AutoJobMainApplication.class)
                .withAutoScanProcessor()
            	//配置你的调度器，如果你的调度器支持默认构造方法可以只指定类型
                .addScheduler(YourScheduler.class)
            	//.addScheduler(new YourScheduler()) 如果不支持默认构造方法就需要添加一个实例
                .build()
                .run();
        System.out.println("==================================>系统创建完成");
    }
}
```

If you want the framework to schedule only through your scheduler, and no longer need the memory task scheduler or DB task scheduler, you can selectively shut down when the application starts:


```java
@AutoJobScan("com.example.autojob.job")
@AutoJobProcessorScan("com.example.autojob")
public class AutoJobMainApplication {
    public static void main(String[] args) {
        new AutoJobBootstrap(AutoJobMainApplication.class)
                .withAutoScanProcessor()
                .closeDBTaskScheduler() // 关闭DB任务调度器
                .closeMemoryTaskScheduler() // 关闭内存任务调度器
                .build()
                .run();
        System.out.println("==================================>系统创建完成");
    }
}
```

Notice If you disable the framework’s native memory task scheduler or DB task scheduler without specifying your own scheduler, the framework will lose the scheduling function for that type of task. If both are disabled, the framework will no longer have any task scheduling function.

### 2. Customize email alarm

The mail alarm in AutoJob is also event-driven. The framework publishes the relevant alarm event-> the corresponding processor creates the mail object-> sends. Therefore, to implement the customized mail alarm, you only need to implement: the customized alarm event, when to publish the event, and the alarm event processor (template creation).

All alarm events are inherited from `AlertEvent`. Let’s take a look at the implementation of the framework’s task run error alarm:


```java
//定义报警事件
@Getter
@Setter
public class TaskRunErrorAlertEvent extends AlertEvent {
    public TaskRunErrorAlertEvent(String title, String content, AutoJobTask errorTask) {
        super(title, AlertEventLevel.WARN, content);
        this.errorTask = errorTask;
    }
    private AutoJobTask errorTask;
    private String stackTrace;
}

//报警事件的邮件模板创建
public static AlertMail newRunErrorAlertMail(TaskRunErrorAlertEvent event) {
        AlertMailBuilder builder = AlertMailBuilder.newInstance();
        AutoJobTask errorTask = event.getErrorTask();
        return builder
            	.setMailClient(errorTask.getMailClient())
                .setTitle(event.getTitle())
                .setLevel(AlertEventLevel.WARN)
                .addContentTitle(String.format("任务：\"%d:%s\"执行失败", errorTask.getId(), errorTask.getAlias()), 1)
                .addBr()
                .addBold("报警时间：" + DateUtils.formatDateTime(event.getPublishTime()))
                .addBr()
                .addBold(String.format("报警机器：%s:%s", event
                        .getNode()
                        .getHost(), event
                        .getNode()
                        .getPort()))
                .addBr()
                .addBold("任务路径：" + errorTask.getReference())
                .addBr()
                .addParagraph("堆栈信息如下：")
                .addParagraph(event
                        .getStackTrace()
                        .replace("\n", "</br>"))
                .addError("请及时处理")
                .getAlertMail();
}

//事件处理器
@Slf4j
public class TaskRunErrorAlertEventHandler implements IAlertEventHandler<TaskRunErrorAlertEvent> {
    @Override
    public void doHandle(TaskRunErrorAlertEvent event) {
        AutoJobConfig config = AutoJobApplication.getInstance().getConfigHolder().getAutoJobConfig();
        if (!config.getTaskRunErrorAlert()) {
            return;
        }
        AlertMail alertMail = AlertMailFactory.newRunErrorAlertMail(event);
        if (alertMail != null) {
            if (alertMail.send()) {
                log.info("发送报警邮件成功");
            } else {
                log.error("发送报警邮件失败");
            }
        }
    }
}

//事件处理器添加进上下文
public class AlertEventHandlerLoader implements IAutoJobLoader {
    @Override
    public void load() {
        TaskEventHandlerDelegate
                .getInstance()
                .addHandler(TaskRunErrorEvent.class, new TaskRunErrorEventHandler());
    }
}

//事件发布
public class TaskRunErrorEventHandler implements ITaskEventHandler<TaskRunErrorEvent> {
    @Override
    public void doHandle(TaskRunErrorEvent event) {
        AlertEventHandlerDelegate
                .getInstance()
                .doHandle(AlertEventFactory.newTaskRunErrorAlertEvent(event.getTask(), event.getErrorStack()));
    }
}
```

You need to pay attention to several points in the above code: `AlertMailBuilder` it is a mail template building class, which can build a mail object; the alarm event handler and the task event handler need to be `Processor` added to the context.

### 3. Custom log storage

The default log storage location of the framework is the database. You can define the relevant storage policy and the storage policy delegator to realize the storage of logs in other places. Here is a simple demonstration:


```java
//定义日志存储策略

/**
 * 运行日志文件保存策略
 *
 * @Date 2022/11/21 9:15
 */
public class AutoJobLogFileStrategy implements IAutoJobLogSaveStrategy<AutoJobLog> {
    @Override
    public void doHandle(String taskPath, List<AutoJobLog> logList) {
        try {
            FileWriter fileWriter = new FileWriter(new File(taskPath));
            //...
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
/**
 * 执行日志文件保存策略
 *
 * @Date 2022/11/21 9:15
 */
public class AutoJobRunLogFileStrategy implements IAutoJobLogSaveStrategy<AutoJobRunLog> {
    @Override
    public void doHandle(String taskPath, List<AutoJobRunLog> logList) {
        try {
            FileWriter fileWriter = new FileWriter(new File(taskPath));
            //...
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


//设置策略委派
public class AutoJobLogFileDelegate implements ILogSaveStrategyDelegate<AutoJobLog> {
    @Override
    public IAutoJobLogSaveStrategy<AutoJobLog> doDelegate(AutoJobConfigHolder configHolder, Class<AutoJobLog> type) {
        //默认使用File保存策略
        return new AutoJobLogFileStrategy();
    }
}

public class AutoJobRunLogFileDelegate implements ILogSaveStrategyDelegate<AutoJobRunLog> {
    @Override
    public IAutoJobLogSaveStrategy<AutoJobRunLog> doDelegate(AutoJobConfigHolder configHolder, Class<AutoJobRunLog> type) {
        //默认使用File保存策略
        return new AutoJobRunLogFileStrategy();
    }
}
```

The above code defines the storage policy of the file, so how to make our policy effective? This requires us to add our policy delegate to the context when we create the task.


```java
public static void main(String[] args) {
        new AutoJobBootstrap(AutoJobMainApplication.class)
                .setLogSaveStrategyDelegate(new AutoJobLogFileDelegate()) //设置运行日志存储策略委派者
                .setRunLogSaveStrategyDelegate(new AutoJobRunLogFileDelegate()) //设置执行日志存储策略委派者
                .build()
                .run();
}
```

After setting our log storage policy delegator, the original storage policy will be overwritten, except if the original storage policy is returned `AutoJobLogDBStrategy` in your delegator logic.

### 4. Customize task filter

The task filter is a filter chain, which is executed before the task is registered in the task scheduling queue. Its main function is to filter the execution of some unsafe tasks. The framework provides a task filter `ClassPathFilter` based on class path filtering, but the white list can only be configured in the configuration file, so it is likely that you want to implement a dynamic white list configuration. For example, compare from the database and so on, then you need to inherit `AbstractRegisterFilter`, as shown below:


```java
//package com.example.spring.job
public class TestFilter extends AbstractRegisterFilter {
    @Override
    public void doHandle(AutoJobTask task) {
       if(/*...*/){
            //当该任务不允许注册时直接设置成不允许注册
            task.setIsAllowRegister(false);
        }
    }
}

@SpringBootApplication
@AutoJobScan("com.example.spring.job")
@AutoJobRegisterPreProcessorScan("com.example.spring") //指定扫描包
public class AutoJobSpringApplication {
    public static void main(String[] args) {
        SpringApplication.run(AutoJobSpringApplication.class, args);
        System.out.println("==================================>Spring应用已启动完成");
        new AutoJobBootstrap(AutoJobSpringApplication.class)
                .withAutoScanProcessor()
                .build()
                .run();
        System.out.println("==================================>AutoJob应用已启动完成");
    }

}

```

When creating an application, you also need to configure `@AutoJobRegisterPreProcessorScan` on the entry class to specify the scanning packet path of the registration pre-processor, otherwise the filter will not be scanned.

Note: Subtasks are not processed by this type of filter.

## <span style="color:#7DF46A;"> 14. Appreciation and encouragement</span>

- If you think this project is right in your heart, you think NB is great.
- If you feel that the project is solving some of your immediate needs.
- If you want the author’s cat to eat better cat bars, cat cans, and sleep in a better cat nest in winter.
- If you get some useful ideas from the project.
- If you want the project to continue to maintain, upgrade more features.
- If you want to encourage and motivate the author to devote more time and energy to improve the project, you are welcome to support and encourage this project.

<div  style="display:flex;width:100%;justify-content:space-around;align-items:center;">
    <div>
    	<img src="https://gitee.com/hyxl-520/auto-job/raw/master/doc/wechat.jpeg" style="width:300px; height:400px;margin:20px" />
    	<img src="https://gitee.com/hyxl-520/auto-job/raw/master/doc/ali.jpeg" style="width:300px;height:400px;margin:20px"/>
	</div>
</div>

