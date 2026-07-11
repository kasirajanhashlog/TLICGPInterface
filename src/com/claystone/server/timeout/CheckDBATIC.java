package com.claystone.server.timeout;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.claystone.common.utils.CommonConstants;
import com.claystone.server.util.HibernateUtil;

public class CheckDBATIC extends TimerTask {
	private Session 							mSession;
	private static Logger								log; 
	private Hashtable<String, MobileUnitTimestamp> 	mUnitTimestampHash;
	private String startDate = null; 
	private ThreadPoolExecutor threadPool;
	private int poolSize ;
	private int maxPoolSize ;
	private long keepAliveTime ; 
	private LinkedBlockingQueue<Runnable> queue; 
	private Properties properties = new Properties(); 
	private String companyIds = "";
	private String providerIds = "";
	private ArrayList<ATICMasterModel> aticVehicleList = new ArrayList<ATICMasterModel>();
	private String tokenURL = "";
	private String clientID = "";
	private String clientSecret = "";
	private String grantType = "";
	private String userName = "";
	private String password = "";
	private String URL = "";

	public CheckDBATIC(String strtDate)
	{


		
		log = Logger.getLogger(CheckDB.class);
		try {
			properties.load(new FileInputStream("server.properties"));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			log.error("Exception" ,e1);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			log.error("Exception" ,e1);
		}
		poolSize = Integer.parseInt(properties.getProperty("UnitPoolSize"));

		maxPoolSize = Integer.parseInt(properties.getProperty("UnitMaxPoolSize"));

		keepAliveTime = Integer.parseInt(properties.getProperty("KeepAliveTime")); 
		strtDate =  properties.getProperty("StartDate"); 
		queue = new LinkedBlockingQueue<Runnable>();
		threadPool = new ThreadPoolExecutor(poolSize, maxPoolSize,
				keepAliveTime, TimeUnit.SECONDS, queue);

		mUnitTimestampHash = new Hashtable<String, MobileUnitTimestamp>(); 
		companyIds = properties.getProperty("AticUnitCompanyIds");
		providerIds = properties.getProperty("AticUnitProviderIds");
		
		tokenURL = properties.getProperty("tokenAticURL");
		clientID = properties.getProperty("aticClientID");
		clientSecret = properties.getProperty("aticClientSecret");
		grantType = properties.getProperty("aticGrantType");
		userName = properties.getProperty("aticUserName");
		password = properties.getProperty("aticPassword");
		URL = properties.getProperty("aticMasterURL");
		
//		companyIds = "394";
//		providerIds = "203";
		if(strtDate != null){
			this.startDate = strtDate;
		}
		HttpsURLConnection https = null;
		int serverResponseCode;
//		String tokenURL = "https://login.mzoneweb.net/connect/token";
//		String clientID = "mz-aticapi";
//		String clientSecret = "Lj4f6NHn.Tz6#KycUgsvEMiL";
//		String grantType = "password";
//		String userName = "tliadmin";
//		String password = "tliadmin";
//		String URL = "https://live.mzoneweb.net/mzone62.api/Vehicles?&$select=id,description,unit_Description";
		StringBuilder sb = null;
		InputStreamReader inReader;
		InputStream in = null;
		Reader r = null;
		try {
			String llToken = GetATICAccessToken(tokenURL, clientID, clientSecret, grantType,userName,password);
			
			if(!llToken.equals("")) {
				URL url = new URL(URL);
				URLConnection con = url.openConnection();
				https = (HttpsURLConnection) con;
				https.setRequestMethod("GET"); // GET
				https.setDoOutput(true);
				https.setRequestProperty("Authorization", "Bearer " + llToken);
				https.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
				https.connect();
				serverResponseCode = https.getResponseCode();
				if(serverResponseCode == CommonConstants.UNAUTHORISED_ACCESS) {
					
				}
				switch (serverResponseCode) {
				case HttpsURLConnection.HTTP_OK: {
					sb = new StringBuilder();
					in = https.getInputStream();
					inReader = new InputStreamReader(in);
					BufferedReader reader = new BufferedReader(inReader);
					String line;
					while ((line = reader.readLine()) != null) {
						sb.append(line);
					}
					break;
				}
				default:
					sb = new StringBuilder();
					try {
						in = https.getInputStream();
						inReader = new InputStreamReader(in);
					}catch (Exception e) {
						log.error("Error in news parsing" + e.toString());
						in = https.getErrorStream();
						inReader = new InputStreamReader(in);
					}				
					BufferedReader reader1 = new BufferedReader(inReader);
					String line1;
					while ((line1 = reader1.readLine()) != null) {
						sb.append(line1);
					}
				}
				String data = sb.toString();
				try {
					if (data != null && !data.isEmpty()) {
						JSONObject llTMData = new JSONObject(data);
						JSONArray jaData = llTMData.getJSONArray("value");
						 ArrayList<ATICMasterModel> aticMasterList = new ArrayList<ATICMasterModel>();
							for (int i = 0; i < jaData.length(); i++) {
								ATICMasterModel aticMasterModel = new ATICMasterModel();
								JSONObject llData = jaData.getJSONObject(i);
								String idStr = llData.getString("id");
								String unitDescStr = llData.getString("unit_Description");
								String descStr = llData.getString("description");
								aticMasterModel.setAticVehicleId(idStr);
								aticMasterModel.setMobileunitid(unitDescStr);
								aticMasterModel.setAccessToken(llToken);
								aticMasterList.add(aticMasterModel);
							}
							aticVehicleList = aticMasterList;
							
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					log.error(e.getMessage());
					e.printStackTrace();
				}
			}else {
				
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error(e.getMessage());
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		CheckMobileUnitData(); 
	}
	private void CheckMobileUnitData(){
		try
		{
			log.info("Timeout processing cycle: Start ---> " + new Date());

			if(BuildMobileUnits() == false){
				//HibernateUtil.rollback();
				throw(new Exception("Unable to build units List"));
			}

			Collection<MobileUnitTimestamp> unitTimestampList = mUnitTimestampHash.values();
			Iterator<MobileUnitTimestamp> unitTimestampIter =  unitTimestampList.iterator();

			while(unitTimestampIter.hasNext()){
				MobileUnitTimestamp unitTimestamp = unitTimestampIter.next();
				if(!unitTimestamp.isProcessedFlag()){
					log.info("New Processing for Unit: " + unitTimestamp.getMobileUnitId());
					unitTimestamp.setProcessedFlag(true);
					//unitTimestamp.setUnitActive(true);
					mUnitTimestampHash.put(unitTimestamp.getMobileUnitId(), unitTimestamp);

					MobileUnitTaskRunnable lMobileUnitTask = null;
					lMobileUnitTask = new MobileUnitTaskRunnable();
					lMobileUnitTask.setmUnitTimestampHash(mUnitTimestampHash);
					lMobileUnitTask.setMobileunitId( unitTimestamp.getMobileUnitId()); 
					ProcessUnitData(lMobileUnitTask); 
				}else{
					if (unitTimestamp.getLastProcessedTime() != null && (unitTimestamp.getLastProcessedTime().getTime()
							+ (unitTimestamp.getDataInterval() * 5 * 1000)) < (new Date().getTime())) {
						unitTimestampIter.remove();
						log.info("Stop Processing for ended thread Unit: " + unitTimestamp.getMobileUnitId()
								+ "; LPT : " + unitTimestamp.getLastProcessedTime() + "; Buffer : 5; Interval : "+ unitTimestamp.getDataInterval());
					} else if (unitTimestamp.getLastProcessedTime() != null
							&& unitTimestamp.getLastResponseTime() != null
							&& (unitTimestamp.getLastProcessedTime().getTime()
									- unitTimestamp.getLastResponseTime().getTime()) > (15 * 60 * 1000)) {
						unitTimestamp.setUnitActive(false);
						mUnitTimestampHash.put(unitTimestamp.getMobileUnitId(), unitTimestamp);
						log.info("Initi Stop Processing for Unit: " + unitTimestamp.getMobileUnitId() + "; LPT : "
								+ unitTimestamp.getLastProcessedTime());
					} else {
						log.info("Continue Processing for Unit: " + unitTimestamp.getMobileUnitId() + "; LPT : "
								+ unitTimestamp.getLastProcessedTime());
					}					
				} 
			}// end of while 
			log.info("Timeout processing cycle: End ---> " + new Date());
		}
		catch(Throwable e){
			log.error("",e);
			HibernateUtil.rollback();
		} finally {
			HibernateUtil.close();
		}
	}

	private boolean BuildMobileUnits(){
		try{
			Date currentDateTime = new Date();
			SimpleDateFormat datetimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			mSession = HibernateUtil.beginTransaction();
//			providerIds = "193";//Kapoor Diesels
//			providerIds = "195";//	GLOB-NMIPL
			List unitidList = mSession.createSQLQuery(" select distinct vmu.mobileunitid , vmu.vehicle_id, vmu.company_id,"
					+ " dp.data_provider_name  ,dp.token_url   , " + 
					"dp.api_url  ,dp.token_req_params  ,dp.api_req_params  ,dp.token_req_method  ,dp.api_req_method  ,   " + 
					"dp.token_res_type  ,dp.api_res_type  ,dp.data_interval  , dp.search_key  ,dp.is_token_enable , "
					+ " v.license_plate , v.transporter_name, v.vehicle_native_name, " 
					+ " dp.data_provider_id "+
					"from vehicle_mobile_unit vmu ,   " + 
					"data_provider dp , or_workgroup_vehicle wgv, mobile_unit mu , vehicle v   " + 
					" where wgv.group_owner_id in(" + companyIds+") "+
					/*
					 * " where mu.mobileunitid in('AT0005','AT0043'" + ",'AT0048'" + ",'AT0049'" +
					 * ",'AT0051'" + ",'AT0112')" +
					 */
					 //" where mu.mobileunitid in('REAL0017' )" +
//					 " and mu.data_provider_id in("+ providerIds +")   " + 06MAR2023
					 " and mu.data_provider_id in("+ providerIds +")   " + 
					 /*"and mu.data_provider_id in(2)   " + */
					 "and mu.mobileunitid= vmu.mobileunitid   " + 
//					 " and mu.mobileunitid in('AT0569','AT0449','AT3086')"+
//					 "and mu.mobileunitid = 'KPR0000'"+ //Kapoor Diesels
//					 "and mu.mobileunitid in ('KPR0000','KPR0001','KPR0002','KPR0003','KPR0004','KPR0005','KPR0006','KPR0007','KPR0008','KPR0009')"+//Kapoor Diesels
					 "and vmu.first_effective_date <= '"+datetimeFormatter.format(currentDateTime) +"' " + 
					 "and vmu.last_effective_date >=  ' "+datetimeFormatter.format(currentDateTime) + "' " + 
					 " and vmu.is_active = true "+
					 "and mu.data_provider_id  = dp.data_provider_id   " + 
					 "and dp.record_status =   " + CommonConstants.RECORD_STATUS_ACTIVE+
					 " and wgv.vehicle_id = vmu.vehicle_id   " + 
					 "and v.vehicle_id = vmu.vehicle_id   " + 
				//	 " and v.license_plate = 'TN01AU2621' " +
					 "and v.record_status =   " + CommonConstants.RECORD_STATUS_ACTIVE+
					" order by vmu.mobileunitid ").list();


			HibernateUtil.commit(); 
			
			Collection<MobileUnitTimestamp> unitTimestampList = mUnitTimestampHash.values();
			Iterator<MobileUnitTimestamp> unitTimestampIter =  unitTimestampList.iterator();

			while(unitTimestampIter.hasNext()){
				MobileUnitTimestamp unitTimestamp = unitTimestampIter.next();
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
					unitTimestamp.setUnitActive(false);
					mUnitTimestampHash.put(unitTimestamp.getMobileUnitId(), unitTimestamp);
					log.info("Initi Stop Processing for Unit: " + unitTimestamp.getMobileUnitId() );
				}
			}
			if(unitidList.size() <= 0){
				log.info("VMU list size 0");
				return false;
			}else{
				log.info("VMU list size "+ unitidList.size());
			}
			//07AUG2023--Changes started for ATIC-MZONE api
			//Get all the vehicleids
//			Reader r = null;
//			HttpURLConnection http = null;
//			HttpURLConnection http2 = null;
//			int serverResponseCode;
//			try {
//				URL url = new URL("https://live.mzoneweb.net/mzone62.api/LastKnownPositions");
//				URLConnection con = url.openConnection();
//				http = (HttpURLConnection) con;
//				http.setRequestMethod("GET"); // GET
//				http.setDoOutput(true);
//				http.setRequestProperty("Authorization", "Bearer " + llToken);
//				http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
//				http.connect();
//				serverResponseCode = http.getResponseCode();
//				
//			}catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			//Addition ended here
//			log.info("ATIC Master Vehicle List:"+aticVehicleList);
			for(int i = 0; i < unitidList.size() ; i++){
				Object[] row = (Object[])unitidList.get(i);
				String mobileUnitId = (String)row[0];
				String llVehicleNativeName = "";
				String accessToken = "";
				String aticGrantType="";
				String aticUserName="";
				String aticPassword="";
				String aticClientId="";
				String aticClientSecret="";
				for(int iCount=0;iCount<aticVehicleList.size();iCount++) {
					ATICMasterModel lAticMasterModel = new ATICMasterModel();
					lAticMasterModel = aticVehicleList.get(iCount);
					if(lAticMasterModel.getMobileunitid().equals(mobileUnitId)) {
						llVehicleNativeName = lAticMasterModel.getAticVehicleId();
						accessToken = lAticMasterModel.getAccessToken();
						break;
					}
				}
				int vehicleId = (Integer) row[1];
				int vehicleCompanyId = (Integer) row[2];
				String dataProviderName = (String)row[3];
				String tokenURL = (String)row[4];
				String apiURL = (String)row[5];
				String tokenReqPara = (String)row[6];
				if(!tokenReqPara.equals("")) {

					
					JSONObject joTokenReqPara = new JSONObject(tokenReqPara);
					if(joTokenReqPara.has("grant_type")&& joTokenReqPara.getString("grant_type") != null) {
						aticGrantType = joTokenReqPara.getString("grant_type");
					}
					if(joTokenReqPara.has("username")&& joTokenReqPara.getString("username") != null) {
						aticUserName = joTokenReqPara.getString("username");
					}
					if(joTokenReqPara.has("password")&& joTokenReqPara.getString("password") != null) {
						aticPassword = joTokenReqPara.getString("password");
					}
					if(joTokenReqPara.has("client_id")&& joTokenReqPara.getString("client_id") != null) {
						aticClientId = joTokenReqPara.getString("client_id");
					}
					if(joTokenReqPara.has("client_secret")&& joTokenReqPara.getString("client_secret") != null) {
						aticClientSecret = joTokenReqPara.getString("client_secret");
					}
						
				}
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

				int dataProviderId = (Integer) row[18];
				if(llVehicleNativeName.equals("")) {
					continue;
				}
				
				log.info("Mobile Unit ID :"+mobileUnitId ); 
				if(mUnitTimestampHash.containsKey(mobileUnitId) == false){ 
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
					unitTimestamp.setVehicleNativeName(llVehicleNativeName);
					unitTimestamp.setVehicleID(vehicleId);
					unitTimestamp.setDataProviderId(dataProviderId);
					unitTimestamp.setAccessToken(accessToken);
					unitTimestamp.setAticGrantType(aticGrantType);
					unitTimestamp.setAticUserName(aticUserName);
					unitTimestamp.setAticPassword(aticPassword);
					unitTimestamp.setAticClientId(aticClientId);
					unitTimestamp.setAticClientSecret(aticClientSecret);
					
					mUnitTimestampHash.put(mobileUnitId, unitTimestamp); 
				} else {
					MobileUnitTimestamp unitTimestamp = mUnitTimestampHash.get(mobileUnitId);
					unitTimestamp.setApiUrl(apiURL);
					unitTimestamp.setUnitActive(true);
					unitTimestamp.setCompanyId(vehicleCompanyId);
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
					unitTimestamp.setVehicleNativeName(llVehicleNativeName);
					unitTimestamp.setVehicleID(vehicleId);
					unitTimestamp.setDataProviderId(dataProviderId);
					unitTimestamp.setAccessToken(accessToken);
					unitTimestamp.setAticGrantType(aticGrantType);
					unitTimestamp.setAticUserName(aticUserName);
					unitTimestamp.setAticPassword(aticPassword);
					unitTimestamp.setAticClientId(aticClientId);
					unitTimestamp.setAticClientSecret(aticClientSecret);
					mUnitTimestampHash.put(mobileUnitId, unitTimestamp);
				}
			}

			log.info("Built mobile units");
			return true;
		}catch(Exception e){
			HibernateUtil.rollback();
			log.error(e.getMessage());
			e.printStackTrace();
			return false;
		}
	}  
	public void StopPool()
	{
		threadPool.shutdown();
	}

	public void ProcessUnitData(Runnable task)
	{
		threadPool.execute(task);
		log.info("Thread pool count :" + threadPool.getPoolSize());
		log.info("Thread active count:" + threadPool.getActiveCount());
		log.info("Thread queue count:" + threadPool.getQueue().size());
	}
	private static String GetATICAccessToken(String tokenURL, String clientID, String clientSecret, String grantType,
			String userName,String password) {
		String data = "";
		URL url;
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("client_id", clientID);
		params.put("client_secret", clientSecret);
		params.put("grant_type", grantType);
		params.put("username", userName);
		params.put("password",password);
		Set set = params.entrySet();
		Iterator i = set.iterator();
		try {
			StringBuilder postData = new StringBuilder();
			for (Entry<String, String> param : params.entrySet()) {
				if (postData.length() != 0) {
					postData.append('&');
				}
				postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
				postData.append('=');
				postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
			}
			url = new URL(tokenURL);
			byte[] postDataBytes = postData.toString().getBytes("UTF-8");
			HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
			conn.setDoOutput(true);
			conn.getOutputStream().write(postDataBytes);
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			StringBuilder builder = new StringBuilder();
			for (String line = null; (line = reader.readLine()) != null;) {
				builder.append(line).append("\n");
			}
			reader.close();
			conn.disconnect();
			try {
				JSONObject llTataToken = new JSONObject(builder.toString());
				data = llTataToken.getString("access_token");
//				log.error("Tata Motors token returned");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.error(e.getMessage());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error(e.getMessage());
		}

		return data;
	}
}
