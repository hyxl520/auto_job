# Auto-Job 任务调度框架 v0.9.6

<div  style="display:flex;width:100%;height:100px;justify-content:space-around;align-items:center;">
    <div>
        <a src="./README.md">中文文档</a> |
        <a src="./README_EN.md">English Document</a>
	</div>
</div>

## <span style="color:#FFFE91;">版本更新</span>

**2022-11-22：** 初版0.9.1

------



**2022-12-9：** 0.9.2

优化API。

核心调度模块停止使用缓存时间戳，解决时间精度低的问题。

重构注解调度器。

重构执行器池，新增基于时间动态调整的线程池封装`TimerThreadPoolExecutorHelper`。

修改已知BUG。

调整项目结构。

**2022-12-26：** 新增Rest接口：链接: https://www.apifox.cn/apidoc/shared-05120000-4d19-4c75-a7f6-dc56105972cb  访问密码 : autojob 

**2022-12-27：** 优化子任务处理逻辑，新增任务MissFire事件

**2022-12-29**： 增加多数据库适配，目前支持mysql，postresql

**2023-1-3：** 

优化重试机制，每个任务可单独配置重试逻辑

优化邮件报警机制，每个任务可单独配置邮箱

新增任务重试事件`TaskRetryEvent`

调度记录表新增`is_retry`属性，表明该条调度记录是否是由于异常进行重试的调度记录

其他已知BUG

------



**2023-3-14：**  0.9.3

内置RPC框架编解码器改为使用ProtoStuff，简化API。

配置支持嵌套配置，内容可以通过VMOptions等给出，保护系统安全。

支持环境变量，便于在测试环境和生产环境切换。见：“四、环境变量”

------



**2023-3-28：** 0.9.4

对任务表进行拆分，目前拆分成aj_method_job和aj_script_job便于后期任务类型拓展。 **注意0.9.4版本与之前版本的数据库结构不兼容**

优化日志处理逻辑，从处理器私有处理线程到loop轮询，避免并发任务过多导致线程资源耗尽。

新增任务运行上下文、任务运行堆栈，便于故障恢复等。具体见“九、任务运行上下文；十一、所有配置”。

新增模板任务，更加优雅地开发任务。具体见：“十二、高级用法-注解任务开发的高级应用-`@TemplateAutoJob`”。

------



**2023-7-12：** 0.9.5

解决日志存在的BUG：日志消息队列不清空的问题。
解决集群部署时重复启动的问题。
新增任务属性：executableMachines，用于指定执行机器，只有地址匹配的机器方可执行。**数据库结构有变动（aj_method_job和aj_script_job新增executable_machines列），可以根据SQL脚本自行新增，不要直接执行脚本，否则会造成数据丢失。**

------



**2023-7-12：** 0.9.6（<span style="color:#7DF46A;">最新</span>）

**新增功能：**

- 任务并发运行：同一个任务可以同时运行多个实例。
- 任务故障转移：重试策略新增故障转移策略，在开启集群模式时有效，异常的任务会选择一个“较好的”节点进行故障转移执行。
- 任务动态分片：在集群模式下支持分片任务，节点会自动感知当前集群节点数目，根据集群情况动态分片。
- 任务保存策略：支持不存在时保存、创建一个新的版本、存在则更新策略。
- 注解式脚本任务：可以直接在一个Java方法上使用`@ScriptJob`注解来定义一个脚本任务，支持动态命令行。
- 函数任务：可以非常简单的将一些业务逻辑交由AutoJob管理，AutoJob会为这些业务逻辑提供故障重试、过长中断、日志存储的功能。

其他性能优化和BUG解决。

**更新说明：**

<span style="color:#FFFD55;">本次更新涉及数据库结构变动，需要重新执行SQL脚本，请先备份数据。</span>


## 一、背景

生活中，业务上我们会碰到很多有关作业调度的场景，如每周五十二点发放优惠券、或者每天凌晨进行缓存预热、亦或每月定期从第三方系统抽数等等，Spring和java目前也有原生的定时任务支持，但是其都存在一些弊病，如下：

- **不支持集群，未避免任务重复执行的问题**
- **不支持生命周期的统一管理**
- **不支持分片任务：处理有序数据时，多机器分片执行任务处理不同数据**
- **不支持失败重试：出现异常任务终结，不能根据执行状态控制任务重新执行**
- **不能很好的和企业系统集成，如不能很好的和企业系统前端集成以及不能很好的嵌入到后端服务**
- **不支持动态调整：不重启服务情况下不能修改任务参数**
- **无报警机制：任务失败之后没有报警通知（邮箱、短信）**
- **无良好的执行日志和调度日志跟踪**

基于原生定时任务的这些弊病，AutoJob就由此诞生，AutoJob为解决分布式作业调度提供了新的思路和解决方案。

## 二、特性

**简单：** 简单包括集成简单、开发简单和使用简单。

集成简单：框架能非常简单的集成到Spring项目和非Spring项目，得益于AutoJob不依赖于Spring容器环境和MyBatis环境，你无需为了使用该框架还得搭建一套Spring应用。

开发简单：AutoJob开发初衷就希望具有低代码侵入性和快速开发的特点，如下在任意一个类中，你只需要在某个需要调度的任务上加上注解，该任务就会被框架进行动态调度：

