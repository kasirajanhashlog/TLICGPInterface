package com.claystone.server.timeout;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
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

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.json.JSONObject;

import com.claystone.common.utils.CommonConstants;
import com.claystone.db.Gpsdata;
import com.claystone.server.timeout.KapoorDieselsGetData.DefaultTrustManager;
//import com.claystone.server.timeout.JsonMethodAPI.DefaultTrustManager;
import com.claystone.server.util.HibernateUtil;


public class CheckDBFastTag extends TimerTask
{
	private Session 							mSession;
	private Logger								log; 
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
	private DatabasePoolManager databasePoolManager;
	private int polling_hours;
	private long delay;

	public CheckDBFastTag(String strtDate)
	{
		databasePoolManager = DatabasePoolManager.getDatabasePoolManager();
		log = Logger.getLogger(CheckDBFastTag.class);
		try {
			properties.load(new FileInputStream("server.properties"));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			log.error("Exception" ,e1);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			log.error("Exception" ,e1);
		}
		poolSize = Integer.parseInt(properties.getProperty("FastTagUnitPoolSize"));

		maxPoolSize = Integer.parseInt(properties.getProperty("FastTagUnitMaxPoolSize"));

		keepAliveTime = Integer.parseInt(properties.getProperty("KeepAliveTime")); 
		strtDate =  properties.getProperty("StartDate"); 
		queue = new LinkedBlockingQueue<Runnable>();
		threadPool = new ThreadPoolExecutor(poolSize, maxPoolSize,
				keepAliveTime, TimeUnit.SECONDS, queue);

		mUnitTimestampHash = new Hashtable<String, MobileUnitTimestamp>(); 
		companyIds = properties.getProperty("FastTagCompanyIds");
		providerIds = properties.getProperty("FastTagProviderIds");
		String polling_hours_str = properties.getProperty("polling_hours");
		polling_hours = Integer.parseInt(polling_hours_str);
//		companyIds = "431";
//		providerIds = "233";
		if(strtDate != null){
			this.startDate = strtDate;
		}

	}

	@Override
	public void run(){
		
//		CheckMobileUnitData(); 
		Session session = HibernateUtil.beginTransaction();
		List<?> fastTagCompanyList = session.createSQLQuery(" select next_processed_time as last_processed_time,(next_processed_time + interval '"+polling_hours+" hours') as next_processed_time  " +
//		List<?> fastTagCompanyList = session.createSQLQuery(" select next_processed_time as last_processed_time,(next_processed_time + interval '"+polling_hours+" minutes') as next_processed_time  " + 
				" from data_provider  " +
				" where data_provider_name ='FASTTAG'"+ 
				" and record_status = " + CommonConstants.RECORD_STATUS_ACTIVE +
				" order by company_id " ).list(); 
		HibernateUtil.commit();
		Date lastProcessedTime = null;
		Date nextProcessedTime = null;
		Date currentTime = new Date();
		if(fastTagCompanyList.size()>0) {
			Object[] row = (Object[])fastTagCompanyList.get(0);
			lastProcessedTime = (Date) row[0];
			nextProcessedTime = (Date) row[1];
			
//			System.out.println("LastProcessedTime:"+lastProcessedTime);
//			System.out.println("NextProcessedTime:"+nextProcessedTime);
//			System.out.println("CurrentTime:"+new Date());
			Calendar currTime = GregorianCalendar.getInstance();
			Calendar maxTime = GregorianCalendar.getInstance();
			
//			System.out.println("Current Time is greater than next processed Time");
//			System.out.println("Current Time is less than next processed Time");
//			System.out.println("Calculate Delay");
//			int year = nextProcessedTime.getYear() + 1900;
//			int month = nextProcessedTime.getMonth()+1;
//			int day = nextProcessedTime.getDate();
//			int hours = nextProcessedTime.getHours();
//			int minutes = nextProcessedTime.getMinutes();
//			int seconds = nextProcessedTime.getSeconds();
			
			int year = lastProcessedTime.getYear() + 1900;
			int month = lastProcessedTime.getMonth()+1;
			int day = lastProcessedTime.getDate();
			int hours = lastProcessedTime.getHours();
			int minutes = lastProcessedTime.getMinutes();
			int seconds = lastProcessedTime.getSeconds();
			
			maxTime.set(Calendar.DATE, day);
			maxTime.set(Calendar.HOUR_OF_DAY, hours);
			maxTime.set(Calendar.MINUTE, minutes);
			maxTime.set(Calendar.SECOND, seconds);
			
			delay = maxTime.getTimeInMillis() - currTime.getTimeInMillis();
//			System.out.println("Delay:"+delay);
//			delay = 0;
//			if(delay <= 0) {
			if(currTime.getTimeInMillis() > maxTime.getTimeInMillis() ) {
				System.out.println("LastProcessedTime:"+lastProcessedTime);
				System.out.println("NextProcessedTime:"+nextProcessedTime);
				System.out.println("CurrentTime:"+new Date());
				CheckMobileUnitData(polling_hours,nextProcessedTime); 
			}
		}
	}

