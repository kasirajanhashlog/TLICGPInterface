package com.claystone.server.timeout;

import java.io.BufferedReader;
import java.io.Console;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.claystone.common.utils.CommonConstants;
import com.claystone.db.GpsApiPolling;
import com.claystone.db.GpsApiPollingId;
import com.claystone.server.util.HibernateUtil;
import com.google.gson.Gson;

public class CheckDBTrimble extends TimerTask{
	private Session 							mSession;
	private Logger								log; 
	private Hashtable<String, MobileUnitTimestamp> 	mUnitTrimbleTimestampHash;
	private String startDate = null; 
	private ThreadPoolExecutor threadPool;
	private int poolSize ;
	private int maxPoolSize ;
	private long keepAliveTime ; 
	private LinkedBlockingQueue<Runnable> queue; 
	private Properties properties = new Properties(); 
	private String companyIds = "";
	private String providerIds = "";
	private String startAPI = "";
	private String stopAPI = "";
	private String requestParams = "";

	public CheckDBTrimble(String strtDate)
	{
		log = Logger.getLogger(CheckDBTrimble.class);
		try {
			properties.load(new FileInputStream("server.properties"));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			log.error("Exception" ,e1);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			log.error("Exception" ,e1);
		}
		/*poolSize = Integer.parseInt(properties.getProperty("UnitPoolSize"));

		maxPoolSize = Integer.parseInt(properties.getProperty("UnitMaxPoolSize"));

		keepAliveTime = Integer.parseInt(properties.getProperty("KeepAliveTime")); */
		strtDate =  properties.getProperty("StartDate"); 
		/*queue = new LinkedBlockingQueue<Runnable>();
		threadPool = new ThreadPoolExecutor(poolSize, maxPoolSize,
				keepAliveTime, TimeUnit.SECONDS, queue);*/

		mUnitTrimbleTimestampHash = new Hashtable<String, MobileUnitTimestamp>(); 
		companyIds = properties.getProperty("UnitTrimbleCompanyIds");
		providerIds = properties.getProperty("UnitTrimbleProviderIds");
		startAPI = properties.getProperty("TrimbleStartAPI");
		stopAPI = properties.getProperty("TrimbleStopAPI");
		requestParams = properties.getProperty("TrimbleRequestParams");

		if(strtDate != null){
			this.startDate = strtDate;
		}

	}

	@Override
	public void run(){
		CheckMobileUnitEFCONData(); 
	}