```java
	@AutoJob(attributes = "{'我爱你，心连心',12.5,12,true}", cronExpression = "5/7 * * * * ?")
    public void formatAttributes(String string, Double decimal, Integer num, Boolean flag) {
        //参数注入
        AutoJobLogHelper logger = new AutoJobLogHelper();//使用框架内置的日志类
        logger.setSlf4jProxy(log);//对Slf4j的log进行代理，日志输出将会使用Slf4j输出
        logger.info("string={}", string);
        logger.warn("decimal={}", decimal);
        logger.debug("num={}", num);
        logger.error("flag={}", flag);
        //使用mapper
        mapper.selectById(21312L);
        //...
    }
```

使用简单：使用该框架你无需关注太多的配置，整个框架的启动只需要**一行代码**,如下：

```java
//配置任务扫描包路径
@AutoJobScan({"com.yourpackage"})
//处理器自动扫描
@AutoJobProcessorScan({"com.yourpackage"})
public class AutoJobMainApplication {
    public static void main(String[] args) {
    //框架启动
    	new AutoJobBootstrap(AutoJobMainApplication.class)
                .build()
                .run();
        System.out.println("==================================>系统创建完成");
 	}

}
```

得益于良好的系统架构和编码设计，你的应用启动无需过多配置，只需要一行代码

**动态：** 框架提供API，支持任务的动态CURD操作，即时生效。

**多数据库支持：** 提供多类型数据库支持，目前支持MySQL、PostgreSQL、Oracle、DamengSQL，理论支持SQL标准的所有数据库。

**任务依赖：** 支持配置子任务，当父任务执行结束且执行成功后将会主动触发一次子任务的执行。

**一致性：** 框架使用DB乐观锁实现任务的一致性，在集群模式下，调度器在调度任务前都会尝试获取锁，获取锁成功后才会进行该任务的调度。

**HA<span style="color:#FFFE91;">（新）</span>：** 该框架支持去中心化的集群部署，集群节点通过RPC加密通信。集群节点之间会自动进行故障转移。

**弹性增缩容<span style="color:#FFFE91;">（新）</span>：** 支持节点的动态上下线，同时节点支持开启保护模式，防止恶劣的网络环境下节点脱离集群。

**任务失败重试<span style="color:#FFFE91;">（新）</span>：** 支持故障转移和本地重试。

**完整的生命周期：** 框架提供任务完整的生命周期事件，业务可捕捉并做对应的处理。

**动态调度线程池：** 框架使用自研的动态线程池，可灵活根据任务流量动态调整线程池核心线程和最大线程参数，节省系统线程资源，并且提供了默认的拒绝处理器，防止任务被missFire。

**异步非阻塞的日志处理：** 日志采用生产者消费者模型，基于自研的内存消息队列，任务方法作为日志的生产者，生产日志放入消息队列，框架启动对应的日志消费线程进行日志处理。

**实时日志：** 日志将会实时的进行保存，便于跟踪。

**任务白名单：** 提供任务白名单功能，只有在白名单中的任务才允许被注册和调度，保证系统安全。

**可拓展的日志存储策略：** 日志支持多种策略保存，如内存Cache、数据库等，可根据项目需要灵活增加保存策略，如Redis、文件等。

**丰富的调度机制：** 支持Cron like表达式，repeat-cycle调度、子任务触发、延迟触发等，得益于良好的编码设计，用户可非常简单的新增自定义调度器，如下：

```java
/**
 * 你的自定义调度器
 * @Author Huang Yongxiang
 * @Date 2022/08/18 14:56
 */
public class YourScheduler extends AbstractScheduler{
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
            	//配置你的调度器
                .addScheduler(YourScheduler.class)
                .build()
                .run();
        System.out.println("==================================>系统创建完成");
    }
}
```

**任务报警：** 框架支持邮件报警，目前原生支持QQ邮箱、163邮箱、GMail等，同时也支持自定义的邮箱smtp服务器。

