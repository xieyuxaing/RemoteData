package DataAn.communicate.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import DataAn.communicate.service.ICommunicateService;
import DataAn.fileSystem.dao.IVirtualFileSystemDao;
import DataAn.fileSystem.domain.VirtualFileSystem;
import DataAn.fileSystem.service.IVirtualFileSystemService;
import DataAn.galaxyManager.service.IParameterService;
import DataAn.prewarning.domain.WarningValue;
import DataAn.prewarning.service.IPrewarningService;
import DataAn.status.option.StatusTrackingType;
import DataAn.status.service.IStatusTrackingService;
import DataAn.storm.exceptioncheck.model.ExceptionJobConfig;
import DataAn.storm.exceptioncheck.model.ExceptionPointConfig;
import DataAn.sys.service.IStormServerService;

@Service
public class CommunicateServiceImpl implements ICommunicateService{

	@Resource
	private IPrewarningService prewarningService;
	@Resource
	private IStatusTrackingService statusTrackingService;
	@Resource
	private IVirtualFileSystemDao fileDao;
	@Resource
	private IParameterService parameterService;
	@Resource
	private IVirtualFileSystemService fileService;
	@Resource
	private IStormServerService stormServerService;
	
	@Override
	public String getExceptionJobConfigList(String series, String star,
			String parameterType) {
		if (StringUtils.isBlank(series) || StringUtils.isBlank(star) || StringUtils.isBlank(parameterType)) {
			System.out.println("请求参数：星系，星，设备类型 不能为空！！！");
			return null;
		}
		//特殊工况参数配置
		List<WarningValue> jobWarningValues = prewarningService.getWarningValueByParams(series, star, null, parameterType, "0");
		if(jobWarningValues != null && jobWarningValues.size() > 0){
			Map<String,String> map = new HashMap<String,String>();
			List<ExceptionJobConfig> jobConfigList = new ArrayList<ExceptionJobConfig>();
			ExceptionJobConfig jobConfig = null;
			for (WarningValue wv : jobWarningValues) {
				String deviceName = parameterService.getParameter_deviceName_by_en(series, star, wv.getParameterType(), wv.getParameter());
				if(StringUtils.isNotBlank(deviceName)){
					jobConfig = new ExceptionJobConfig();
					jobConfig.setDeviceName(deviceName);
					jobConfig.setDeviceType(wv.getParameterType());
					jobConfig.setParamCode(wv.getParameter());
					jobConfig.setMax(wv.getMaxVal());
					jobConfig.setMin(wv.getMinVal());
					jobConfig.setDelayTime(wv.getLimitTimes());//时间单位
					jobConfig.setCount(wv.getTimeZone());
					jobConfigList.add(jobConfig);
				}else{
					throw new RuntimeException(series+"-"+star+"-"+wv.getParameterType()+"-"+wv.getParameter()+" : 找不到设备1");
				}
			}
			map.put("exceptionJobConfig", JSON.toJSONString(jobConfigList));
			//异常参数配置
			List<WarningValue> exceWarningValues = prewarningService.getWarningValueByParams(series, star, null, parameterType, "1");
			if(exceWarningValues != null && exceWarningValues.size() > 0){
				List<ExceptionPointConfig> exceConfigList = new ArrayList<ExceptionPointConfig>();
				ExceptionPointConfig exceConfig = null;
				for (WarningValue ew : exceWarningValues) {
					String deviceName = parameterService.getParameter_deviceName_by_en(series, star, ew.getParameterType(), ew.getParameter());
					if(StringUtils.isNotBlank(deviceName)){
						exceConfig = new ExceptionPointConfig();
						exceConfig.setDeviceType(ew.getParameterType());
						exceConfig.setDeviceName(deviceName);
						exceConfig.setParamCode(ew.getParameter());
						exceConfig.setMax(ew.getMaxVal());
						exceConfig.setMin(ew.getMinVal());
						exceConfig.setDelayTime(ew.getLimitTimes());//时间单位
						exceConfigList.add(exceConfig);						
					}else{
						throw new RuntimeException(series+"-"+star+"-"+ew.getParameterType()+"-"+ew.getParameter()+" : 找不到设备2");
					}
				}
				map.put("exceptionPointConfig", JSON.toJSONString(exceConfigList));
				return JSON.toJSONString(map);
			}
//			return JSON.toJSONString(jobConfigList);
		}
		return null;
	}

