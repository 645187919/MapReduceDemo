package overwrite_partitioner;

import java.util.HashMap;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Partitioner;

/**
 * K2  V2  对应的是map输出kv的类型
 * @author
 *
 */
public class ProvincePartitioner extends Partitioner<Text, FlowBean>{

	public static HashMap<String, Integer> proviceDict = new HashMap<String, Integer>();
	//将该部分业务放在内存处理，增加处理速度
	static{
		proviceDict.put("136", 0);
		proviceDict.put("137", 1);
		proviceDict.put("138", 2);
		proviceDict.put("139", 3);
	}
	
	
	
	@Override
	public int getPartition(Text key, FlowBean value, int numPartitions) {
		String prefix = key.toString().substring(0, 3);
		Integer provinceId = proviceDict.get(prefix);
		
		return provinceId==null?4:provinceId;
	}



}
