package com.claystone.server.timeout;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
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

import org.apache.log4j.Logger;
import org.hibernate.Session;

import com.claystone.common.utils.CommonConstants;
import com.claystone.server.util.HibernateUtil;

public class CheckDB extends TimerTask
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

	public CheckDB(String strtDate)
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
		companyIds = properties.getProperty("UnitCompanyIds");
		providerIds = properties.getProperty("UnitProviderIds");
//		companyIds = "491";
//		providerIds = "395";
		if(strtDate != null){
			this.startDate = strtDate;
		}

	}

	@Override
	public void run(){
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
//					 "and mu.mobileunitid = 'KPR0000'"+ //Kapoor Diesels
//					 "and mu.mobileunitid in ('KPR0000','KPR0001','KPR0002','KPR0003','KPR0004','KPR0005','KPR0006','KPR0007','KPR0008','KPR0009')"+//Kapoor Diesels
					 "and vmu.first_effective_date <= '"+datetimeFormatter.format(currentDateTime) +"' " + 
					 "and vmu.last_effective_date >=  ' "+datetimeFormatter.format(currentDateTime) + "' " + 
					 " and vmu.is_active = true "+
					 "and mu.data_provider_id  = dp.data_provider_id   " + 
					 "and dp.record_status =   " + CommonConstants.RECORD_STATUS_ACTIVE+
					 " and wgv.vehicle_id = vmu.vehicle_id   " + 
					 "and v.vehicle_id = vmu.vehicle_id   " + 
//					 " and v.license_plate = 'NL01AH6850' " +
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
}