	private void CheckMobileUnitEFCONData(){
		log.info("Timeout processing cycle: Start ---> " + new Date());

		try{
			mSession = HibernateUtil.beginTransaction();

			Date currentDateTime = new Date();
			
			SimpleDateFormat datetimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			
			List unitidList = mSession.createSQLQuery(" select distinct vmu.mobileunitid , vmu.vehicle_id, vmu.company_id,"
					+ " dp.data_provider_name  ,dp.token_url   , " + 
					"dp.api_url  ,dp.token_req_params  ,dp.api_req_params  ,dp.token_req_method  ,dp.api_req_method  ,   " + 
					"dp.token_res_type  ,dp.api_res_type  ,dp.data_interval  , dp.search_key  ,dp.is_token_enable , v.license_plate , v.transporter_name " + 
					"from vehicle_mobile_unit vmu ,   " + 
					"data_provider dp , or_workgroup_vehicle wgv, mobile_unit mu , vehicle v   " + 
					"where wgv.group_owner_id in(" + companyIds+") "+
					 "and mu.data_provider_id in("+ providerIds +")   " + 
					 "and mu.mobileunitid= vmu.mobileunitid   " + 
					 "and vmu.first_effective_date <= '"+datetimeFormatter.format(currentDateTime) +"' " + 
					 "and vmu.last_effective_date >=  ' "+datetimeFormatter.format(currentDateTime) + "' " + 
					 " and vmu.is_active = true "+
					 "and mu.data_provider_id  = dp.data_provider_id   " + 
					 "and dp.record_status =   " + CommonConstants.RECORD_STATUS_ACTIVE+
					 " and wgv.vehicle_id = vmu.vehicle_id   " + 
					 "and v.vehicle_id = vmu.vehicle_id   " + 
					 "and v.record_status =   " + CommonConstants.RECORD_STATUS_ACTIVE+
					" order by vmu.mobileunitid ").list();

			HibernateUtil.commit(); 
			if(unitidList.size() <= 0){
				log.info("VMU list size 0");
			}else{
				log.info("VMU Trimble list size "+ unitidList.size());
			}
			
			if(mUnitTrimbleTimestampHash.size() == 0) {
				mSession = HibernateUtil.beginTransaction();
				ArrayList<GpsApiPolling> gpsdataCheck = HibernateUtil.castList(GpsApiPolling.class, mSession.createQuery(" from GpsApiPolling"
						+ " where id.companyId in(" + companyIds+") "+
						" and id.dataProviderName = 'TRIMBLE'" +
						" and apiReqMethod = 'Start'"
						+ " and lastResponseCode >= 0 ").list());
				HibernateUtil.commit();
				if(gpsdataCheck.size() > 0) {
					log.info("Build Existing Data : " + gpsdataCheck.size() + " / "+ new Date());
					for(int y = 0; y < gpsdataCheck.size(); y++) {
						GpsApiPolling lGpsApiPolling = gpsdataCheck.get(y);
						MobileUnitTimestamp unitTimestamp  = new MobileUnitTimestamp();
						unitTimestamp.setMobileUnitId(lGpsApiPolling.getId().getMobileunitid());
						unitTimestamp.setLastProcessedTime(lGpsApiPolling.getLastProcessedTime());
						unitTimestamp.setProcessedFlag(false);
						unitTimestamp.setUnitActive(true);
						unitTimestamp.setCompanyId(lGpsApiPolling.getId().getCompanyId());
						unitTimestamp.setApiUrl(lGpsApiPolling.getApiUrl());
						unitTimestamp.setApiReqMethod(lGpsApiPolling.getApiReqMethod());
						unitTimestamp.setApiReqParams(lGpsApiPolling.getApiReqParams());
						unitTimestamp.setApiResType(lGpsApiPolling.getApiResType());
						unitTimestamp.setDataInterval(lGpsApiPolling.getDataInterval());
						unitTimestamp.setDataProviderName(lGpsApiPolling.getId().getDataProviderName());
						unitTimestamp.setSearchKey(lGpsApiPolling.getId().getSearchKey());
						unitTimestamp.setTokenEnable(false);
						unitTimestamp.setTokenUrl(stopAPI);
						unitTimestamp.setTokenReqMethod(null);
						unitTimestamp.setTokenReqParams(requestParams);
						unitTimestamp.setTokenResType(null);
						unitTimestamp.setLicensePlate(lGpsApiPolling.getLicensePlate());
						unitTimestamp.setTransporterName(lGpsApiPolling.getTransporterName());
						
						mUnitTrimbleTimestampHash.put(lGpsApiPolling.getId().getMobileunitid(), unitTimestamp); 
					}
				}
			}
			Collection<MobileUnitTimestamp> unitTrimbleTimestampList = mUnitTrimbleTimestampHash.values();
			Iterator<MobileUnitTimestamp> unitTrimbleTimestampIter =  unitTrimbleTimestampList.iterator();

			String addVehicleIds = null,removeVehicleIds=null;
			while(unitTrimbleTimestampIter.hasNext()){
				MobileUnitTimestamp unitTimestamp = unitTrimbleTimestampIter.next();
				boolean unitFound = false;
				for(int i = 0; i < unitidList.size() ; i++){
					Object[] row = (Object[])unitidList.get(i);
					String mobileUnitId = (String)row[0];			
					if(unitTimestamp.getMobileUnitId().equals(mobileUnitId)) {
						unitFound = true;
						break;
					}
				}
				if(unitFound == false){
					if(removeVehicleIds == null)
						removeVehicleIds = ""+unitTimestamp.getLicensePlate();
					else
						removeVehicleIds += ","+unitTimestamp.getLicensePlate();
					//mUnitTrimbleTimestampHash.remove(unitTimestamp.getMobileUnitId());
					log.info("Initi Stop Processing for Unit: " + unitTimestamp.getMobileUnitId() );
				}
			}
			
			for(int i = 0; i < unitidList.size() ; i++){
				Object[] row = (Object[])unitidList.get(i);
				String mobileUnitId = (String)row[0];
				int vehicleId = (Integer) row[1];
				int vehicleCompanyId = (Integer) row[2];
				String dataProviderName = (String)row[3];
				String tokenURL = (String)row[4];
				String apiURL = (String)row[5];
				String tokenReqPara = (String)row[6];
				String apiReqPara = (String)row[7]; 
				String tokenReqMethod = (String)row[8];
				String apiReqMethod = (String)row[9];
				String tokenResType = (String)row[10];
				String apiResType = (String)row[11]; 
				int dataInterval = (Integer) row[12];
				String searchKey = (String)row[13]; 
				boolean tokenEnable = (Boolean) row[14];
				String licensePlate = (String)row[15]; 
				String transporterName = (String)row[16];  

				log.info("Mobile Unit ID :"+mobileUnitId ); 
				if(mUnitTrimbleTimestampHash.containsKey(mobileUnitId) == false){ 
					Date lastProcessedTime = new Date();
					MobileUnitTimestamp unitTimestamp  = new MobileUnitTimestamp();
					unitTimestamp.setMobileUnitId(mobileUnitId);
					unitTimestamp.setLastProcessedTime(lastProcessedTime);
					unitTimestamp.setProcessedFlag(false);
					unitTimestamp.setUnitActive(true);
					unitTimestamp.setCompanyId(vehicleCompanyId);
					unitTimestamp.setApiUrl(apiURL);
					unitTimestamp.setApiReqMethod(apiReqMethod);
					unitTimestamp.setApiReqParams(apiReqPara);
					unitTimestamp.setApiResType(apiResType);
					unitTimestamp.setDataInterval(dataInterval);
					unitTimestamp.setDataProviderName(dataProviderName);
					unitTimestamp.setSearchKey(searchKey);
					unitTimestamp.setTokenEnable(tokenEnable);
					unitTimestamp.setTokenUrl(tokenURL);
					unitTimestamp.setTokenReqMethod(tokenReqMethod);
					unitTimestamp.setTokenReqParams(tokenReqPara);
					unitTimestamp.setTokenResType(tokenResType);
					unitTimestamp.setLicensePlate(licensePlate);
					unitTimestamp.setTransporterName(transporterName);
					
					if(addVehicleIds == null)
						addVehicleIds = ""+unitTimestamp.getLicensePlate();
					else
						addVehicleIds += ","+unitTimestamp.getLicensePlate();
					
					mUnitTrimbleTimestampHash.put(mobileUnitId, unitTimestamp); 
					log.info("Initi Start Processing for Unit: " + unitTimestamp.getMobileUnitId() );
				} else {
					MobileUnitTimestamp unitTimestamp  = mUnitTrimbleTimestampHash.get(mobileUnitId);
					//unitTimestamp.setUnitActive(true);
					unitTimestamp.setCompanyId(vehicleCompanyId);
					unitTimestamp.setApiUrl(apiURL);
					unitTimestamp.setApiReqMethod(apiReqMethod);
					unitTimestamp.setApiReqParams(apiReqPara);
					unitTimestamp.setApiResType(apiResType);
					unitTimestamp.setDataInterval(dataInterval);
					unitTimestamp.setDataProviderName(dataProviderName);
					unitTimestamp.setSearchKey(searchKey);
					unitTimestamp.setTokenEnable(tokenEnable);
					unitTimestamp.setTokenUrl(tokenURL);
					unitTimestamp.setTokenReqMethod(tokenReqMethod);
					unitTimestamp.setTokenReqParams(tokenReqPara);
					unitTimestamp.setTokenResType(tokenResType);
					unitTimestamp.setLicensePlate(licensePlate);
					unitTimestamp.setTransporterName(transporterName);
					
					mUnitTrimbleTimestampHash.put(mobileUnitId, unitTimestamp); 
				}
			}
		
			if(addVehicleIds != null) {
				String[] vehStr =  addVehicleIds.split(",");
				ArrayList<TrimbleDataModel> addVehList = new ArrayList<TrimbleDataModel>();
				for(int i=0;i < vehStr.length;i++) {
					TrimbleDataModel model = new TrimbleDataModel();
					model.setEventDate(datetimeFormatter.format(new Date()));
					model.setVehicleNumber(vehStr[i]);
					addVehList.add(model);
				}
				JSONObject json1 = new JSONObject();
				json1.put("consignments", addVehList);
				String json = new Gson().toJson(json1);
				
				log.info("Start Trimble API method URL : "+ startAPI + ", Data : "+ json);
				String sb = ProcessTrimbleAPI(startAPI, json);
				log.info("Start Trimble API method response: " + sb);

	            if(sb != null) {
				JSONParser parser = new JSONParser();
	            Object obj  = parser.parse(sb);
	            JSONObject jo = (JSONObject) obj; 
	            List  val = (List) jo.get("consignments");
	            	for(int i = 0 ; i < val.size(); i++) {
			            JSONObject joObj = (JSONObject) val.get(i); 
			            Long ID = (Long) joObj.get("id"); 
			            String vehicleNo = (String) joObj.get("vehicleNumber"); 
			            
			            if(ID > -1) {
			            	int statusInt = ID.intValue();
							unitTrimbleTimestampList = mUnitTrimbleTimestampHash.values();
							unitTrimbleTimestampIter =  unitTrimbleTimestampList.iterator();
							while(unitTrimbleTimestampIter.hasNext()){
								MobileUnitTimestamp unitTimestamp = unitTrimbleTimestampIter.next();
								if(unitTimestamp.getLicensePlate().equals(vehicleNo)) {
									InsertGPSAPIPolling(unitTimestamp, statusInt, true);
									log.info("Complete Start Trimble Processing for Unit: " + unitTimestamp.getMobileUnitId() );
									break;
								}
							}
			            }
		            }
	            }else {
					String[] str = addVehicleIds.split(",");
					for(int w = 0; w < str.length; w++) {
						unitTrimbleTimestampList = mUnitTrimbleTimestampHash.values();
						unitTrimbleTimestampIter =  unitTrimbleTimestampList.iterator();
						while(unitTrimbleTimestampIter.hasNext()){
							MobileUnitTimestamp unitTimestamp = unitTrimbleTimestampIter.next();
							if(unitTimestamp.getLicensePlate().equals(str[w])) {
								InsertGPSAPIPolling(unitTimestamp, -2, true);
								break;
							}
						}
					}
				}
			}
			if(removeVehicleIds != null) {
				String[] vehStr =  removeVehicleIds.split(",");
				ArrayList<TrimbleRemoveDataModel> removeVehList = new ArrayList<TrimbleRemoveDataModel>();
				for(int i=0;i < vehStr.length;i++) {
					TrimbleRemoveDataModel model = new TrimbleRemoveDataModel();
					model.setVehicleNumber(vehStr[i]);
					removeVehList.add(model);
				}
				JSONObject json1 = new JSONObject();
				json1.put("consignments", removeVehList);
				String json = new Gson().toJson(json1);

				log.info("Stop Trimble API method URL : "+ stopAPI + ", Data : "+ json);
				String sb = ProcessTrimbleAPI(stopAPI, json);
				log.info("Stop Trimble API method response : " + sb.toString());
				
				if(sb != null) {
					JSONParser parser = new JSONParser();
					Object obj  = parser.parse(sb);
					JSONObject jo = (JSONObject) obj; 
					List  val = (List) jo.get("consignments");
					String vehicleNumber = "";
					for(int i = 0 ; i < val.size(); i++) {
						JSONObject joObj = (JSONObject) val.get(i); 
						String status = (String) joObj.get("status"); 
						String vehicleNo = (String) joObj.get("vehicleNumber"); 

						int statusInt = 1000;
						if(!status.equals("INVALID VEHICLE NUMBER")) {
							if(vehicleNumber.contains(vehicleNo) == false) {
								unitTrimbleTimestampList = mUnitTrimbleTimestampHash.values();
								unitTrimbleTimestampIter =  unitTrimbleTimestampList.iterator();
								while(unitTrimbleTimestampIter.hasNext()){
									MobileUnitTimestamp unitTimestamp = unitTrimbleTimestampIter.next();
									if(unitTimestamp.getLicensePlate().equals(vehicleNo)) {
										InsertGPSAPIPolling(unitTimestamp, statusInt, false);
										mUnitTrimbleTimestampHash.remove(unitTimestamp.getMobileUnitId());
										if(vehicleNumber.equals(""))
											vehicleNumber = vehicleNo;
										else
											vehicleNumber += "," +vehicleNo;
										log.info("Complete Trimble Stop Processing for Unit: " + unitTimestamp.getMobileUnitId() );
										break;
									}
								}
							}

						}
					}
				}
			}
			
			log.info("Timeout processing cycle: End ---> " + new Date());
		}
		catch(Throwable e){
			log.error("",e);
			HibernateUtil.rollback();
		} finally {
			HibernateUtil.close();
		}
	}
	