![1668580284754](https://gitee.com/hyxl-520/auto-job/raw/master/doc/%E9%82%AE%E4%BB%B6%E6%8A%A5%E8%AD%A6.png)

目前系统提供：任务失败报警、任务被拒报警、节点开启保护模式报警、节点关闭保护模式报警，当然用户也可非常简单的进行邮件报警的拓展。

**丰富的任务入参：** 框架支持基础的数据类型和对象类型的任务入参，如Boolean,String,Long,Integer,Double等类型，对于对象入参，框架默认使用JSON进行序列化入参。

**良好的前端集成性：** 框架提供相关API，用户可以灵活开发Restful接口接入到企业项目，无需额外占用一个进程或机器来单独运行调度中心。

**内存任务：** 框架提供DB任务和内存任务两种类型，DB任务持久化到数据库，声明周期在数据库内记录，内存任务除了日志，整个生命周期都在内存中完成，相比DB任务具有无锁、调度快速的特点。

**脚本任务：** 提供脚本任务的执行，如Python、Shell，SQL等。

**动态分片<span style="color:#FFFE91;">（新）</span>：** 集群模式下框架支持任务分片，多机运行。

**全异步：** 任务调度流程采用全异步实现，如异步调度、异步执行、异步日志等，有效对密集调度进行流量削峰，理论上支持任意时长任务的运行。

## 三、快速使用

### 1、项目导入

该框架不依赖于Spring容器环境和MyBatis等持久层框架，你可以将其作为一个Maven模块导入到你的项目中，你可以去码云上下载：https://gitee.com/hyxl-520/auto-job.git

项目分为两个模块：auto-job-framework和auto-job-spring，前者是框架的核心部分，后者是与Spring集成的使用，后续可能会基于Spring web开发相关控制台。

### 2、项目配置

项目配置主要为框架配置和数据源配置。框架配置默认读取类路径下的`auto-job.yml`和`auto-job.properties`文件，具体配置项内容见“所有配置”；数据源配置，框架默认使用Druid作为连接池，你只需要在`druid.properties`文件中配置数据源就行了，当然你可以自定义数据源，具体方法在`AutoJobBootstrap`里。相关建表脚本可以在db目录下找到。框架默认使用MySQL数据库，理论上支持SQL标准的其他数据库

### 3、任务开发

#### 3.1、基于注解

开发一个基于注解的任务非常简单，除了日志输出使用框架内置的日志辅助类`AutoJobLogHelper`输出外，其他你就只需要关心你的业务。当然，`AutoJobLogHelper`使用起来和slf4j几乎没有区别，它提供四种级别的日志输出：debug、info、warn、error，而且你可以使用`AutoJobLogHelper`对你的slf4j进行代理，这样这些任务执行中输出的日志将会直接使用slf4j进行输出。如下，是一个简单演示：

```java
 @AutoJob(attributes = "{'我爱你，心连心',12.5,12,true}", cronExpression = "5/7 * * * * ?", id = 2, alias = "参数测试任务")
    public void formatAttributes(String string, Double decimal, Integer num, Boolean flag) {
        AutoJobLogHelper logger=new AutoJobLogHelper();
        //log是org.slf4j.Logger对象，这里对其进行代理
        logger.setSlf4jProxy(log);
        logger.info("string={}", string);
        logger.warn("decimal={}", decimal);
        logger.debug("num={}", num);
        logger.error("flag={}", flag);
    }
```

在你开发的任务上加上`@AutoJob`注解，配置一些东西，这个任务就开发完成了。`@AutoJob`是用来标识一个方法是一个AutoJob任务，当然还有其他注解，这里暂不做阐述。细心的同学会发现这个任务是有参数的，没错，AutoJob框架支持参数，更多参数的配置后文会详细讲解。

#### 3.2、基于构建

手动创建任务相比注解来说更为灵活，框架提供了创建任务的构建者对象，如`AutoJobMethodTaskBuilder`和`AutoJobScriptTaskBuilder`对象，前者用于构建方法型任务，后者用于构建脚本型任务。

```java
MethodTask task = new AutoJobMethodTaskBuilder(Jobs.class, "hello") //方法型任务需要指定方法所在的类以及方法名
          .setTaskId(IdGenerator.getNextIdAsLong())
          .setTaskAlias("测试任务") //任务别名
    	  .setParams("{'我爱你，心连心',12.5,12,true}") //任务参数，支持simple参数
          .setTaskType(AutoJobTask.TaskType.MEMORY_TASk)
          .setMethodObjectFactory(new DefaultMethodObjectFactory()) //方法运行对象工厂，用于创建方法运行的对象上下文
          .addACronExpressionTrigger("* 5 7 * * * ?", -1) //添加一个cron-like触发器
          .build();

AutoJobApplication
         .getInstance()
         .getMemoryTaskAPI() //获取全局的内存任务的API
         .registerTask(new AutoJobMethodTaskAttributes(task)); //注册任务
```

#### 3.3、基于FunctionalInterface

在实际的开发中，我们有的业务逻辑并不是一个定时调度的任务，但是我们希望能够使用AutoJob提供的一些功能，比如故障重试、过长中断、日志DB存储。在0.9.6以前，AutoJob的任务只有方法型任务和脚本型任务，如果每一段业务逻辑都要封装成一个方法的话十分麻烦，因此0.9.6提供了`FunctionTask`，支持在运行时书写任务交由AutoJob管理，见如下例子：

```java
@AutoJobScan("com.jingge.spring")
public class Server {
    public static void main(String[] args) {
        new AutoJobBootstrap(Server.class, args)
                .withAutoScanProcessor()
                .build()
                .run();
        /*=================测试=================>*/
        //创建一个FunctionTask，参数是要执行的任务逻辑
        FunctionTask functionTask = new FunctionTask(context -> {
            context
                    .getLogHelper()
                    .info("测试一下");
            //阻塞5秒，模拟任务执行时长5秒
            SyncHelper.sleepQuietly(5, TimeUnit.SECONDS);
        });
        //直接调用submit方法提交任务给AutoJob执行，将会返回一个FunctionFuture对象
        FunctionFuture future = functionTask.submit();
        //可以阻塞等待执行结果
        System.out.println("执行完啦：" + future.get());
        //可以直接调用getLogs方法获取任务执行的日志，日志将会按照配置的保存策略保存到指定位置（一般是DB）
        functionTask
                .getLogs(3, TimeUnit.SECONDS)
                .forEach(System.out::println);
        System.out.println("日志输出完成");
        /*=======================Finished======================<*/
    }
}
```

### 4、框架启动

得益于良好的设计，该框架你可以在任何一个main方法启动，如下是示列的一个启动

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
        System.out.println("==================================>AutoJob应用已启动完成");
    }
}
```

第5行是用于配置任务扫描的类路径，支持子包扫描，不配置时会扫描整个项目，用时较长。

第6行是处理器扫描，处理器主要是在框架启动前和框架启动后进行一些处理，默认是扫描整个项目，注意该注解只有设置了withAutoScanProcessor才能生效，如代码第10行，框架自己的处理器为自动加载，无需配置。

第9-12行是框架的启动代码，`AutoJobBootstrap`是应用引导构建程序，通过它你能增加很多自定义的配置。在第11行后，AutoJob应用即创建完成，第12行调用run方法启动整个应用。

### 5、动态修改

框架本身不是一个Web应用，没有提供对应修改的Rest接口，但是框架提供了很多操作任务的API，你可以在`AutoJobAPI`和`AutoJobLogAPI`里找到。你可以你可以参考auto-job-spring模块里提供的实例开发对应Rest接口，随着版本更替，autojob将会在未来支持控制台。

## 四、环境变量

在实际开发中，应用具有一般具有不同的环境，如测试环境、生产环境，不同环境使用不同的数据源为了适应以上场景AutoJob自0.9.4开始支持环境变量，引入的目的主要是适应不同场景的数据源，如当前环境变量如果是`dev`的话则会读取`druid-dev.proterties`的内容。缺省是`AUTO_JOB_ENV`，当然你可以自定义环境变量KEYNAME，如下：

```java
public static void main(String[] args) {
        new AutoJobBootstrap(AutoJobRunner.class)
            	//如果设置的env格式是"${name}"的格式则会去读取key=name的值作为环境变量，反之给定的字符串就认定是环境变量，如.setEnv("dev")，则dev就是环境变量。
                .setEnv("${env}")
                .build()
                .run();
}
```

## 五、任务类型

### 按照功能分类

任务按照功能可以分为方法型任务和脚本型任务。

方法型任务对应Java中的一个方法，该方法可以有返回值，允许有参数，参数的注入可以见“任务参数”。方法内部的日志输出必须使用`AutoJobLogHelper`来输出，否则日志可能无法保存。

脚本型任务对应一个磁盘上的脚本文件或一段cmd命令。具体使用可见章节：“高级用法-脚本任务”。

### 按照调度方式分类

任务按照调度方式可以分为内存型任务和DB型任务。

内存型任务的生命周期都在内存中完成，具有调度迅速、无锁、随调随动的特点，适合短周期、有限次、临时性的任务。

DB型任务将会保存到数据库，每一次调度都会更新数据库相关状态。DB型任务采用乐观锁，每次执行前都需要获得锁才能执行，具有长期性、易维护、易修改等特点，适合于定期数据同步、定时缓存预热等在长期内都会用到的任务。

## 六、任务参数

**方法型任务**

方法型任务支持两种参数格式，一种是FULL型参数，一种是SIMPLE参数，具体区别可见如下示列：

```java
void exampleMethod1(String str, Integer num, Double decimal, Boolean flag);

