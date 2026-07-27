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

public class CheckDBCompany extends TimerTask {
	private Session 							mSession;
	private Logger								log; 
	private Hashtable<String, MobileUnitTimestamp> 	mCompanyTimestampHash;
	private String startDate = null; 
	private ThreadPoolExecutor threadPool;
	private int poolSize ;
	private int maxPoolSize ;
	private long keepAliveTime ; 
	private LinkedBlockingQueue<Runnable> queue; 
	private Properties properties = new Properties(); 
	private String companyIds = "";
	private String providerIds = "";

	public CheckDBCompany(String strtDate)
	{
		log = Logger.getLogger(CheckDBCompany.class);
		try {
			properties.load(new FileInputStream("server.properties"));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			log.error("Exception" ,e1);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			log.error("Exception" ,e1);
		}
		poolSize = Integer.parseInt(properties.getProperty("CompanyPoolSize"));

		maxPoolSize = Integer.parseInt(properties.getProperty("CompanyMaxPoolSize"));

		keepAliveTime = Integer.parseInt(properties.getProperty("KeepAliveTime")); 
		strtDate =  properties.getProperty("StartDate"); 
		queue = new LinkedBlockingQueue<Runnable>();
		threadPool = new ThreadPoolExecutor(poolSize, maxPoolSize,
				keepAliveTime, TimeUnit.SECONDS, queue);

		mCompanyTimestampHash = new Hashtable<String, MobileUnitTimestamp>(); 
		companyIds = properties.getProperty("CompCompanyIds");
		providerIds = properties.getProperty("CompProviderIds");
//		companyIds = "387";
//		providerIds = "378";
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
			log.info("Company Timeout processing cycle: Start ---> " + new Date());

			if(BuildMobileUnits() == false){
				//HibernateUtil.rollback();
				throw(new Exception("Unable to build units List"));
			}

			Collection<MobileUnitTimestamp> companyTimestampList = mCompanyTimestampHash.values();
			Iterator<MobileUnitTimestamp> companyTimestampIter =  companyTimestampList.iterator();

			while(companyTimestampIter.hasNext()){
				MobileUnitTimestamp companyTimestamp = companyTimestampIter.next();
				if(!companyTimestamp.isProcessedFlag()){
					log.info("New Processing for Company: " + companyTimestamp.getCompanyId() + "; provider : " + companyTimestamp.getDataProviderName());
					companyTimestamp.setProcessedFlag(true);
					//unitTimestamp.setUnitActive(true);
					mCompanyTimestampHash.put(""+companyTimestamp.getCompanyId() + companyTimestamp.getDataProviderName() , companyTimestamp);

					MobileCompanyTaskRunnable lMobileCompanyTask = null;
					lMobileCompanyTask = new MobileCompanyTaskRunnable();
					lMobileCompanyTask.setmCompanyTimestampHash(mCompanyTimestampHash);
					lMobileCompanyTask.setMobileCompanyId( "" + companyTimestamp.getCompanyId()); 
					lMobileCompanyTask.setDataProviderName( "" + companyTimestamp.getDataProviderName()); 
					ProcessUnitDataCompany(lMobileCompanyTask); 
				}else{
					if(companyTimestamp.getLastProcessedTime() != null && companyTimestamp.getLastResponseTime() != null 
							&& companyTimestamp.getLastProcessedTime().getTime() - companyTimestamp.getLastResponseTime().getTime() > (15*60*1000)){
						companyTimestamp.setUnitActive(false);
						mCompanyTimestampHash.put(""+companyTimestamp.getCompanyId() + companyTimestamp.getDataProviderName(), companyTimestamp);
						log.info("Initi Stop Processing for Company: " + companyTimestamp.getCompanyId() + "; provider : " + companyTimestamp.getDataProviderName() );
					}else{
						log.info("Continue Processing for Company: " + companyTimestamp.getCompanyId() + "; provider : " + companyTimestamp.getDataProviderName() );
					}

				} 
			}// end of while 
			log.info("Company Timeout processing cycle: End ---> " + new Date());
		}
		catch(Throwable e){
			log.error("",e);
			HibernateUtil.rollback();
		}finally {
			HibernateUtil.close();
		}
	}

	private boolean BuildMobileUnits(){
		try{
			mSession = HibernateUtil.beginTransaction();

			List unitidList = mSession.createSQLQuery(" select distinct c.company_id,"
					+ " dp.data_provider_name  ,dp.token_url   , " + 
					"dp.api_url  ,dp.token_req_params  ,dp.api_req_params  ,dp.token_req_method  ,dp.api_req_method  ,   " + 
					"dp.token_res_type  ,dp.api_res_type  ,dp.data_interval  , dp.search_key  ,dp.is_token_enable,dp.data_provider_id  " + 
					"from company c , " + 
					"data_provider dp " + 
					"where c.company_id in(" + companyIds +") "+
					"and  dp.company_id = c.company_id " +
					 "and dp.data_provider_id in("+ providerIds +")   " + 
					 "and dp.record_status =   " + CommonConstants.RECORD_STATUS_ACTIVE+
//					 "and dp.record_status =   " + CommonConstants.RECORD_STATUS_DELETED+
					" order by c.company_id ").list();

			HibernateUtil.commit(); 

			Collection<MobileUnitTimestamp> CompanyTimestampList = mCompanyTimestampHash.values();
			Iterator<MobileUnitTimestamp> CompanyTimestampIter =  CompanyTimestampList.iterator();

			while(CompanyTimestampIter.hasNext()){
				MobileUnitTimestamp CompanyTimestamp = CompanyTimestampIter.next();
				boolean unitFound = false;
				for(int i = 0; i < unitidList.size() ; i++){
					Object[] row = (Object[])unitidList.get(i);
					int mobileCompanyId = (int)row[0];
					if(CompanyTimestamp.getCompanyId() == (mobileCompanyId)) {
						unitFound = true;
						break;
					}
				}
				if(unitFound == false){
					CompanyTimestamp.setUnitActive(false);
					String CompanyId = ""+CompanyTimestamp.getCompanyId() + CompanyTimestamp.getDataProviderName();
					mCompanyTimestampHash.put(CompanyId, CompanyTimestamp);
					log.info("Initi Stop Processing for Company: " + CompanyTimestamp.getCompanyId()  + "; provider : " + CompanyTimestamp.getDataProviderName());
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
				//String mobileUnitId = (String)row[0];
				//int vehicleId = (Integer) row[1];
				int companyId = (Integer) row[0];
				String dataProviderName = (String)row[1];
				String tokenURL = (String)row[2];
				String apiURL = (String)row[3];
				String tokenReqPara = (String)row[4];
				String apiReqPara = (String)row[5]; 
				String tokenReqMethod = (String)row[6];
				String apiReqMethod = (String)row[7];
				String tokenResType = (String)row[8];
				String apiResType = (String)row[9]; 
				int dataInterval = (Integer) row[10];
				String searchKey = (String)row[11]; 
				boolean tokenEnable = (Boolean) row[12];
				int dataProviderId = (Integer) row[13];
				
				String CompanyId = ""+companyId + dataProviderName;

				log.info("Company ID :"+ companyId ); 
				if(mCompanyTimestampHash.containsKey(CompanyId) == false){
					Date lastProcessedTime = new Date();
					MobileUnitTimestamp CompanyTimestamp  = new MobileUnitTimestamp();
					//CompanyTimestamp.setMobileUnitId(mobileUnitId);
					CompanyTimestamp.setLastProcessedTime(lastProcessedTime);
					CompanyTimestamp.setProcessedFlag(false);
					CompanyTimestamp.setUnitActive(true);
					CompanyTimestamp.setCompanyId(companyId);
					CompanyTimestamp.setApiUrl(apiURL);
					CompanyTimestamp.setApiReqMethod(apiReqMethod);
					CompanyTimestamp.setApiReqParams(apiReqPara);
					CompanyTimestamp.setApiResType(apiResType);
					CompanyTimestamp.setDataInterval(dataInterval);
					CompanyTimestamp.setDataProviderName(dataProviderName);
					CompanyTimestamp.setSearchKey(searchKey);
					CompanyTimestamp.setTokenEnable(tokenEnable);
					CompanyTimestamp.setTokenUrl(tokenURL);
					CompanyTimestamp.setTokenReqMethod(tokenReqMethod);
					CompanyTimestamp.setTokenReqParams(tokenReqPara);
					CompanyTimestamp.setTokenResType(tokenResType);
					CompanyTimestamp.setLicensePlate("All");
					CompanyTimestamp.setTransporterName("All");
					CompanyTimestamp.setDataProviderId(dataProviderId);

					mCompanyTimestampHash.put(CompanyId, CompanyTimestamp); 
				} else {
					MobileUnitTimestamp CompanyTimestamp  =  mCompanyTimestampHash.get(CompanyId); 
					CompanyTimestamp.setApiUrl(apiURL);
					CompanyTimestamp.setUnitActive(true);
					CompanyTimestamp.setApiReqMethod(apiReqMethod);
					CompanyTimestamp.setApiReqParams(apiReqPara);
					CompanyTimestamp.setApiResType(apiResType);
					CompanyTimestamp.setDataInterval(dataInterval);
					CompanyTimestamp.setDataProviderName(dataProviderName);
					CompanyTimestamp.setSearchKey(searchKey);
					CompanyTimestamp.setTokenEnable(tokenEnable);
					CompanyTimestamp.setTokenUrl(tokenURL);
					CompanyTimestamp.setTokenReqMethod(tokenReqMethod);
					CompanyTimestamp.setTokenReqParams(tokenReqPara);
					CompanyTimestamp.setTokenResType(tokenResType);
					CompanyTimestamp.setDataProviderId(dataProviderId);
					
					mCompanyTimestampHash.put(CompanyId, CompanyTimestamp);
				}
			}

			log.info("Built Companys");
			return true;
		}catch(Exception e){
			HibernateUtil.rollback();
			log.error(e.getMessage());
			e.printStackTrace();
			return false;
		}finally {
			HibernateUtil.close();
		}
	}  
	public void StopPool()
	{
		threadPool.shutdown();
	}

	public void ProcessUnitDataCompany(Runnable task)
	{
		threadPool.execute(task);
		log.info("Thread pool count :" + threadPool.getPoolSize());
		log.info("Thread active count:" + threadPool.getActiveCount());
		log.info("Thread queue count:" + threadPool.getQueue().size());
	} 
}