	private void CheckMobileUnitData(int polling_hours,Date nextProcessedTime){
		try
		{
			log.error("Timeout processing cycle: Start ---> " + new Date());
			ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
			if(BuildMobileUnits() == false){
				//HibernateUtil.rollback();
				String status = UpdateDataProviderNextProcessedTime(polling_hours,nextProcessedTime);
				throw(new Exception("Unable to build units List"));
			}
			//Here update the data_provider_next_processed_time
			String status = UpdateDataProviderNextProcessedTime(polling_hours,nextProcessedTime);
			
			Collection<MobileUnitTimestamp> unitTimestampList = mUnitTimestampHash.values();
			Iterator<MobileUnitTimestamp> unitTimestampIter =  unitTimestampList.iterator();
			int row = 0;
			while(unitTimestampIter.hasNext()){
				MobileUnitTimestamp unitTimestamp = unitTimestampIter.next();
//					log.error("New Processing for Unit: " + unitTimestamp.getMobileUnitId());
					String licenseNumber = unitTimestamp.getLicensePlate();
					String mobileunitId = unitTimestamp.getMobileUnitId();
					String url = unitTimestamp.getApiUrl();
					String method = unitTimestamp.getApiResType();
					if (method.equalsIgnoreCase("JSON")) {
//						JsonMethodAPI api = new JsonMethodAPI();
						if (unitTimestamp.getDataProviderName().equalsIgnoreCase("FASTTAG")) {
							HashMap<String, String> columnList = new HashMap<String, String>();
							columnList.put("ignition", "ignition");
							columnList.put("gpsdate", "gpsDateTime");
							columnList.put("latitude", "latitude");
							columnList.put("longitude", "longitude");
							columnList.put("speed", "speed");
							String timeFormat = "MM/dd/yyyy h:mm:ss a";
							
//							dataList = api.GetDataFromAPIFASTTAG(url, mobileunitId, columnList, timeFormat,licenseNumber);
							dataList = GetDataFromAPIFASTTAG(url, mobileunitId, columnList, timeFormat,licenseNumber);
						}
						if (dataList == null) {
							dataList = new ArrayList<GPSDataModel>();
						}
						if (dataList.size() > 0) {
							/*
							 * InsertGpsData gpsData = new InsertGpsData();
							 * gpsData.InsertAPIGPSdata(dataList);
							 */

							BambooDB lBambooDB = null;
							for (int i = 0; i < dataList.size(); i++) {
								GPSDataModel model = dataList.get(i);
								Date gpsdate = model.getGpsdate();

								DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
								String gpsdateStr = formatstr.format(gpsdate);

								Session session = HibernateUtil.beginTransaction();
								ArrayList<Gpsdata> gpsdataCheck = HibernateUtil.castList(Gpsdata.class,
										session.createQuery(" from Gpsdata" + " where id.mobileunitid = '"
												+ model.getMobileunitid() + "'" + " and id.gpsdate = '" + gpsdateStr + "'")
												.list());

								HibernateUtil.commit();
								if (gpsdataCheck.size() <= 0) {
									BambooData lBambooData = BambooData.getInstance(dataList.get(i));
									lBambooData.eventCode = CommonConstants.EVENT_CODE_SIM_GPSDATA_ULIP;
									if (lBambooData != null) {
										// String mobileUnitID = lBambooData.mobileUnitID;
										lBambooDB = new BambooDB(lBambooData);
										lBambooDB.isLocationData = true;
//										lBambooDB.eventCode = CommonConstants.EVENT_CODE_SIM_GPSDATA_ULIP;
										databasePoolManager.ProcessUnitData(lBambooDB);
										lBambooDB = null;

									}
								}
							}
						}
						JsonMethodAPI lGPSAPIPolling = new JsonMethodAPI();
						//The following function is commented due to performance issue on 27JUL2026
						// lGPSAPIPolling.InsertGPSAPIPolling_FastTag(unitTimestamp, dataList);
					}

					row++;
					log.error("Unit: " + unitTimestamp.getMobileUnitId()+",Count:"+row);
					
			}// end of while 
//			//Here update the data_provider_next_processed_time
//			String status = UpdateDataProviderNextProcessedTime(polling_hours);

			log.error("Timeout processing cycle: End ---> " + new Date());
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
//			List tempList = null;
//			if(tempList == null) {
//				return false;
//			}
			
			Date currentDateTime = new Date();
			String cDate ;
			SimpleDateFormat dateFomatter = new SimpleDateFormat("yyyy-MM-dd");
			cDate = dateFomatter.format(currentDateTime);
			cDate = cDate + " 00:00:00";
			SimpleDateFormat datetimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			
			
			
			mSession = HibernateUtil.beginTransaction();
//			List unitidList = mSession.createSQLQuery(" select distinct vmu.mobileunitid , vmu.vehicle_id, vmu.company_id,"
//					+ " dp.data_provider_name  ,dp.token_url   , " + 
//					"dp.api_url  ,dp.token_req_params  ,dp.api_req_params  ,dp.token_req_method  ,dp.api_req_method  ,   " + 
//					"dp.token_res_type  ,dp.api_res_type  ,dp.data_interval  , dp.search_key  ,dp.is_token_enable , "
//					+ " v.license_plate , v.transporter_name, v.vehicle_native_name, " 
//					+ " dp.data_provider_id "+
//					"from vehicle_mobile_unit vmu ,   " + 
//					"data_provider dp , or_workgroup_vehicle wgv, mobile_unit mu left join gps_api_polling gap on " +
//					" mu.company_id = gap.company_id and mu.mobileunitid = gap.mobileunitid and gap.last_processed_time < '" +cDate +"'"+
//					" ,vehicle v   " + 
//					" where wgv.group_owner_id in(" + companyIds+") "+
//					 " and mu.data_provider_id in("+ providerIds +")   " + 
//					 "and mu.mobileunitid= vmu.mobileunitid   " + 
//					 "and vmu.first_effective_date <= '"+datetimeFormatter.format(currentDateTime) +"' " + 
//					 "and vmu.last_effective_date >=  ' "+datetimeFormatter.format(currentDateTime) + "' " + 
//					 " and vmu.is_active = true "+
//					 "and mu.data_provider_id  = dp.data_provider_id   " + 
////					 "and dp.record_status =   " + CommonConstants.RECORD_STATUS_ACTIVE+
//					 "and dp.record_status =   " + CommonConstants.RECORD_STATUS_DELETED+
//					 " and wgv.vehicle_id = vmu.vehicle_id   " + 
//					 "and v.vehicle_id = vmu.vehicle_id   " + 
//					 " and v.license_plate = 'TN31AB9523' " +
//					 "and v.record_status =   " + CommonConstants.RECORD_STATUS_ACTIVE+
//					" order by vmu.mobileunitid ").list();
			
			List unitidList = mSession.createSQLQuery(" select distinct vmu.mobileunitid , vmu.vehicle_id, vmu.company_id,"
					+ " dp.data_provider_name  ,dp.token_url   , " + 
					"dp.api_url  ,dp.token_req_params  ,dp.api_req_params  ,dp.token_req_method  ,dp.api_req_method  ,   " + 
					"dp.token_res_type  ,dp.api_res_type  ,dp.data_interval  , dp.search_key  ,dp.is_token_enable , "
					+ " v.license_plate , v.transporter_name, v.vehicle_native_name, " 
					+ " dp.data_provider_id,gap.last_processed_time "+
					"from vehicle_mobile_unit vmu ,   " + 
					"data_provider dp , or_workgroup_vehicle wgv, mobile_unit mu, " +
					" vehicle v,gps_api_polling gap   " + 
					" where wgv.group_owner_id in(" + companyIds+") "+
					 " and mu.data_provider_id in("+ providerIds +")   " + 
					 "and mu.mobileunitid= vmu.mobileunitid   " + 
					 "and vmu.first_effective_date <= '"+datetimeFormatter.format(currentDateTime) +"' " + 
					 "and vmu.last_effective_date >=  ' "+datetimeFormatter.format(currentDateTime) + "' " + 
					 " and vmu.is_active = true "+
					 "and mu.data_provider_id  = dp.data_provider_id   " + 
//					 " and v.license_plate = 'TN88C5595' " +
//					 " and v.license_plate = 'KA01AJ3141' " +
					 "and dp.record_status =   " + CommonConstants.RECORD_STATUS_ACTIVE+
					 " and wgv.vehicle_id = vmu.vehicle_id   " + 
					 "and v.vehicle_id = vmu.vehicle_id   " + 
					 "and v.record_status =   " + CommonConstants.RECORD_STATUS_ACTIVE+
					 " and mu.company_id = gap.company_id and mu.mobileunitid = gap.mobileunitid "+
//					 "and gap.last_processed_time < '" +cDate +"'"+
					 " union " +
					 " select distinct vmu.mobileunitid , vmu.vehicle_id, vmu.company_id,"
					+ " dp.data_provider_name  ,dp.token_url   , " + 
					"dp.api_url  ,dp.token_req_params  ,dp.api_req_params  ,dp.token_req_method  ,dp.api_req_method  ,   " + 
					"dp.token_res_type  ,dp.api_res_type  ,dp.data_interval  , dp.search_key  ,dp.is_token_enable , "
					+ " v.license_plate , v.transporter_name, v.vehicle_native_name, " 
					+ " dp.data_provider_id, (select current_date -1 from company where company_id = 368) as last_processed_time "+
					" from vehicle_mobile_unit vmu ,   " + 
					"data_provider dp , or_workgroup_vehicle wgv, mobile_unit mu, " +
					" vehicle v " + 
					" where wgv.group_owner_id in(" + companyIds+") "+
					" and mu.data_provider_id in("+ providerIds +")   " + 
					"and mu.mobileunitid= vmu.mobileunitid   " + 
					"and vmu.first_effective_date <= '"+datetimeFormatter.format(currentDateTime) +"' " + 
					"and vmu.last_effective_date >=  ' "+datetimeFormatter.format(currentDateTime) + "' " + 
					" and vmu.is_active = true "+
					"and mu.data_provider_id  = dp.data_provider_id   " + 
					"and dp.record_status =   " + CommonConstants.RECORD_STATUS_ACTIVE+
					" and wgv.vehicle_id = vmu.vehicle_id   " + 
					"and v.vehicle_id = vmu.vehicle_id   " + 
					"and v.record_status =   " + CommonConstants.RECORD_STATUS_ACTIVE+
//					" and v.license_plate = 'TN88C5595' " +
					" and vmu.mobileunitid not in (select mobileunitid from gps_api_polling where company_id in (" + companyIds+"))").list();
//					" order by vmu.mobileunitid ").list();


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
				String llVehicleNativeName = "";
				if((String)row[17] != null && !((String)row[17]).isEmpty())
				{
					llVehicleNativeName =  (String)row[17];
				}
				int dataProviderId = (Integer) row[18];
				Date lastProcessedTime = (Date)row[19];
				
				log.error("Mobile Unit ID :"+mobileUnitId ); 
				if(mUnitTimestampHash.containsKey(mobileUnitId) == false){ 
//					Date lastProcessedTime = new Date();
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
					unitTimestamp.setLastProcessedTime(lastProcessedTime);
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
//		threadPool.execute(task);
//		log.info("Thread pool count :" + threadPool.getPoolSize());
//		log.info("Thread active count:" + threadPool.getActiveCount());
//		log.info("Thread queue count:" + threadPool.getQueue().size());
	} 
	//05APR2024
		public ArrayList<GPSDataModel> GetDataFromAPIFASTTAG(String URL, String unitID, HashMap<String, String> columnList,
				String timeformat,String licenceNumber) {
			ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
			String lastErrorCode="";
			String lastErrorDescription ="";
			boolean error = false;
			try {
				log.info(unitID + /* " Response result : " + result + */"; URL : "+ URL + "; Time : " + new Date());
//				String result = getJSONAPIResult(URL);
				String result = getHashlogAPIResult(URL,licenceNumber);
				if (result != null) {
					try {
						JSONObject jsonObject = new JSONObject(result);
					
						if (jsonObject.has("status")) {
							int sucRes = jsonObject.getInt("status");
							if (sucRes == 200) {
								JSONObject jsonObjectRoot = jsonObject.getJSONObject("results");
								JSONObject jsonObjectSubRoot1 = jsonObjectRoot.getJSONObject("vehLtxnList");
								if (jsonObjectSubRoot1.has("result")) {
									String sucRes1 = jsonObjectSubRoot1.getString("result");
									if (sucRes1.equals("SUCCESS")) {
										JSONObject jsonObjectSubRoot2 = jsonObjectSubRoot1.getJSONObject("vehicle");
										JSONObject jsonObjectSubRoot3 = jsonObjectSubRoot2.getJSONObject("vehltxnList");
										Date lastVaildLocDate = null;
										for (int lst = 0; lst < jsonObjectSubRoot3.getJSONArray("txn").length(); lst++) {
											JSONObject jsonObjectSubRoot4 = jsonObjectSubRoot3.getJSONArray("txn")
													.getJSONObject(lst);

											String readerReadTime = jsonObjectSubRoot4.getString("readerReadTime");
											String laneDirection = jsonObjectSubRoot4.getString("laneDirection");
											String tollPlazaGeocode = jsonObjectSubRoot4.getString("tollPlazaGeocode");
											String tollPlazaName = jsonObjectSubRoot4.getString("tollPlazaName");

											String[] coor = tollPlazaGeocode.split(",");
											double lat = Double.parseDouble(coor[0].trim());
											double lon = Double.parseDouble(coor[1].trim());
											int direction = (laneDirection.equals("N")) ? 0
													: (laneDirection.equals("E")) ? 90
															: (laneDirection.equals("S")) ? 180
																	: (laneDirection.equals("W")) ? 270 : 0;

											SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
											Date locationDate = sdf.parse(readerReadTime);
//											if (locationDate.getTime() > simAssetRegistration.getLocationStartTime().getTime()) {
//												UpdateSimLocationResult(simAssetRegistration, lat, lon, locationDate, true, direction);
	//
//												if (lastVaildLocDate == null
//														|| locationDate.getTime() > lastVaildLocDate.getTime()) {
//													lastVaildLocDate = locationDate;
//												}
//											}
//											UpdateSimLocationResult(simAssetRegistration, lat, lon, locationDate, true, direction);
											int latitude = (int) ((lat) * Double.valueOf(1000000.00));
											int longitude = (int) ((lon) * Double.valueOf(1000000.00));
											
											GPSDataModel dataModel = new GPSDataModel();
											dataModel.setMobileunitid(unitID);
											dataModel.setGpsdate(locationDate);
											dataModel.setGpsstatus(false);
//											dataModel.setLatitude(latitude);
//											dataModel.setLongitude(longitude);
											dataModel.setLatitude(lat);
											dataModel.setLongitude(lon);
											dataModel.setSpeed((short)0);
											dataModel.setDirection((short)direction);
//											dataModel.setIgnition(ignition);
											dataModel.setEventcode((short)CommonConstants.EVENT_CODE_SIM_GPSDATA_ULIP);
											dataModel.setAcc(true);//By default set Acc = true for fast tag data.Because to resolve distance calculation in Asset Trail--04JUN2024
											dataList.add(dataModel);
										
										}
									} else {
										error = true;
										lastErrorCode = "Error";
										lastErrorDescription = jsonObject.toString();

									}
								} else {
									error = true;
									lastErrorCode = "Error";
									lastErrorDescription = jsonObject.toString();

								}

							} else {
								error = true;
								lastErrorCode = "Error";
								lastErrorDescription = jsonObject.toString();

							}
						} else {
							error = true;
							lastErrorCode = "Error";
							lastErrorDescription = jsonObject.toString();

						}
					} catch (Exception e) {
						error = true;
						lastErrorCode = "";
						lastErrorDescription = "result is NULL";
						log.error("Error : " + e);
					}
				}
				Collections.sort(dataList, new Comparator<GPSDataModel>() {
					@Override public int compare(GPSDataModel lhs, GPSDataModel rhs) {
						return lhs.getGpsdate().compareTo(rhs.getGpsdate());
					}
				});
					
				
				log.info("result: "+result); 
				log.info("result: "+result.contains("root"));
				

				log.info(unitID + " dataList : " + dataList.size() + " Time : " + new Date());
			} catch (Exception e) {
				log.error(e.getMessage());
				e.printStackTrace();
			}
			return dataList;
		}
		//05APR2024
		public static String getHashlogAPIResult(String urlString, String licenceNumber){
			// TODO Auto-generated method stub		
			HttpsURLConnection connection = null;
			int serverResponseCode;
			StringBuilder sb = null;
			InputStream in;
			InputStreamReader inReader;
	 		try {
	 			
	 			 // configure the SSLContext with a TrustManager
	 	        SSLContext ctx = SSLContext.getInstance("TLS");
	 	        ctx.init(new KeyManager[0], new TrustManager[] {new DefaultTrustManager()}, new SecureRandom());
	 	        SSLContext.setDefault(ctx);

	 	        
				URL url = new URL(urlString);
				connection = (HttpsURLConnection) url.openConnection(); 
				connection.setHostnameVerifier(new HostnameVerifier() {
		            @Override
		            public boolean verify(String arg0, SSLSession arg1) {
		                return true;
		            }
		        });
				connection.setDoOutput(true);
				connection.setRequestMethod("POST");
				connection.setRequestProperty("content-type", "application/json");
				//connection.setRequestProperty("authkey", CARGO_TOKEN_KEY);
				connection.setConnectTimeout(3*60*1000);
				OutputStream os = connection.getOutputStream();
				OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
				String objData = "{\r\n\"licenceNumber\":\""+licenceNumber+"\"\r\n}";
				osw.write(objData);
				osw.flush();
				osw.close();
				os.close();  //don't forget to close the OutputStream
				connection.connect();
				serverResponseCode = connection.getResponseCode();

				switch (serverResponseCode) {
				case HttpsURLConnection.HTTP_OK:
				{
					sb = new StringBuilder();
					in = connection.getInputStream();
					inReader = new InputStreamReader(in);
					BufferedReader reader = new BufferedReader(inReader);
					String line;
					while ((line = reader.readLine()) != null) {
						sb.append(line);
					}
					break;
				}
				default:
					try {
						sb = new StringBuilder();
						in = connection.getInputStream();
						inReader = new InputStreamReader(in);
						BufferedReader reader = new BufferedReader(inReader);
						String line;
						while ((line = reader.readLine()) != null) {
							sb.append(line);
						}
					}catch (Exception e) {
						System.out.println(licenceNumber +" : Error in Hashlog parsing : "+ serverResponseCode+" : "+ e.getMessage());
						try {
							sb = new StringBuilder();
							in = connection.getErrorStream();
							inReader = new InputStreamReader(in);
							BufferedReader reader1 = new BufferedReader(inReader);
							String line1;
							while ((line1 = reader1.readLine()) != null) {
								sb.append(line1);
							}
						}catch (Exception e1) {
							System.out.println(licenceNumber +" : Error in Hashlog parsing : "+ serverResponseCode+" : "+ e1.getMessage());
						}
					}
					
				}
			} catch (Exception e) {
				System.out.println(licenceNumber +" : Error in Hashlog parsing : "+ e.toString());
				e.printStackTrace();
				return null;
			}finally {
				if(connection != null) {
					connection.disconnect();
				}		 
				in = null;		
				inReader = null;
				connection = null;
	 		}
			if(sb==null)
				return null;
			else
				return sb.toString();
		}
		//Added on 2020OCT07 For Token In case of token authentication failure do send to login
		
	private static class DefaultTrustManager implements X509TrustManager {

	    @Override
	    public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

	    @Override
	    public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

	    @Override
	    public X509Certificate[] getAcceptedIssuers() {
	        return null;
	    }
	}
	public String UpdateDataProviderNextProcessedTime(int polling_hours,Date nextProcessedTime) {
		String Status = "";
		try {
			Session mSession = HibernateUtil.beginTransaction();
//		mSession.createSQLQuery(" update data_provider set next_processed_time = (next_processed_time + interval '"+polling_hours+" hours') where data_provider_name = 'FASTTAG'").executeUpdate();
			mSession.createSQLQuery(" update data_provider set next_processed_time = '"+nextProcessedTime+"' where data_provider_name = 'FASTTAG'").executeUpdate();
			HibernateUtil.commit();
			Status = "Updated Successfully !";
			log.error("NextProcessTime:"+nextProcessedTime+" is "+Status );
			return Status;
		} catch (Exception e) {
			e.printStackTrace();
			HibernateUtil.rollback();
		} finally {
			HibernateUtil.close();
		}
		return Status;
		
	}
}
