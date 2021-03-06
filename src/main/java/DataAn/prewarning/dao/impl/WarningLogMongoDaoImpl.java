package DataAn.prewarning.dao.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import DataAn.common.dao.Pager;
import DataAn.common.utils.DateUtil;
import DataAn.common.utils.Log4jUtil;
import DataAn.galaxyManager.dao.ISeriesDao;
import DataAn.galaxyManager.dao.IStarDao;
import DataAn.galaxyManager.domain.Series;
import DataAn.galaxyManager.domain.Star;
import DataAn.galaxyManager.service.IParameterService;
import DataAn.galaxyManager.service.impl.ParameterServiceImpl;
import DataAn.mongo.client.MongodbUtil;
import DataAn.mongo.init.InitMongo;
import DataAn.prewarning.dao.IWarningLogMongoDao;
import DataAn.prewarning.dto.QueryLogDTO;
import DataAn.routing.DataSearchTask;

@Repository
public class WarningLogMongoDaoImpl implements IWarningLogMongoDao {
	@Resource
	private ISeriesDao seriersDao;
	@Resource
	private IStarDao starDao;
	@Resource
	private IParameterService ParameterService;
	@Override
	public void deleteWainingById(String logId, String series, String star, String parameterType, String warningType ,String hadRead) {
		String databaseName = InitMongo.getDataDBBySeriesAndStar(series, star);
		String collectionName = getCollectionName(parameterType, warningType);
		if(hadRead.equals("1"))
		{
			MongodbUtil.getInstance().deleteMany(databaseName, collectionName, "_id", new ObjectId(logId));
		}else if(hadRead.equals("0"))
		{
			MongoCollection<Document> collection = MongodbUtil.getInstance().getCollectionNotShard(databaseName, collectionName);
			if (collection != null) {
				collection.updateMany(Filters.eq("_id", new ObjectId(logId)), Updates.set("hadRead", "1"));
			}
		}
		
	}
	@Override
	public void readAllWarningLog(){
		List<String> dbNames = new ArrayList<String>();
		List<Series> seriesList = new ArrayList<Series>();
		seriesList = seriersDao.findAll();
		for (Series s : seriesList) {
			List<Star> starList = null;
			starList = starDao.getStarListBySeriesId(s.getId());
			for (Star str : starList) {
				String db = InitMongo.getDataDBBySeriesAndStar(s.getCode(), str.getCode());
				dbNames.add(db);
			}
		}
		MongodbUtil mongodbUtil = MongodbUtil.getInstance(); 
		for (String databaseName : dbNames) {
			MongoCollection<Document> collection1 = mongodbUtil.getCollectionNotShard(databaseName,"top_job");
			if (collection1 != null) {
				collection1.updateMany(Filters.eq("hadRead", "0"), Updates.set("hadRead", "1"));
			}
			MongoCollection<Document> collection2 = mongodbUtil.getCollectionNotShard(databaseName,"top_exception");
			if (collection2 != null) {
				collection2.updateMany(Filters.eq("hadRead", "0"), Updates.set("hadRead", "1"));
			}			
			MongoCollection<Document> collection3 = mongodbUtil.getCollectionNotShard(databaseName,"flywheel_job");
			if (collection3 != null) {
				collection3.updateMany(Filters.eq("hadRead", "0"), Updates.set("hadRead", "1"));
			}			
			MongoCollection<Document> collection4 = mongodbUtil.getCollectionNotShard(databaseName,"flywheel_exception");
			if (collection4 != null) {
				collection4.updateMany(Filters.eq("hadRead", "0"), Updates.set("hadRead", "1"));
			}
		}
	
	}

