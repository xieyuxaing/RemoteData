package DataAn.jfreechart.thread;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RecursiveTask;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import com.mongodb.client.MongoCursor;
import DataAn.common.utils.DateUtil;
import DataAn.jfreechart.dto.LineMapDto;
import DataAn.jfreechart.dto.LineTimeSeriesDto;
import DataAn.mongo.client.MongodbUtil;

/**
 * 多线程获取mongodb数据:LineTimeSeriesDto[] arrayData
 *
 */
public class SearchByDayDoneTask1 extends RecursiveTask<LineMapDto>{

	private static final long serialVersionUID = 1L;
	
	private LineTimeSeriesDto[] arrayData;
	private String databaseName;
	private String collectionName;
	private Date beginDate0;
	private Date beginDate;
	private Date endDate;
	private Map<String, Double> paramMinMap;
	private Map<String, Double> paramMaxMap;
	private Map<String, String> paramsMap;	

	public SearchByDayDoneTask1(LineTimeSeriesDto[] arrayData,
			String databaseName, String collectionName,Date beginDate0,
			Date beginDate, Date endDate, Map<String, Double> paramMinMap,
			Map<String, Double> paramMaxMap, Map<String, String> paramsMap) {
		super();
		this.arrayData = arrayData;
		this.databaseName = databaseName;
		this.collectionName = collectionName;
		this.beginDate0 = beginDate0;
		this.beginDate = beginDate;
		this.endDate = endDate;
		this.paramMinMap = paramMinMap;
		this.paramMaxMap = paramMaxMap;
		this.paramsMap = paramsMap;
	}

	@Override
	protected LineMapDto compute() {
		long begin = System.currentTimeMillis();
		
		MongodbUtil mg = MongodbUtil.getInstance();
		int index = 0;
		if(beginDate0.before(beginDate)){
			index = (int) mg.countByDate(databaseName, collectionName, beginDate0, beginDate);//计数器
		}
		
		System.out.println(DateUtil.format(beginDate) + " 到 "+ DateUtil.format(endDate) + " begin get mongodb data ...index: " + index);
				
		Map<String, Double> minMap = new HashMap<String, Double>();
		Map<String, Double> maxMap = new HashMap<String, Double>();
		//参数集
		Set<String> en_params = paramsMap.keySet();
		Double min = null;
		Double max = null;
		
		//查询mongodb数据集
		MongoCursor<Document> cursor = mg.find(databaseName, collectionName, beginDate, endDate);
		if(cursor == null){
			throw new RuntimeException(DateUtil.format(beginDate) + " 到 "+ DateUtil.format(endDate) +" 未找到报告数据！");
		}
		int count = 0;// 迭代器
		Map<String,Double> dataMap = null;
		LineTimeSeriesDto lineTimeSeriesDto = null;
		Document doc = null;
		Date datetime = null;
		long lastTime = 0; //上一个时间截
		long nextTime = 0; //下一个时间截
		int second_count = 0; //秒级数据集的个数
		while (cursor.hasNext()) {
			lineTimeSeriesDto = new LineTimeSeriesDto();
			
			doc = cursor.next();
			nextTime = doc.getDate("datetime").getTime();
			//如果这次的时间截跟上次的时间截不相等
			if(nextTime != lastTime){
				second_count = 0;
				lastTime = nextTime;
			}else{
				nextTime = nextTime + (second_count * 100);
				second_count ++;
			}
			datetime = new Date(nextTime);
			dataMap = new HashMap<String,Double>();			
			for (String key : en_params) {
				String strValue = doc.getString(key);
				if(StringUtils.isNotBlank(strValue)){
					
					// 转换为double 类型
					double dValue = Double.parseDouble(strValue.trim());
					
					//在有效值区间之内
					if(paramMaxMap.get(key) != null && dValue > paramMaxMap.get(key))
						continue;
					
					if(paramMinMap.get(key) != null && dValue < paramMinMap.get(key))
						continue;
					
					// 往Map里面添加数据
					dataMap.put(key, dValue);
					
					// 获取最小值
					min = minMap.get(key);
					if (min == null) {
						min = dValue;
					}
					minMap.put(key, this.getMin(min, dValue));
					// 获取最大值
					max = maxMap.get(key);
					if (max == null) {
						max = dValue;
					}
					maxMap.put(key, this.getMax(max, dValue));
				}
			}
			lineTimeSeriesDto.setDatetime(datetime);
			lineTimeSeriesDto.setParam(dataMap);
			arrayData[index+count]=	lineTimeSeriesDto;
			count++;
		}
		if(count == 0){
			throw new RuntimeException(DateUtil.format(beginDate) + " 到 "+ DateUtil.format(endDate) + " 报告数据记录数为：" + count);
		}
		LineMapDto lineMapDto = new LineMapDto();
		lineMapDto.setMinMap(minMap);
		lineMapDto.setMaxMap(maxMap);
		lineMapDto.setIndex(index);
		lineMapDto.setCount(count);
		
		long end_getData = System.currentTimeMillis();		
		System.out.println(DateUtil.format(beginDate) + " 到 "+ DateUtil.format(endDate) + " get mongodb data count: " + count);
		System.out.println("getData time: " + (end_getData - begin));
		return lineMapDto;
	}
	
	protected double getMax(double data1, double data2) {
		if (data1 >= data2)
			return data1;
		return data2;
	}

	protected double getMin(double data1, double data2) {
		if (data1 <= data2)
			return data1;
		return data2;
	}
}
