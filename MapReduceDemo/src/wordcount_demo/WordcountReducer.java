package wordcount_demo;
import java.io.IOException;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
/**
 //生命周期：框架每传递进来一个k相同的value 组，reduce方法就被调用一次
 * KEYIN, VALUEIN 对应  mapper输出的KEYOUT,VALUEOUT类型对应
 * KEYOUT, VALUEOUT 是自定义reduce逻辑处理结果的输出数据类型
 * KEYOUT是单词
 * VLAUEOUT是总次数
 */
public class WordcountReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
	/**
	 * <angelababy,1><angelababy,1><angelababy,1><angelababy,1><angelababy,1>
	 * <hello,1><hello,1><hello,1><hello,1><hello,1><hello,1>
	 * <banana,1><banana,1><banana,1><banana,1><banana,1><banana,1>
	 * 入参key，是一组相同单词kv对的key
	 */
	@Override
	protected void reduce(Text key, Iterable<IntWritable> values,
						  Context context) throws IOException, InterruptedException {
	//count不能声明到外面，每次调用时是处理不同的单词
		int count=0;
		for(IntWritable value:values){
			count+=value.get();
		}
		context.write(key, new IntWritable(count));
	}
}