	@Override
	public Long getNotReadCount(String series, String star, String parameterType, String parameter,
			String warningType) {
		List<String> databaseNames = getDBsBySeriersAndStar(series, star);
		MongodbUtil mongodbUtil = MongodbUtil.getInstance();
		Long sum = 0l;
		if (StringUtils.isNotBlank(parameterType) && StringUtils.isNotBlank(warningType)) {
			String collectionName = getCollectionName(parameterType, warningType);
			for (String databaseName : databaseNames) {
				MongoCollection<Document> collection = mongodbUtil.getCollectionNotShard(databaseName, collectionName);
				if (collection != null) {
					sum += collection.count(Filters.and(Filters.eq("status", 1),Filters.eq("hadRead", "0")));
				}
			}
		} else if (StringUtils.isNotBlank(parameterType) && StringUtils.isBlank(warningType)) {
			for (String databaseName : databaseNames) {
				MongoCollection<Document> collection = mongodbUtil.getCollectionNotShard(databaseName,
						parameterType + "_job");
				if (collection != null) {
					sum += collection.count(Filters.and(Filters.eq("status", 1),Filters.eq("hadRead", "0")));
				}

				MongoCollection<Document> collection2 = mongodbUtil.getCollectionNotShard(databaseName,
						parameterType + "_exception");
				if (collection2 != null) {
					sum += collection2.count(Filters.and(Filters.eq("status", 1),Filters.eq("hadRead", "0")));
				}
			}
		} else if (StringUtils.isBlank(parameterType) && StringUtils.isNotBlank(warningType)) {
			String collectionName = "";
			String collectionName2 = "";
			if ("0".equals(warningType)) {
				collectionName = "flywheel_job";
				collectionName2 = "top_job";
			} else if ("1".equals(warningType)) {
				collectionName = "flywheel_exception";
				collectionName2 = "top_exception";
			}
			for (String databaseName : databaseNames) {
				MongoCollection<Document> collection = mongodbUtil.getCollectionNotShard(databaseName, collectionName);
				if (collection != null) {
					sum += collection.count(Filters.and(Filters.eq("status", 1),Filters.eq("hadRead", "0")));
				}

				MongoCollection<Document> collection2 = mongodbUtil.getCollectionNotShard(databaseName,
						collectionName2);
				if (collection2 != null) {
					sum += collection2.count(Filters.and(Filters.eq("status", 1),Filters.eq("hadRead", "0")));
				}
			}
		} else {
			for (String databaseName : databaseNames) {
				MongoCollection<Document> collection = mongodbUtil.getCollectionNotShard(databaseName,
						"flywheel_job");
				if (collection != null) {
					sum += collection.count(Filters.and(Filters.eq("status", 1),Filters.eq("hadRead", "0")));
				}
				MongoCollection<Document> collection2 = mongodbUtil.getCollectionNotShard(databaseName,
						"top_job");
				if (collection2 != null) {
					sum += collection2.count(Filters.and(Filters.eq("status", 1),Filters.eq("hadRead", "0")));
				}
				MongoCollection<Document> collection3 = mongodbUtil.getCollectionNotShard(databaseName,
						"flywheel_exception");
				if (collection3 != null) {
					sum += collection3.count(Filters.and(Filters.eq("status", 1),Filters.eq("hadRead", "0")));
				}
				MongoCollection<Document> collection4 = mongodbUtil.getCollectionNotShard(databaseName,
						"top_exception");
				if (collection4 != null) {
					sum += collection4.count(Filters.and(Filters.eq("status", 1),Filters.eq("hadRead", "0")));
				}
			}
		}
		return sum;
	}

	private String getCollectionName(String parameterType, String warningType) {
		String collectionName = "";
		if ("0".equals(warningType)) {
			collectionName = parameterType + "_job";
		} else if ("1".equals(warningType)) {
			collectionName = parameterType + "_exception";
		}
		return collectionName;
	}