	@Override
	public String getExceptionPointConfigList(String series, String star,
			String parameterType) {
		if (StringUtils.isBlank(series) || StringUtils.isBlank(star) || StringUtils.isBlank(parameterType)) {
			System.out.println("请求参数：星系，星，设备类型 不能为空！！！");
			return null;
		}
		//异常参数配置
		List<WarningValue> warningValues = prewarningService.getWarningValueByParams(series, star, null, parameterType, "1");
		if(warningValues != null && warningValues.size() > 0){
			List<ExceptionPointConfig> exceConfigList = new ArrayList<ExceptionPointConfig>();
			ExceptionPointConfig exceConfig = null;
			for (WarningValue wv : warningValues) {
				exceConfig = new ExceptionPointConfig();
				exceConfig.setDeviceType(wv.getParameterType());
				exceConfig.setDeviceName(parameterService.getParameter_deviceName_by_en(series, star, wv.getParameterType(), wv.getParameter()));
				exceConfig.setParamCode(wv.getParameter());
				exceConfig.setMax(wv.getMaxVal());
				exceConfig.setMin(wv.getMinVal());
				exceConfig.setDelayTime(wv.getLimitTimes());//时间单位
				exceConfigList.add(exceConfig);
			}
			return JSON.toJSONString(exceConfigList);
		}
		return null;
	}

	
	@Override
	public String getAllWarnValue() {
		try {
			JSONArray jsonArray = new JSONArray();
			List<WarningValue> warningValues = prewarningService.getWarningValueByParams("", "", "", "", "0");
			for (WarningValue value : warningValues) {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("series", value.getSeries());
				jsonObject.put("star", value.getStar());
				jsonObject.put("parameter", value.getParameter());
				jsonObject.put("parameterType", value.getParameterType());
				jsonObject.put("maxVal", value.getMaxVal());
				jsonObject.put("minVal", value.getMinVal());
				jsonObject.put("timeZone", value.getTimeZone());
				jsonObject.put("limitTimes", value.getLimitTimes());
				jsonArray.add(jsonObject);
			}
			return jsonArray.toJSONString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String getAllErrorValue() {
		try {
			JSONArray jsonArray = new JSONArray();
			List<WarningValue> warningValues = prewarningService.getWarningValueByParams("", "", "", "", "1");
			for (WarningValue value : warningValues) {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("series", value.getSeries());
				jsonObject.put("star", value.getStar());
				jsonObject.put("parameter", value.getParameter());
				jsonObject.put("parameterType", value.getParameterType());
				jsonObject.put("timeZone", value.getTimeZone());
				jsonObject.put("maxVal", value.getMaxVal());
				jsonObject.put("minVal", value.getMinVal());
				jsonArray.add(jsonObject);
			}
			return jsonArray.toJSONString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String getWarnValueByParam(String series, String star,
			String parameterType, String parameter) {
		if (StringUtils.isBlank(series) || StringUtils.isBlank(star) || StringUtils.isBlank(parameterType)) {
			System.out.println("请求参数：星系，星，设备类型 不能为空！！！");
			return null;
		}
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("series", series);
		jsonObject.put("star", star);
		jsonObject.put("device", parameterType);
		try {
			JSONArray jsonArray = new JSONArray();
			List<WarningValue> warningValues = prewarningService.getWarningValueByParams(series, star, parameter, parameterType, "0");
			for (WarningValue value : warningValues) {
				JSONObject jO = new JSONObject();
				jO.put("paramName", value.getParameter());
				jO.put("jobMax", value.getMaxVal());
				jO.put("jobMin", value.getMinVal());
				jO.put("delayTime", value.getTimeZone());
				jO.put("count", value.getLimitTimes());
				jsonArray.add(jO);
			}
			jsonObject.put("parameterInfos", jsonArray);
			return jsonObject.toJSONString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String getErrorValueByParam(String series, String star,
			String parameterType, String parameter) {
		if (StringUtils.isBlank("series") || StringUtils.isBlank("star") || StringUtils.isBlank("parameterType")) {
			return "请求参数：星系，星，设备类型 不能为空！！！";
		}
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("series", series);
		jsonObject.put("star", star);
		jsonObject.put("device", parameterType);
		try {
			JSONArray jsonArray = new JSONArray();
			List<WarningValue> warningValues = prewarningService.getWarningValueByParams(series, star, parameter, parameterType, "1");
			for (WarningValue value : warningValues) {
				JSONObject jO = new JSONObject();
				jO.put("paramName", value.getParameter());
				jO.put("delayTime", value.getTimeZone());
				jO.put("exceptionMax", value.getMaxVal());
				jO.put("exceptionMin", value.getMinVal());
				jsonArray.add(jO);
			}
			jsonObject.put("parameterInfos", jsonArray);
			return jsonObject.toJSONString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	@Transactional
	public String updateStatus(String version, String statusType,
			String exceptionInfo) {
		JSONObject jsonObject = new JSONObject();
		try {
			VirtualFileSystem file = fileDao.selectByFileTypeIsFileAndMongoFSUUId(version);
			if (file != null) {
				
				statusTrackingService.updateStatusTracking(file.getFileName(), statusType, file.getParameterType(),
						exceptionInfo);
				
				//storm 数据处理失败删除文件并更新状态
				//如果出错则删除文件
				if(statusType.equals(StatusTrackingType.FILEUPLOADFAIL)){
					fileService.deleteFileByUUId(version);
				}
				
				jsonObject.put("sucFlag", true);
				return jsonObject.toJSONString();
			} else {
				jsonObject.put("sucFlag", false);
				return jsonObject.toJSONString();
			}
		} catch (Exception e) {
			jsonObject.put("sucFlag", false);
			e.printStackTrace();
		}		
		return jsonObject.toJSONString();
	}

	@Override
	public boolean updateServerStatus(String serverIP) {
		try {
			stormServerService.updateByServerIP(serverIP);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}