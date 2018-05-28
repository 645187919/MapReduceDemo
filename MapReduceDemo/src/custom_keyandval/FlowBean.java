package custom_keyandval;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
//1、注意FlowBean在设计的时候，直接重写构造函数，直接传上下流量，生成总流量的技巧，把一些逻辑直接放在Bean对象中来处理。
//2、由于传入自定义Bean对象，这里需要实现Writable接口用于序列换和反序列化Bean对象
public class FlowBean implements WritableComparable<FlowBean>{
	
	private long upFlow;
	private long dFlow;
	private long sumFlow;
	
	//反序列化时，需要反射调用空参构造函数，所以要显示定义一个
	public FlowBean(){}
	
	public FlowBean(long upFlow, long dFlow) {
		this.upFlow = upFlow;
		this.dFlow = dFlow;
		this.sumFlow = upFlow + dFlow;
	}
	
	
	public void set(long upFlow, long dFlow) {
		this.upFlow = upFlow;
		this.dFlow = dFlow;
		this.sumFlow = upFlow + dFlow;
	}
	
	
	
	
	public long getUpFlow() {
		return upFlow;
	}
	public void setUpFlow(long upFlow) {
		this.upFlow = upFlow;
	}
	public long getdFlow() {
		return dFlow;
	}
	public void setdFlow(long dFlow) {
		this.dFlow = dFlow;
	}


	public long getSumFlow() {
		return sumFlow;
	}


	public void setSumFlow(long sumFlow) {
		this.sumFlow = sumFlow;
	}


	/**
	 * 序列化方法，将对象的字段信息写入输出流
	 */
	@Override
	public void write(DataOutput out) throws IOException {
		out.writeLong(upFlow);
		out.writeLong(dFlow);
		out.writeLong(sumFlow);
		
	}


	/**
	 * 反序列化方法
	 * 注意：反序列化的顺序跟序列化的顺序完全一致。从输入流中读取各个字段信息
	 */
	@Override
	public void readFields(DataInput in) throws IOException {
		 upFlow = in.readLong();
		 dFlow = in.readLong();
		 sumFlow = in.readLong();
	}
	//对象写到文本中要调用toString方法，这里就是生成我们写出的数据格式。反序列化时调用。
	@Override
	public String toString() {
		 
		return upFlow + "\t" + dFlow + "\t" + sumFlow;
	}

	@Override
	public int compareTo(FlowBean o) {
		
		return this.sumFlow>o.getSumFlow()?-1:1;
	}

}