	private String ProcessTrimbleAPI(String urlStop, String urlParameters) {
		String response = null;
		try {
			PostMethod post = new PostMethod(urlStop);
			//post.setRequestHeader("Content-Type", "text/xml; charset=utf-8");
			post.setRequestBody(urlParameters);
			HttpClient httpclient = new HttpClient();
			try {
				int result = httpclient.executeMethod(post);
				// Display status code
				log.error("Response Trimble status code: " + result + " ; Date : " + new Date());
				if(result == 200) {
					response = post.getResponseBodyAsString();
				}
				// Display response
				log.error("Trimble Response body: " + post.getResponseBodyAsString());
				log.info(post.getResponseBodyAsString());
 
				
			} catch (Exception e) {
				log.error("Error Trimble Api : " + e + " ; Date : " + new Date());
				e.printStackTrace();
			} finally {
				// Release current connection to the connection pool once you are done
				post.releaseConnection();
				 ((SimpleHttpConnectionManager)httpclient.getHttpConnectionManager()).shutdown();
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error("Error Trimble Api : " + e + " - Date : " + new Date());
			e.printStackTrace();
		}
		return response;
	}
	private void InsertGPSAPIPolling(MobileUnitTimestamp model, int response, boolean processStatus ) {
		try {
			if(model != null){
				ArrayList<GpsApiPolling> gpsdataCheck = null;
				String mobileunit = CommonConstants.API_POLL_ALL;
				Session session = HibernateUtil.beginTransaction();
				if(model.getMobileUnitId() != null && !model.getMobileUnitId().trim().equals("")) {
					mobileunit = model.getMobileUnitId();
					gpsdataCheck = HibernateUtil.castList(GpsApiPolling.class, session.createQuery(" from GpsApiPolling"
							+ " where id.mobileunitid = '" + model.getMobileUnitId() + "'" +
							" and id.companyId = " + model.getCompanyId() +
							" and id.dataProviderName = '"+ model.getDataProviderName() +"'").list());
				}else {
					gpsdataCheck = HibernateUtil.castList(GpsApiPolling.class, session.createQuery(" from GpsApiPolling"
							+ " where id.companyId = " + model.getCompanyId() +
							" and id.dataProviderName = '"+ model.getDataProviderName() +"'").list());
				}


				GpsApiPolling apiPolling = null;
				if(gpsdataCheck != null && gpsdataCheck.size() > 0) {
					apiPolling = gpsdataCheck.get(0);
				}else {
					apiPolling = new GpsApiPolling();
					GpsApiPollingId apiPollingId = new GpsApiPollingId();
					apiPollingId.setCompanyId(model.getCompanyId());
					apiPollingId.setDataProviderName(model.getDataProviderName());
					apiPollingId.setMobileunitid(mobileunit);
					if(mobileunit.equals(CommonConstants.API_POLL_ALL))
						apiPollingId.setSearchKey(mobileunit);
					else
						apiPollingId.setSearchKey(CommonConstants.API_POLL_UNIT);
					apiPolling.setId(apiPollingId);
					apiPolling.setApiReqParams(model.getApiReqParams());
					apiPolling.setApiResType(model.getApiResType());
					apiPolling.setApiUrl(model.getApiUrl());
					apiPolling.setDataInterval(model.getDataInterval());
				}
				if(apiPolling != null) {
					if(processStatus == true)
						apiPolling.setApiReqMethod("Start");
					else
						apiPolling.setApiReqMethod("Stop");
					apiPolling.setLicensePlate(model.getLicensePlate());
					apiPolling.setTransporterName(model.getTransporterName());
					apiPolling.setIsUnitActive(model.isUnitActive());
					apiPolling.setLastDataCount(0);
					apiPolling.setLastModifiedTime(new Date());
					apiPolling.setLastProcessedTime(model.getLastProcessedTime());
					apiPolling.setLastResponseCode(response);
					if(gpsdataCheck == null || gpsdataCheck.size() <= 0){
						DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
						Date gpsdateStr = formatstr.parse("2000-01-01 00:00:00");
						apiPolling.setLastResponseTime(gpsdateStr);
					}
					if(gpsdataCheck != null && gpsdataCheck.size() > 0) {
						if(apiPolling.getLastGpsdateTime() == null) {
							DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
							Date gpsdateStr = formatstr.parse("2000-01-01 00:00:00");
							apiPolling.setLastGpsdateTime(gpsdateStr);
						}
					}else {
						DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
						Date gpsdateStr = formatstr.parse("2000-01-01 00:00:00");
						apiPolling.setLastGpsdateTime(gpsdateStr);
					}

					if(gpsdataCheck != null && gpsdataCheck.size() > 0)
						session.update(apiPolling);
					else
						session.save(apiPolling);
				}				
				HibernateUtil.commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
			HibernateUtil.rollback();
		}finally {
			HibernateUtil.close();
		}		
	}

	/*public void StopPool()
	{
		threadPool.shutdown();
	}*/

	/*public void ProcessUnitData(Runnable task)
	{
		threadPool.execute(task);
		log.info("Thread pool count :" + threadPool.getPoolSize());
		log.info("Thread active count:" + threadPool.getActiveCount());
		log.info("Thread queue count:" + threadPool.getQueue().size());
	}*/ 
	
	/*private static class DefaultTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

        @Override
        public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }*/
	public class TrimbleDataModel {
		private String eventDate;
		private String vehicleNumber;
		
		public String getEventDate() {
			return eventDate;
		}
		public void setEventDate(String eventDate) {
			this.eventDate = eventDate;
		}
		public String getVehicleNumber() {
			return vehicleNumber;
		}
		public void setVehicleNumber(String vehicleNumber) {
			this.vehicleNumber = vehicleNumber;
		}
		
	}
	public class TrimbleRemoveDataModel {
		private String vehicleNumber;
		
		public String getVehicleNumber() {
			return vehicleNumber;
		}
		public void setVehicleNumber(String vehicleNumber) {
			this.vehicleNumber = vehicleNumber;
		}
		
	}
}

