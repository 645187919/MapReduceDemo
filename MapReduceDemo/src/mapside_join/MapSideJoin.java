package mapside_join;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/*
MapReduce核心思想：解决一个问题时，主要考虑用什么做key，key相同的会到同一个reduce下。什么做value
实现两个“表”的join操作，其中一个表数据量小，一个表很大，（数据倾斜在Reduce端经常见）这种场景在实际中非常常见，
比如“订单日志” join “产品信息”。
		1.2 分析（避免数据倾斜 ）
		--原理阐述
		适用于关联表中有小表的情形；
		可以将小表分发到所有的map节点，这样，map节点就可以在本地对自己所读到的大表数据进行join并输出最终结果
		可以大大提高join操作的并发度，加快处理速度

		--示例：先在mapper类中预先定义好小表，进行join
		--并用distributedcache机制将小表的数据分发到每一个maptask执行节点，从而每一个maptask节点可以从本地加载到小表的数据，
		进而在本地即可实现join。
*/

public class MapSideJoin {

	public static class MapSideJoinMapper extends Mapper<LongWritable, Text, Text, NullWritable> {
		// 用一个hashmap来加载保存产品信息表
		Map<String, String> pdInfoMap = new HashMap<String, String>();

		Text k = new Text();

		/**
		 * 通过阅读父类Mapper的源码，发现 setup方法是在maptask处理数据之前调用， 可以用来做一些初始化工作
		 * (在map处理数据前调用，利用distributedcache机制直接将小表加载到hashmap中存放到各个maptask中)。
		 */
		@Override
		protected void setup(Context context) throws IOException, InterruptedException {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("pdts.txt")));
			String line;
			while (StringUtils.isNotEmpty(line = br.readLine())) {
				String[] fields = line.split(",");
				pdInfoMap.put(fields[0], fields[1]);
			}
			br.close();
		}

		// 由于已经持有完整的产品信息表，所以在map方法中就能实现join逻辑了
		@Override
		protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String orderLine = value.toString();
			String[] fields = orderLine.split("\t");
			String pdName = pdInfoMap.get(fields[1]);
			k.set(orderLine + "\t" + pdName);
			context.write(k, NullWritable.get());
		}

	}

	public static void main(String[] args) throws Exception {

		Configuration conf = new Configuration();

		Job job = Job.getInstance(conf);

		job.setJarByClass(MapSideJoin.class);

		job.setMapperClass(MapSideJoinMapper.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(NullWritable.class);

		FileInputFormat.setInputPaths(job, new Path("D:/srcdata/mapjoininput"));
		FileOutputFormat.setOutputPath(job, new Path("D:/temp/output"));

		// 指定需要缓存一个文件到所有的maptask运行节点工作目录
		/* job.addArchiveToClassPath(archive); */// 缓存jar包到task运行节点的classpath中
		/* job.addFileToClassPath(file); */// 缓存普通文件到task运行节点的classpath中
		/* job.addCacheArchive(uri); */// 缓存压缩包文件到task运行节点的工作目录
		/* job.addCacheFile(uri) */// 缓存普通文件到task运行节点的工作目录

		// 将产品表文件缓存到task工作节点的工作目录中去
		job.addCacheFile(new URI("file:/D:/srcdata/mapjoincache/pdts.txt"));

		//map端join的逻辑不需要reduce阶段，设置reducetask数量为0
		job.setNumReduceTasks(0);
		
		boolean res = job.waitForCompletion(true);
		System.exit(res ? 0 : 1);

	}

}