	private String getParameterKey(String warningType){
		String parameterKey = "";
		if ("0".equals(warningType)) {
			parameterKey = "deviceName";
		} else if ("1".equals(warningType)) {
			parameterKey = "paramCode";
		}
		return parameterKey;
	}
	private List<QueryLogDTO> getWarningLogListByCollection(MongoCollection<Document> collection, String warnType) {
		List<QueryLogDTO> queryLogDTOs = new ArrayList<QueryLogDTO>();
		if (collection == null) {
			return queryLogDTOs;
		}
		//FindIterable<Document> document_It = collection.find(Filters.and(Filters.eq("hadRead", "0"),Filters.eq("status", 1))).sort(Filters.eq("_recordtime",-1));
		FindIterable<Document> document_It = collection.find(Filters.and(Filters.eq("hadRead", "0"),Filters.eq("status", 1))).limit(1000);
		
		/*String res;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long lt = System.currentTimeMillis();
        Date date = new Date(lt);
        res = simpleDateFormat.format(date);
		System.out.println("开始迭代:"+res);
		System.out.println(collection.count(Filters.and(Filters.eq("status", 1),Filters.eq("hadRead", "0"))));*/
		for (Document doc : document_It) {
			String deviceorparamname="";
			String value="";
			String timevalue=DateUtil.formatSSS(doc.getDate("datetime"));
			try{
				String jobbegintime=doc.getString("beginDate");
				String jobendtime=doc.getString("endDate");
				if(warnType.equals("0"))//若果是特殊工况则说明字段显示起止时间
				{
					deviceorparamname=doc.getString("deviceName");
					value="起止时间："+jobbegintime+"--"+jobendtime;
				}else if(warnType.equals("1"))//若果是异常则说明字段显示异常值
				{
					
					String paramCode=doc.getString("paramCode");
					String paramname=ParameterService.getParameter_simpleZh_by_en(doc.getString("series"),doc.getString("star"),doc.getString("deviceType"),paramCode);
					deviceorparamname=paramname;
					value="异常参数值："+doc.getString("paramValue");
				}
			}catch(Exception e){
				Log4jUtil.getInstance().getLogger(WarningLogMongoDaoImpl.class).error("从"
						+ "monogodb中查询信息出错，可能是从数据库中查到的字段转换类型时出错");
			}			
			QueryLogDTO warningLog = new QueryLogDTO();
			warningLog.setLogId(doc.getObjectId("_id").toString());
			//warningLog.setParameter(doc.getString("deviceName")+doc.getString("paramName")+doc.getString("paramCode"));
			warningLog.setParameter(deviceorparamname);
			warningLog.setParameterType(doc.getString("deviceType"));
			//warningLog.setParamValue(Double.parseDouble(doc.getString("value")));
			warningLog.setParamValue(value);
			warningLog.setSeries(doc.getString("series"));
			warningLog.setStar(doc.getString("star"));
			//warningLog.setTimeValue(DateUtil.formatSSS(doc.getDate("datetime")));
			warningLog.setTimeValue(timevalue);
			warningLog.setWarningType(warnType);
			warningLog.setRecordtime(doc.getString("_recordtime"));
			queryLogDTOs.add(warningLog);
		}
		
		/*String res2;
        SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long lt2 = System.currentTimeMillis();
        Date date2 = new Date(lt2);
        res2 = simpleDateFormat2.format(date2);
		System.out.println("结束迭代:"+res2);*/
		return queryLogDTOs;
	}

	@Override
	public List<QueryLogDTO> getQueryLogDTOs() {
		MongodbUtil mongodbUtil = MongodbUtil.getInstance();
		List<String> databaseNames = getDBsBySeriersAndStar("", "");
		List<QueryLogDTO> queryLogAllDTOs = new ArrayList<QueryLogDTO>();
		for (String databaseName : databaseNames) {
			queryLogAllDTOs.addAll(getWarningLogListByCollection(
					mongodbUtil.getCollectionNotShard(databaseName, "flywheel_job"), "0"));
			queryLogAllDTOs.addAll(getWarningLogListByCollection(
					mongodbUtil.getCollectionNotShard(databaseName, "top_job"), "0"));
			queryLogAllDTOs.addAll(getWarningLogListByCollection(
					mongodbUtil.getCollectionNotShard(databaseName, "flywheel_exception"), "1"));
			queryLogAllDTOs.addAll(getWarningLogListByCollection(
					mongodbUtil.getCollectionNotShard(databaseName, "top_exception"), "1"));
		}
		return queryLogAllDTOs;
	}
	

	private void updateHadRead(QueryLogDTO queryLogDTO) {
		MongodbUtil mongodbUtil = MongodbUtil.getInstance();
		String databaseName = InitMongo.getDataDBBySeriesAndStar(queryLogDTO.getSeries(), queryLogDTO.getStar());
		String collectionName = getCollectionName(queryLogDTO.getParameterType(), queryLogDTO.getWarningType());
		MongoCollection<Document> collection = mongodbUtil.getCollectionNotShard(databaseName, collectionName);
		if (collection != null) {
			collection.updateMany(Filters.eq("_id", new ObjectId(queryLogDTO.getLogId())), Updates.set("hadRead", "1"));
		}
	}