void exampleMethod2(String str, Integer num, Double decimal, Boolean flag, Long count, Param param);

class param{
    private int id;
    private String num;
    //...
}
```

如上方法：`exampleMethod1`，使用SIMPLE型参数：

```java
MethodTask task = new AutoJobMethodTaskBuilder(Jobs.class, "hello") 
          .setTaskId(IdGenerator.getNextIdAsLong())
          .setTaskAlias("测试任务")
    	  .setParams("{'我是字符串参数',12,12.5,true}")
          .setTaskType(AutoJobTask.TaskType.MEMORY_TASk)
          .setMethodObjectFactory(new DefaultMethodObjectFactory()) 
    	  .build();
//{'我是字符串参数',12,12.5,true}
```

使用FULL型参数

```java
MethodTask task = new AutoJobMethodTaskBuilder(Jobs.class, "hello")
                .setTaskId(IdGenerator.getNextIdAsLong())
                .setTaskAlias("测试任务")
                .setParams("[{\"values\":{\"value\":\"字符串参数\"},\"type\":\"string\"},{\"values\":{\"value\":12},\"type\":\"integer\"},{\"values\":{\"value\":12.5},\"type\":\"decimal\"},{\"values\":{\"value\":false},\"type\":\"boolean\"}]")
                .setTaskType(AutoJobTask.TaskType.MEMORY_TASk)
                .setMethodObjectFactory(new DefaultMethodObjectFactory())
                .build();

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
  }
]
*/
```

我们可以发现SIMPLE参数十分简单，`"{a1,a2,a3,...}"`，参数表达式本身是一个字符串，大引号包裹，参数顺序按照从左到右依次匹配。SIMPLE参数支持四类参数

`'字符串参数'`，单引号包裹，对应类型`String`；

`12`：整数型参数，对应类型：`Integer`包装类型，如果数值超过整形范围，则会自动匹配`Long`类型。

`12.5`：小数型参数，对应类型：`Double`包装类型。

`true|false`：布尔型参数，对应类型：`Boolean`包装类型。

FULL型参数相比就要复杂的多了，本身是一个JSON数组字符串，每一个JSON对象代表一个参数，每个对象有type和values两个属性，字面意思，类型和值，FULL类型除了支持SIMPLE型的四种类型参数外还支持对象型，对象型的参数使用JSON来进行序列化和反序列化。由于FULL型参数过于复杂，因此框架提供了`AttributesBuilder`对象，可以非常简单的生成FULL型参数，以`exampleMethod2`为例：

```java
Param param = new Param();
        param.setId(1);
        param.setNum("12");
