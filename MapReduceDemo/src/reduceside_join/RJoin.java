package reduceside_join;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;


/*
需求：将以下两张表合并在一起
		订单数据表t_order：
		2、实现机制：
		通过将关联的条件作为map输出的key，将两表满足join条件的数据并携带数据来源的文件信息，
		发往同一个reduce task，在reduce中进行数据的串联
		/**
 * 订单表和商品表合一?
order.txt(订单id, 日期, 商品编号, 数量)
	1001	20150710	P0001	2
	1002	20150710	P0001	3
	1002	20150710	P0002	3
	1003	20150710	P0003	3
product.txt(商品编号, 商品名字, 价格, 数量)
	P0001	小米5	1001	2
	P0002	锤子T1	1000	3
	P0003	锤子	1002	4
 */

		*/
public class RJoin {

	static class RJoinMapper extends Mapper<LongWritable, Text, Text, InfoBean> {
		InfoBean bean = new InfoBean();
		Text k = new Text();

		@Override
		protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

			String line = value.toString();

			FileSplit inputSplit = (FileSplit) context.getInputSplit();
			String name = inputSplit.getPath().getName();
			// 通过文件名判断是哪种数据
			String pid = "";
			if (name.startsWith("order")) {
				String[] fields = line.split(",");
				// id date pid amount
				pid = fields[2];
				//bean对象包含订单和产品的所有属性，这里只是添加了对应的订单属性
				bean.set(Integer.parseInt(fields[0]), fields[1], pid, Integer.parseInt(fields[3]), "", 0, 0, "0");

			} else {
				String[] fields = line.split(",");
				// id pname category_id price
				pid = fields[0];
				//bean对象包含订单和产品的所有属性，这里只是添加了对应的产品属性
				bean.set(0, "", pid, 0, fields[1], Integer.parseInt(fields[2]), Float.parseFloat(fields[3]), "1");

			}
			k.set(pid);
			context.write(k, bean);
		}

	}

	static class RJoinReducer extends Reducer<Text, InfoBean, InfoBean, NullWritable> {
		
		@Override
		protected void reduce(Text pid, Iterable<InfoBean> beans, Context context) throws IOException, InterruptedException {
			
			InfoBean pdBean = new InfoBean();
			ArrayList<InfoBean> orderBeans = new ArrayList<InfoBean>();

			for (InfoBean bean : beans) {
				if ("1".equals(bean.getFlag())) {
					try {
						BeanUtils.copyProperties(pdBean, bean);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					InfoBean odbean = new InfoBean();
					try {
						BeanUtils.copyProperties(odbean, bean);
						orderBeans.add(odbean);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			}

			// 拼接两类数据形成最终结果
			for (InfoBean bean : orderBeans) {

				bean.setPname(pdBean.getPname());
				bean.setCategory_id(pdBean.getCategory_id());
				bean.setPrice(pdBean.getPrice());
//只输出bean，则value用NullWritable来填充
				context.write(bean, NullWritable.get());
			}

		}

	}

	public static void main(String[] args) throws Exception {
		
		args = new String[]{"D:/srcdata/wgj/", "D:/temp/out3"};
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf);

		// 指定本程序的jar包所在的本地路径
		// job.setJarByClass(RJoin.class);
//		job.setJar("c:/join.jar");

		// 指定本业务job要使用的mapper/Reducer业务类
		job.setMapperClass(RJoinMapper.class);
		job.setReducerClass(RJoinReducer.class);

		// 指定mapper输出数据的kv类型
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(InfoBean.class);

		// 指定最终输出的数据的kv类型
		job.setOutputKeyClass(InfoBean.class);
		job.setOutputValueClass(NullWritable.class);

		// 指定job的输入原始文件所在目录
		FileInputFormat.setInputPaths(job, new Path(args[0]));
		// 指定job的输出结果所在目录
		FileOutputFormat.setOutputPath(job, new Path(args[1]));

		// 将job中配置的相关参数，以及job所用的java类所在的jar包，提交给yarn去运行
		/* job.submit(); */
		boolean res = job.waitForCompletion(true);
		System.exit(res ? 0 : 1);

	}
}