	@Override
	public Pager<QueryLogDTO> selectByOption(int pageIndex, int pageSize, String series, String star,
			String parameterType, String parameter, String createdatetimeStart, String createdatetimeEnd,
			String warningType, String hadRead) {
		MongodbUtil mongodbUtil = MongodbUtil.getInstance();
		List<QueryLogDTO> queryLogAllDTOs = new ArrayList<QueryLogDTO>();
		List<QueryLogDTO> queryLogResultDTOs = new ArrayList<QueryLogDTO>();
		if ("0".equals(hadRead)) {
			List<String> databaseNames = getDBsBySeriersAndStar(series, star);
			for (String databaseName : databaseNames) {
				queryLogAllDTOs.addAll(getWarningLogListByCollection(
						mongodbUtil.getCollectionNotShard(databaseName, "flywheel_job"), "0"));
				queryLogAllDTOs.addAll(getWarningLogListByCollection(
						mongodbUtil.getCollectionNotShard(databaseName, "top_job"), "0"));
				queryLogAllDTOs.addAll(getWarningLogListByCollection(
						mongodbUtil.getCollectionNotShard(databaseName, "flywheel_exception"), "1"));
				queryLogAllDTOs.addAll(getWarningLogListByCollection(
						mongodbUtil.getCollectionNotShard(databaseName, "top_exception"), "1"));
			}
			/*if (queryLogAllDTOs.size() < pageSize) {
				for (int i = 0; i < queryLogAllDTOs.size(); i++) {
					QueryLogDTO queryLogDTO = queryLogAllDTOs.get(i);
					//updateHadRead(queryLogDTO);
					queryLogResultDTOs.add(queryLogDTO);
				}
			} else {
				for (int i = 0; i < pageSize; i++) {
					QueryLogDTO queryLogDTO = queryLogAllDTOs.get(i);
					//updateHadRead(queryLogDTO);
					queryLogResultDTOs.add(queryLogDTO);
				}
			}*/
			
			
			String res;
	        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	        long lt = System.currentTimeMillis();
	        Date date = new Date(lt);
	        res = simpleDateFormat.format(date);
			System.out.println("-----------开始排序:"+res);
			//获取未读的预警信息时根据时间对记录排序
			Collections.sort(queryLogAllDTOs, new Comparator<QueryLogDTO>(){
				public int compare(QueryLogDTO o1,QueryLogDTO o2){
					Date o1date = DateUtil.format(o1.getRecordtime(),"yyyy-MM-dd HH:mm:ss");
					Date o2date = DateUtil.format(o2.getRecordtime(),"yyyy-MM-dd HH:mm:ss");
					
					if(o1date.equals(o2date)){
						return 0;
					}
					if(o1date.before(o2date)){
						return 1;
					}
					if(o1date.after(o2date)){
						return -1;
					}
					return 0;
				}
			});
			
			String res2;
	        SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	        long lt2 = System.currentTimeMillis();
	        Date date2 = new Date(lt2);
	        res2 = simpleDateFormat2.format(date2);
			System.out.println("--------------结束排序:"+res2);
			//获取未读的预警信息时不对预警信息进行分页
			for (int i = 0; i < queryLogAllDTOs.size(); i++) {
				QueryLogDTO queryLogDTO = queryLogAllDTOs.get(i);
				//updateHadRead(queryLogDTO);
				queryLogResultDTOs.add(queryLogDTO);
			}
			Pager<QueryLogDTO> pager = new Pager<QueryLogDTO>(pageSize, pageIndex, queryLogAllDTOs.size(),
					queryLogResultDTOs);
			return pager;
		} else {
			String databaseName = InitMongo.getDataDBBySeriesAndStar(series, star);
			String collectionName = getCollectionName(parameterType, warningType);
			MongoCollection<Document> collection = mongodbUtil.getCollectionNotShard(databaseName, collectionName);
			FindIterable<Document> document_It = null;
			Long totalCount = 0l;
			List<QueryLogDTO> queryLogDTOs = new ArrayList<QueryLogDTO>();
			if (collection == null) {
				Pager<QueryLogDTO> pager = new Pager<QueryLogDTO>(pageSize, pageIndex, totalCount, queryLogDTOs);
				return pager;
			} else {
				if (StringUtils.isNotBlank(createdatetimeStart) && StringUtils.isNotBlank(createdatetimeEnd)
						&& StringUtils.isNotBlank(parameter)) {
					String paramKey = getParameterKey(warningType);
					document_It = collection
							.find(Filters.and(Filters.eq(paramKey, parameter),
									Filters.eq("status", 1),
									Filters.gte("datetime", DateUtil.format(createdatetimeStart)),
									Filters.lte("datetime", DateUtil.format(createdatetimeEnd))))
							.sort(Filters.eq("datetime", -1)).skip((pageIndex - 1) * pageSize).limit(pageSize);
					totalCount = collection.count(Filters.and(Filters.eq(paramKey, parameter),Filters.eq("status", 1),
							Filters.gte("datetime", DateUtil.format(createdatetimeStart)),
							Filters.lte("datetime", DateUtil.format(createdatetimeEnd))));

				}
				if (StringUtils.isNotBlank(createdatetimeStart) && StringUtils.isNotBlank(createdatetimeEnd)
						&& StringUtils.isBlank(parameter)) {
					document_It = collection
							.find(Filters.and(Filters.gte("datetime", DateUtil.format(createdatetimeStart)),
									Filters.lte("datetime", DateUtil.format(createdatetimeEnd)),Filters.eq("status", 1)))
							.sort(Filters.eq("datetime", -1)).skip((pageIndex - 1) * pageSize).limit(pageSize);
					totalCount = collection
							.count(Filters.and(Filters.eq("status", 1),Filters.gte("datetime", DateUtil.format(createdatetimeStart)),
									Filters.lte("datetime", DateUtil.format(createdatetimeEnd))));
				}
				if (StringUtils.isNotBlank(createdatetimeStart) && StringUtils.isBlank(createdatetimeEnd)
						&& StringUtils.isNotBlank(parameter)) {
					String paramKey = getParameterKey(warningType);
					document_It = collection
							.find(Filters.and(Filters.eq(paramKey, parameter),
									Filters.eq("status", 1),
									Filters.gte("datetime", createdatetimeStart)))
							.sort(Filters.eq("datetime", -1)).skip((pageIndex - 1) * pageSize).limit(pageSize);
					totalCount = collection.count(Filters.and(Filters.eq(paramKey, parameter),Filters.eq("status", 1),
							Filters.gte("datetime", DateUtil.format(createdatetimeStart))));
				}
				if (StringUtils.isBlank(createdatetimeStart) && StringUtils.isNotBlank(createdatetimeEnd)
						&& StringUtils.isNotBlank(parameter)) {
					String paramKey = getParameterKey(warningType);
					document_It = collection
							.find(Filters.and(Filters.eq(paramKey, parameter),Filters.eq("status", 1),
									Filters.lte("datetime", DateUtil.format(createdatetimeEnd))))
							.sort(Filters.eq("datetime", -1)).skip((pageIndex - 1) * pageSize).limit(pageSize);
					totalCount = collection.count(Filters.and(Filters.eq(paramKey, parameter),Filters.eq("status", 1),
							Filters.lte("datetime", DateUtil.format(createdatetimeEnd))));
				}
				if (StringUtils.isBlank(createdatetimeStart) && StringUtils.isBlank(createdatetimeEnd)
						&& StringUtils.isNotBlank(parameter)) {
					String paramKey = getParameterKey(warningType);
					document_It = collection.find(Filters.and(Filters.eq("status", 1),Filters.eq(paramKey, parameter))).sort(Filters.eq("datetime", -1))
							.skip((pageIndex - 1) * pageSize).limit(pageSize);
					totalCount = collection.count(Filters.and(Filters.eq(paramKey, parameter),Filters.eq("status", 1)));
				}
				if (StringUtils.isBlank(createdatetimeStart) && StringUtils.isNotBlank(createdatetimeEnd)
						&& StringUtils.isBlank(parameter)) {
					document_It = collection.find(Filters.and(Filters.eq("status", 1),Filters.lte("datetime", DateUtil.format(createdatetimeEnd))))
							.sort(Filters.eq("datetime", -1)).skip((pageIndex - 1) * pageSize).limit(pageSize);
					totalCount = collection.count(Filters.and(Filters.eq("status", 1),Filters.lte("datetime", DateUtil.format(createdatetimeEnd))));
				}
				if (StringUtils.isNotBlank(createdatetimeStart) && StringUtils.isBlank(createdatetimeEnd)
						&& StringUtils.isBlank(parameter)) {
					document_It = collection.find(Filters.and(Filters.eq("status", 1),Filters.gte("datetime", DateUtil.format(createdatetimeStart))))
							.sort(Filters.eq("datetime", -1)).skip((pageIndex - 1) * pageSize).limit(pageSize);
					totalCount = collection.count(Filters.and(Filters.eq("status", 1),Filters.gte("datetime", DateUtil.format(createdatetimeStart))));
				}
				if (StringUtils.isBlank(createdatetimeStart) && StringUtils.isBlank(createdatetimeEnd)
						&& StringUtils.isBlank(parameter)) {
					document_It = collection.find(Filters.eq("status", 1)).sort(Filters.eq("datetime", -1)).skip((pageIndex - 1) * pageSize)
							.limit(pageSize);
					totalCount = collection.count(Filters.eq("status", 1));
					
				}

				for (Document doc : document_It) {
					if(StringUtils.isNotBlank(parameter) && warningType.equals("0")){
						String paramKey = getParameterKey(warningType);
						String param = doc.getString(paramKey);
						if(StringUtils.isBlank(param) || (!param.equals(parameter)))
							continue;						
					}
					
					String value="";
					String timevalue=DateUtil.formatSSS(doc.getDate("datetime"));
					try{
						String jobbegintime=doc.getString("beginDate");
						String jobendtime=doc.getString("endDate");
						if(warningType.equals("0"))//若果是特殊工况则说明字段显示起止时间
						{
							value="起止时间："+jobbegintime+"--"+jobendtime;
						}else if(warningType.equals("1"))//若果是异常则说明字段显示异常值
						{
							value="异常参数值："+doc.getString("value");
						}
					}catch(Exception e){
						Log4jUtil.getInstance().getLogger(WarningLogMongoDaoImpl.class).error("从"
								+ "monogodb中查询信息出错，可能是从数据库中查到的字段转换类型时出错");
					}
																	
					QueryLogDTO warningLog = new QueryLogDTO();
					warningLog.setLogId(doc.getObjectId("_id").toString());
					
					if(warningType.equals("0"))//若果是特殊工况则显示设备名称
					{
						warningLog.setParameter(doc.getString("deviceName"));
					}else if(warningType.equals("1"))//若果是异常则显示参数名称
					{
						/*String strParamName = doc.getString("paramName");
						String strParamCode = doc.getString("paramCode");
						warningLog.setParameter(strParamName);
						if(strParamName==null)
						{
							warningLog.setParameter(strParamCode);
						}*/						
						String paramCode=doc.getString("paramCode");
						String paramname=ParameterService.getParameter_simpleZh_by_en(doc.getString("series"),doc.getString("star"),doc.getString("deviceType"),paramCode);
						String deviceorparamname=paramname;
						warningLog.setParameter(deviceorparamname);
						value="异常参数值："+doc.getString("paramValue");
					}
					warningLog.setParameterType(doc.getString("deviceType"));
					warningLog.setParamValue(value);					
					warningLog.setSeries(doc.getString("series"));
					warningLog.setStar(doc.getString("star"));
					//warningLog.setTimeValue(DateUtil.formatSSS(doc.getDate("datetime")));
					warningLog.setTimeValue(timevalue);
					warningLog.setWarningType(warningType);
					warningLog.setWarnRemark(doc.getString("remark"));
					warningLog.setRecordtime(doc.getString("_recordtime"));
					queryLogDTOs.add(warningLog);
					
					//collection.updateMany(Filters.eq("_id", doc.getObjectId("_id")), Updates.set("hadRead", "1"));
				}
				Pager<QueryLogDTO> pager = new Pager<QueryLogDTO>(pageSize, pageIndex, totalCount, queryLogDTOs);
				return pager;
			}
		}
	}

	private List<String> getDBsBySeriersAndStar(String series, String star) {
		List<String> dbName = new ArrayList<String>();
		List<Series> seriesList = new ArrayList<Series>();
		if (StringUtils.isBlank(series)) {
			seriesList = seriersDao.findAll();
		} else {
			Series sDomain = seriersDao.selectByCode(series);
			seriesList.add(sDomain);
		}

		for (Series s : seriesList) {
			List<Star> starList = null;
			if (StringUtils.isBlank(star)) {
				starList = starDao.getStarListBySeriesId(s.getId());
			} else {
				starList = starDao.getStarBySeriesIdAndCode(s.getId(), star);
			}
			for (Star str : starList) {
				String db = InitMongo.getDataDBBySeriesAndStar(s.getCode(), str.getCode());
				dbName.add(db);
			}
		}
		return dbName;
	}
}