System.out.println(new AttributesBuilder()
        .addParams(AttributesBuilder.AttributesType.STRING, "字符串参数")
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

一般来说，基于注解的任务开发我们更倾向于推荐使用SIMPLE型参数，简单、明了；基于构建的任务开发我们更钟意于FULL型参数，类型丰富。

**脚本型任务**

脚本型任务的参数是通过启动命令给出的，如`python /script.test.py -a 12 -b`，其中`-a 12`和`-b`就是两个参数，因此脚本型任务只支持字符串型参数。

## 七、任务运行对象工厂

任务运行对象工厂是方法型任务才有的属性，因为方法型任务对应的是Java某个类中的方法，因此方法的执行可能依赖于对象实例的上下文，特别是当该框架与Spring集成时很可能会使用Spring容器中的Bean，因此可以指定创建方法依赖的对象的工厂：`IMethodObjectFactory`，框架默认使用类的无参构造方法创建对象实例，当然你可以创建自定义的工厂：

```java
public class SpringMethodObjectFactory implements IMethodObjectFactory {
    public Object createMethodObject(Class<?> methodClass) {
        // SpringUtil持有Spring的容器，获取Spring容器中的Bean
        return SpringUtil.getBean(JobBean.class);
    }
}
```

那么怎么让我们的任务运行对象工厂生效呢，见如下示列：

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

## 八、任务日志

作为一款任务调度框架，详细的日志一定是必不可少的。框架提供三种类型日志记录：调度日志、执行日志、运行日志

**调度日志**

任务的每一次启动到完成被任务是一次调度，调度日志详细记录了调度任务的基础信息、调度时间、运行状态、执行时长、以及任务结果（任务结果对应方法型任务是返回值，由JSON序列化，脚本型任务是脚本返回值）。调度日志对应数据库表`aj_scheduling_record`，其ID关联到本次调度中产生的运行日志和执行日志。

**运行日志**

运行日志为任务在运行期间内部输出的日志，方法型任务为使用`AutoJobLogHelper`输出的日志，脚本型任务为脚本或cmd命令在控制台的输出。运行日志对应数据库表`aj_job_logs`。

**执行日志**

执行日志记录了某次调度任务的执行情况，如何时启动、何时完成、是否运行成功、任务结果、任务异常等。执行日志对应库表`aj_run_logs`。

任务日志都是实时更新的，如果你使用的是框架的默认日志保存策略（数据库存储），你可以通过`AutoJobLogDBAPI`获取到日志。运行日志和执行日志都绑定了调度ID，通过调度ID即可找到本次调度所产生的运行日志和执行日志。

## 九、任务运行上下文

AutoJob自0.9.4开始开始支持运行上下文，任务内部可以通过上下文获取当前任务运行相关的内容，如下是一个使用示列:

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

推荐开启堆栈跟踪，任务使用上下文保留每次运行的状态，由于异常情况导致任务重试可以继上次运行到的位置继续运行。

**注意！建议将任务堆栈最大深度设置在一个合理的值，避免堆栈过深占用大量内存，特别是并发任务量特别多时。配置内容见“十一、所有配置-`autoJob.running.stackTrace` ”**

## 十、框架架构

<img src="https://gitee.com/hyxl-520/auto-job/raw/master/doc/%E6%9E%B6%E6%9E%84%E5%9B%BE-v0.9.1.jpg">

框架架构图的左部分的组件是框架的核心组件。

**任务容器模块**

任务容器模块包含DB任务容器和内存任务容器，分别用于存放DB型的任务和内存型的任务。

**调度模块**

调度模块由调度器、任务调度队列、注册器、时间轮调度器以及时间轮构成。内存任务调度器`AutoJobMemoryTaskScheduler`和DB任务调度器`AutoJobDBScheduler`负责从任务容器调度出即将执行的任务（<=5秒）放到任务调度队列缓存`AutoJobTaskQueue`。时间轮调度器`AutoJobTimeWheelScheduler`通过注册器`AutoJobRegister`调度任务调度队列中的任务进入时间轮，准备执行。时间轮按秒滚动，将执行的任务提交进任务执行器池进行执行。运行成功调度器`AutoJobRunSuccessScheduler`执行运行成功后的相关操作，比如更新状态、更新下次触发时间等等，运行失败调度器`AutoJobRunErrorScheduler`执行运行失败后的相关操作，比如更新状态、根据配置的重试策略更新触发时间、故障转移等等。

**任务执行器池模块**

任务执行器池包含两个动态线程池，分别为快池（fast-pool）和慢池（slow-pool），任务默认第一次执行提交进快池，第二次执行会根据上次执行时长决定是否降级处理。动态线程池是具有根据流量动态调节的线程池，具体的配置可以见“十、所有配置:执行器池配置”。

**日志模块**

日志模块和核心调度模块是完全解耦的，运行日志由任务执行时产生并且发布到内存消息队列，日志模块监听消息发布事件并且取出消息放入消息buffer，单独由日志处理线程定期、定量保存日志。运行日志通过监听任务事件来进行保存。日志模块的设计都是异步化的，尽最大可能减小日志IO对调度的影响。

除了以上的核心组件外，框架还有部分功能拓展组件。

**生命周期处理器**

生命周期处理器也可以理解成生命周期钩子，具体来说是一个任务的生命周期钩子，具体看下面的生命周期事件图

<img src="https://gitee.com/hyxl-520/auto-job/raw/master/doc/%E7%94%9F%E5%91%BD%E5%91%A8%E6%9C%9F%E5%9B%BE.jpg">

要使用一个生命周期钩子也十分简单，下面来看一个示列：

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

以上示列表示一个在任务执行前在控制台输出：“任务：{任务别名}即将开始运行”，要实现一个事件处理器只需要实现`ITaskEventHandler`接口即可，泛型代表你需要处理的事件。当然还可以通过如下方式来实现同上面示列一样的功能

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

`TaskEvent`是所有任务事件的父类，实现其父类事件的处理器时所有的任务相关事件都会执行该处理器，可以判断事件类型来完成相关操作，当一个处理器需要处理多种事件类型时可以如上使用。每个事件处理器可以通过重写`getHandlerLevel`方法指定级别，数字越大，级别越高，执行越会被优先执行。父事件处理器高级别>父事件处理器低级别>子事件处理器高级别>子事件处理器低级别。当然，只声明处理器不将其添加到应用也不会生效的，下面介绍如何使得事件处理器生效。

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

上面的代码演示了如何添加处理器到上下文。在`AutoJob`中，在框架启动前和框架关闭前执行某些操作的处理器成为`Processor`，框架启动前执行的处理器为`IAutoJobLoader`，框架关闭前执行的处理器为`IAutoJobEnd`，上面代码中，通过启动处理器将事件处理器添加到“事件委派者”：`TaskEventHandlerDelegate`，再在应用构建时手动将启动处理器添加到应用上下文中。当然如果你的`Processor`非常多，可以通过注解`@AutoJobProcessorScan`来自动扫描`Processor`，可以指定扫描的包，支持子包扫描，不指定时默认全项目扫描。扫描后通过调用`Processor`的无参构造方法创建实例后自动注入上下文。如下示列：

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

## 十一、所有配置

框架提供了丰富的配置，这些配置默认是从`auto-job.yml`或者`auto-job.properties`文件中加载，当然你可以从数据库动态加载实现动态配置，全部配置如下：

```yaml
# 动态任务调度框架配置V0.9.6
autoJob:
  debug:
    # debug模式，打开后将会使用WARN级别打印调度相关日志帮助调试
    enable: false
  context:
    # 调度队列长度，将要执行的任务将会放入调度队列，当时间段内并发任务量很大时建议将此值设置为较大的值
    schedulingQueue:
      length: 1000
    # 内存任务容器相关配置
    memoryContainer:
      length: 200
      # 内存任务执行完成后的处理策略：CLEAN_FINISHED-清理已完成的任务 KEEP_FINISHED-在缓存中存储
      cleanStrategy: KEEP_FINISHED
    running:
      # 是否开启任务运行堆栈
      stackTrace:
        enable: true
        depth: 16
  # 注解扫描相关配置
  annotation:
    # 注解扫描过滤器
    filter:
      enable: true
      classPattern: "**.**"
    # 是否启用注解任务扫描
    enable: true
    # 注解未配置相关触发器信息时默认延迟触发的时间：分钟
    defaultDelayTime: 30
  # 数据库类型，目前支持MySQL、PostgreSQL、Oracle、DamengSQL，理论支持SQL标准的所有数据库
  database:
    type: mysql
  # 执行器相关配置
  executor:
    fastPool:
      update:
        # 是否启用快池的线程资源动态调配
        enable: true
        # 允许任务的最大响应时间：秒，当开启动态调配时有效
        allowTaskMaximumResponseTime: 1
        # 快池流量监控的周期：秒
        trafficUpdateCycle: 5
        # 资源更新阈值，当任务的实际响应时间与允许任务的最大响应时间相比超出该阈值时会进行动态调整
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
      # 降级阈值，当任务执行时长超过该时间下次会自动降级到慢池执行（分钟）
      threshold: 3
  register:
    filter:
      enable: true
      classPath: "**.**"
  scheduler:
    finished:
      error:
        retry:
          # 重试策略 LOCAL_RETRY-本机重试 FAILOVER-故障转移（开启集群时有效）
          strategy: FAILOVER
          enable: true
          retryCount: 5
          interval: 0.5
  emailAlert:
    enable: false
    # 邮箱服务器类型，目前仅支持SMTP
    serverType: SMTP
    # 邮件发送间隔：毫秒
    interval: 5000
    auth:
      sender: "1158055613@qq.com"
      receiver: "XXXXXX@qq.com"
      token: "XXXXXX"
      # 邮箱类型，目前支持QQMail，gMail(谷歌邮箱)，163Mail，outLookMail，customize（自定义，自定义需要配置自定义邮箱服务器的地址和端口）
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
    # 日志保存策略
    strategy:
      # 当内存日志条数达到此阈值执行一次保存
      saveWhenBufferReachSize: 10
      # 当距离上次日志保存超过该时间（秒）执行一次日志保存
      saveWhenOverTime: 10
    scriptTask:
      # 脚本任务日志的编码格式，该编码和平台相关
      encoding: GBK
  # 集群部署相关配置
  cluster:
    # 是否开启集群模式，开启后支持故障转移和任务分片
    enable: true
    # 本机绑定的TCP端口号
    port: 9501
    auth:
      # 是否开启集群节点身份认证
      enable: true
      # 加密通信秘钥，AES加密，秘钥长度16位
      publicKey: "autoJob!@#=123.#"
      # 身份token
      token: "hello"
    client:
      # 远程通信节点地址，只需要写集群一个节点地址即可，节点会自动同步集群的节点路由表
      remoteNodeAddress: "localhost:9502"
      # 客户端池化配置
      pool:
        size: 10
        getTimeout: 1
        getDataTimeout: 3
        connectTimeout: 2
        keepAliveTimeout: 10
      # 两个集群节点之间允许的最大时差：毫秒，请将建立连接的时长算入
      allowMaxJetLag: 3000
      nodeSync:
        # 集群节点路由表同步周期，同时会作心跳检测
        cycle: 10
        # 下线阈值，当n次心跳检测节点都不在线将会剔除（开启保护模式后除外）
        offLineThreshold: 3
    config:
      protectedMode:
        # 是否开启保护模式，开启后节点不会被剔除
        enable: true
        # 开启阈值，当当前集群节点数低于曾经最大节点数的此百分比时开启保护模式
        threshold: 0.3
```

当然上面配置并不是都需要你配置，框架基本所有配置都设置了默认值，能保证常规场景下的调度。AutoJob的配置除了通过本地配置文件给出，也可以通过外部输入流引入。

AutoJob自0.9.3版本开始，支持嵌套配置，即你可以在配置文件value使用${key}来代替，key可以通过VMOptions指定，也可以通过Program arguments指定，保护系统安全，如数据库相关配置等，如下面的一个数据库配置

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

其中DB的端口与和密码是通过启动时配置VMOptions注入，避免配置文件泄露导致的密码泄露。AutoJob自0.9.3开始也支持环境变量：`auto.job.env`，环境变量目前主要用于数据库配置文件的读取，如，如果指定环境变量`-Dauto.job.env=dev`，则会读取`druid-dev.peroperties`相关的数据库配置。

## 十二、高级用法

### 1、动态分片<span style="color:#FFFE91;">（新）</span>

在开启集群模式下AutoJob支持任务分片，多机运行。下面看两个示列：

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

上面演示了基于注解的分片配置，包含方法型任务和脚本任务，方法型任务的分片可以从`AutoJobRunningContext`里获取到，脚本任务是通过`-total`和`-current`给出的。

分片由一个节点创建好广播给集群里面的各个节点，每个节点收到分片后会直接执行，如果分片执行异常且在开启允许分片故障重试的情况下分片会在执行节点重试，广播分片的节点不会感知各个分片的执行情况，分片的执行由接受的节点负责，如果在广播分片时出现异常，如节点宕机，网络异常等等，该分片会熔断到当前节点运行。

分片的策略默认是基于整数分片，当然可以自定义分片策略：

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

### 2、故障转移<span style="color:#FFFE91;">（新）</span>

在开启集群模式时任务支持故障转移，全局重试配置如下：

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

也可以在基于构建创建任务时单独配置：

```java
new AutoJobMethodTaskBuilder(Jobs.class, "hello")
    		    //...
                .setRetryConfig(new AutoJobRetryConfig(true, RetryStrategy.FAILOVER, 3, 3))
                .build();
```

### 3、脚本任务

框架支持脚本任务，原生支持：Python、Shell、PHP、NodeJs以及PowerShell，提供其他脚本类型拓展。脚本任务对应的对象为`ScriptTask`。脚本作为一个服务器上的脚本文件保存在磁盘上，要构建一个脚本任务非常简单，框架提供`AutoJobScriptTaskBuilder`来辅助构建一个完整的脚本任务，下面看几个示列：

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

以上示列除了演示了如何创建一个脚本任务，也介绍了触发器。框架提供了四种触发器，分别是cron-like触发器、simple触发器、父-子任务触发器、延迟触发器，具体触发器的介绍上面代码注释基本讲解了这里就不作冗述。

**脚本任务自0.9.6版本开始支持注解方式创建，具体见2、注解任务开发的高级应用-`@ScriptJob`**`

### 4、注解任务开发的高级应用

在第三章节-第三小节-基于注解中，简单演示了注解`@AutoJob`的用法，AutoJob框架还提供了其他注解，如`@FactoryAutoJob`、`@Conditional`等，下面一一讲解。

#### **`@AutoJob`注解**

`@Autojob`注解是框架中使用最多的一个注解，将其标注在一个方法上，配置好调度信息，该方法就会在应用启动时将其包装成一个方法型任务放到对应的任务容器，可以参考下下面的示列。

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

#### **`@FactoryAutoJob`注解**

由于`@AutoJob`的配置都是固定的，可能你希望能够动态配置任务的某些属性，因此`@FactoryAutoJob`就为了解决此类场景而出现的，当然你也可以使用基于构建的方式开发任务来实现动态，下面来看一个示列：

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

如上示列，`getRandomString`的包装将由`RandomStringMethodFactory`来进行。

#### **`@Conditional`注解**

相信经常使用Spring的小可耐们对此注解应该熟悉，在Spring中，该注解用于实现条件注入，即符合条件时该Bean才会注入到容器。在AutoJob中，功能类似，只有符合该注解指定条件的方法才能被包装成一个任务。

#### **`@TemplateAutoJob`注解**

基于`@AutoJob`开发任务比较死板，无法灵活的动态指定某些参数，基于构建和`@FactoryAutoJob`开发任务虽然灵活，但是需要配置大量参数，比较麻烦，因此AutoJob自0.9.4推出`@TemplateAutoJob`：基于模板的任务，要实现一个模板任务只需要指定一个类继承`AutoJobTemplate`，如下的示列：

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

子类必须实现`run`、`cron`以及`params`方法，其他方法可以根据需要选择性覆盖重写以支持当前场景。模板任务既有`@AutoJob`方式的方便也兼顾`@FactoryAutoJob`和基于构建方式的灵活，模板提供了任务配置读取、任务启动关闭控制、任务中断逻辑等模板功能，但是当任务量上去的时候也会导致类的数量大量增加。

#### **`@ScriptJob`**<span style="color:#FFFE91;">（新）</span>

原来的版本要创建一个脚本任务只能使用`AutoJobScriptTaskBuilder`来创建，需要配置大量参数，十分麻烦，因此0.9.6版本推出`@ScriptJob`注解，实现声明式创建脚本任务，具体用法如下：

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

命令行模板使用{}作为插值，类似slf4j日志输出的方式，基于此你可以实现动态命令行。

### 5、使用内置RPC框架

AutoJob的目标是一款分布式的任务调度框架，因此内部开发了通信框架：RPC， 这里只做简单介绍。每一个AutoJob都有服务端和客户端，服务端的开启可以通过在配置文件里`cluster.enable=true`开启，要使用RPC框架首先需要开发服务提供类，如框架自带的API：

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

其他AutoJob节点如何调用该服务呢，也非常简单，如下示列：

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

内嵌RPC框架基于netty开发，使用ProtoStuff进行序列化和反序列化。目前RPC仅供学习使用。

### 6、使用基于时间动态调整的线程池封装

框架的执行池`AutoJobTaskExecutorPool`是任务执行的地方，其包含一个快池和一个慢池，分别用于执行运行时间短和运行时间长的任务。框架任务执行原生使用的是两个基于流量动态更新的线程池`FlowThreadPoolExecutorHelper`，为了更加适应业务需求，提供基于时间动态调整的线程池`TimerThreadPoolExecutorPool`。

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

如上示列，快池使用基于时间动态调整的线程池封装，其会在每天早上七点将线程池扩容到核心10线程，最大20线程，核心空闲时长更新为60秒，在每晚十点将线程池缩容到核心0线程，最大1线程并且添加了一个触发监听器；慢池使用基于流量调整线程池封装。

### 7、指定执行机器

有时候我们的AutoJob部署在多个不同的服务上，但是使用的是同一个数据库，在运行方法型任务时就有可能被其他没有这个方法的服务捕获到这个任务导致执行失败，因此可以通过指定执行机器来解决，如下示列：

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

当然也可以模糊匹配

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

使用基于构建的方式创建任务也可以：

```java
new AutoJobMethodTaskBuilder(Jobs.class, "hello")
                .addExecutableMachine("local")//可以使用local或者localhost，表示仅由创建该任务的机器执行
    		   //...
                .build();
```

## 十三、定制化开发

### 1、自定义调度器

调度器的概念在第九节：框架架构里已经说明，那么怎么来自定义一个自己的调度器呢，下面做一个简单示列：

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

可能你希望框架只通过你的调度器来进行调度，而不再需要内存任务调度器或DB任务调度器，你可以在应用启动时选择性关闭：

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

注意！！！如果你没有指定自己的调度器而关闭了框架原生的内存任务调度器或DB任务调度器，则框架会丧失该类型任务的调度功能，如果都关闭了则框架不再具有任何任务的调度功能。

### 2、自定义邮件报警

AutoJob中的邮件报警也是事件驱动的，框架发布相关报警事件->对应处理器创建邮件对象->发送，因此要实现自定义的邮件报警，只需要实现：自定义的报警事件、何时发布事件、报警事件处理器（模板的创建）。

所有的报警事件都继承于`AlertEvent`，下面我们看一下框架的任务运行错误报警的实现方式：

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

上面的代码大家需要关注几个地方：`AlertMailBuilder`是一个邮件模板构建类，可以构建一个邮件对象；报警事件处理器和任务事件处理器一样需要通过`Processor`添加进上下文。

### 3、自定义日志存储

框架默认日志的存储位置是数据库，你可以自己定义相关的存储策略和存储策略委派者，来实现日志在其他地方的存储。下面来简单演示：

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

以上代码定义好了文件的存储策略，那么如何使得我们的策略生效呢，这就需要我们再创建任务时把我们的策略委派给添加进上下文

```java
public static void main(String[] args) {
        new AutoJobBootstrap(AutoJobMainApplication.class)
                .setLogSaveStrategyDelegate(new AutoJobLogFileDelegate()) //设置运行日志存储策略委派者
                .setRunLogSaveStrategyDelegate(new AutoJobRunLogFileDelegate()) //设置执行日志存储策略委派者
                .build()
                .run();
}
```

将我们的日志存储策略委派者设置进去后，原有的存储策略就会被覆盖，当然如果你的委派者逻辑里面返回了`AutoJobLogDBStrategy`等原生的保存策略除外。

### 4、自定义任务过滤器

任务过滤器是一个过滤器链，在任务注册进任务调度队列前执行，主要功能是用于过滤某些不安全的任务的执行，框架提供了基于类路径过滤的任务过滤器`ClassPathFilter`，但是白名单只能在配置文件配置，因此很可能你希望实现一个动态的白名单配置，比如从数据库比对等等，这时你就需要继承`AbstractRegisterFilter`，如下示列：

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

在创建应用时还需要在入口类上配置`@AutoJobRegisterPreProcessorScan`，指定注册前置处理器的扫描包路径，否则该过滤器不会被扫描到。

注意：子任务不会被该类过滤器处理。

## <span style="color:#7DF46A;">十四、赞赏鼓励</span>

- 如果你觉得这个项目正和你的心意，觉得NB，很棒。
- 如果你觉得该项目正解决了你的某些燃眉之急。
- 如果你希望作者的猫猫吃到更好的猫条、猫罐头，冬天能睡上更好的猫窝~
- 如果你从项目中获取到了某些有用的思路、想法。
- 如果你希望该项目能持续维护，升级更多的功能。
- 如果你希望鼓励、激励作者投入更多的时间精力提升项目，欢迎各位支持和鼓励本项目。

<div  style="display:flex;width:100%;justify-content:space-around;align-items:center;">
    <div>
    	<img src="https://gitee.com/hyxl-520/auto-job/raw/master/doc/wechat.jpeg" style="width:300px; height:400px;margin:20px" />
    	<img src="https://gitee.com/hyxl-520/auto-job/raw/master/doc/ali.jpeg" style="width:300px;height:400px;margin:20px"/>
	</div>
</div>

