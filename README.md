# MapReduceDemo
**开发环境：IDEA+hadoop2.6.4（windows版本对应的各种库）**

学了这么久的大数据知识，最近总算有时间来总结下最近学的Demo。
先介绍下MR中的各种自定义组件：详细知识请参考我的博客：https://blog.csdn.net/qq_16633405/article/details/78924165

mapreduce在编程的时候，基本上一个固化的模式，没有太多可灵活改变的地方，除了以下几处：


**1、输入数据接口：InputFormat —> FileInputFormat(文件类型数据读取的通用抽象类) DBInputFormat （数据库数据读取的通用抽象类）**

默认使用的实现类是 ：TextInputFormat。 
job.setInputFormatClass(TextInputFormat.class) 
TextInputFormat的功能逻辑是：一次读一行文本，然后将该行的起始偏移量作为key，行内容作为value返回

**2、逻辑处理接口： Mapper**

完全需要用户自己去实现其中 map() setup() clean()

**3、map输出的结果在shuffle阶段会被partition以及sort，此处有两个接口可自定义：**
 
Partitioner 
有默认实现 HashPartitioner，逻辑是 根据key和numReduces来返回一个分区号； key.hashCode()&Integer.MAXVALUE % numReduces 
通常情况下，用默认的这个HashPartitioner就可以，如果业务上有特别的需求，可以自定义Comparable 
当我们用自定义的对象作为key来输出时，就必须要实现WritableComparable接口，override其中的compareTo()方法

**4、reduce端的数据分组比较接口 ： Groupingcomparator**
 
reduceTask拿到输入数据（一个partition的所有数据）后，首先需要对数据进行分组，其分组的默认原则是key相同，然后对每一组kv数据调用一次reduce()方法，并且将这一组kv中的第一个kv的key作为参数传给reduce的key，将这一组数据的value的迭代器传给reduce()的values参数

**5、逻辑处理接口：Reducer**
 
完全需要用户自己去实现其中 reduce() setup() clean()

**6、输出数据接口： OutputFormat —> 有一系列子类 FileOutputformat DBoutputFormat …..**

默认实现类是TextOutputFormat，功能逻辑是： 将每一个KV对向目标文本文件中输出为一行

**整个过程需要注意以下几点：**

环形缓存区（数据从outputCollector中传入环形缓存区，直到达到80%的缓存时，缓存才会启用清理机制，将已经溢出的数据溢出到文件中去（通过spiller来将数据溢出到文件中去））会溢出多次，每次溢出都会对数据进行分区排序，形成多个分区排序后的数据，最终进行合并。
combiner的作用：对spiller阶段的溢出数据进行一个reduce处理，直接让相同k的value值相加，减少数据量以及传输过程中的开销，大大提高效率。（根据业务需求使用，并不是每个业务都要用。可自定义一个Combiner类，内部逻辑和Reduce类似）
shuffle:洗牌、发牌——（核心机制：数据分区，排序，缓存） 
具体来说：就是将maptask输出的处理结果数据，分发给reducetask，并在分发的过程中，对数据按key进行了分区和排序；

数据倾斜：指的是任务在shuffle阶段时会进行一个分区操作（默认的是hashcode取模），如果有大部分数据被分到一个ReduceTask端进行处理，一小部分任务被分到其他的ReduceTask端进行处理，就会造成其他ReduceTask处理完成后，仍有一个ReduceTask还在处理数据。最终造成整个工程延迟的情况。（为了解决这个问题，引入了Partition）

**这里主要实现了2、3、5三个组件除此之外还实现了自定义Combiner，map端join和reduce端join功能、以及自定义key和val功能等等。每个包下对应的都是一个完整的小Demo，对应的都有相应的需求，也把注释写的很清楚，后期遇到问题有要用到其他的组件会再对代码做补充，希望对各位有所帮助。如有不足或错误请大家批评指正。**
