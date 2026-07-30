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
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.claystone.common.utils.CommonConstants;
import com.claystone.db.GpsApiPolling;
import com.claystone.db.GpsApiPollingId;
import com.claystone.server.util.HibernateUtil;

public class CheckDBEFCON extends TimerTask{
	private Session 							mSession;
	private Logger								log; 
	private Hashtable<String, MobileUnitTimestamp> 	mUnitEFCONTimestampHash;
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

	public CheckDBEFCON(String strtDate)
	{
		log = Logger.getLogger(CheckDBEFCON.class);
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

		mUnitEFCONTimestampHash = new Hashtable<String, MobileUnitTimestamp>(); 
		companyIds = properties.getProperty("UnitEFCONCompanyIds");
		providerIds = properties.getProperty("UnitEFCONProviderIds");
		startAPI = properties.getProperty("EFKONStartAPI");
		stopAPI = properties.getProperty("EFKONStopAPI");
		requestParams = properties.getProperty("EFKONRequestParams");

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
				log.info("VMU EFKON list size "+ unitidList.size());
			}
			
			if(mUnitEFCONTimestampHash.size() == 0) {
				mSession = HibernateUtil.beginTransaction();
				ArrayList<GpsApiPolling> gpsdataCheck = HibernateUtil.castList(GpsApiPolling.class, mSession.createQuery(" from GpsApiPolling"
						+ " where id.companyId in(" + companyIds+") "+
						" and id.dataProviderName = 'EFKON'" +
						" and apiReqMethod = 'Start'"
						+ " and lastResponseCode = 0 ").list());
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
						
						mUnitEFCONTimestampHash.put(lGpsApiPolling.getId().getMobileunitid(), unitTimestamp); 
					}
				}
			}
			Collection<MobileUnitTimestamp> unitEFCONTimestampList = mUnitEFCONTimestampHash.values();
			Iterator<MobileUnitTimestamp> unitEFCONTimestampIter =  unitEFCONTimestampList.iterator();

			String addVehicleIds = null,removeVehicleIds=null;
			while(unitEFCONTimestampIter.hasNext()){
				MobileUnitTimestamp unitTimestamp = unitEFCONTimestampIter.next();
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
					//mUnitEFCONTimestampHash.remove(unitTimestamp.getMobileUnitId());
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
				if(mUnitEFCONTimestampHash.containsKey(mobileUnitId) == false){ 
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
					
					mUnitEFCONTimestampHash.put(mobileUnitId, unitTimestamp); 
					log.info("Initi Start Processing for Unit: " + unitTimestamp.getMobileUnitId() );
				} else {
					MobileUnitTimestamp unitTimestamp  = mUnitEFCONTimestampHash.get(mobileUnitId);
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
					
					mUnitEFCONTimestampHash.put(mobileUnitId, unitTimestamp); 
				}
			}
		
			if(addVehicleIds != null) {
				String urlParameters  = requestParams+addVehicleIds;
				log.info("Start API method URL : "+ startAPI + ", Data : "+ urlParameters);
				String sb = ProcessEFCONAPI(startAPI, urlParameters);
				log.info("Start API method response: " + sb.toString());
				DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				InputSource src = new InputSource();
				src.setCharacterStream(new StringReader(sb.toString()));
				Document doc = builder.parse(src);
				if(doc.getElementsByTagName("ActionResult") != null) {
					if(doc.getElementsByTagName("actionData") != null) {
						for(int i = 0 ; i < doc.getElementsByTagName("actionData").getLength(); i++) {
							String vehicleNo = doc.getElementsByTagName("vehicleNo").item(i).getTextContent();
							String status = doc.getElementsByTagName("status").item(i).getTextContent();
							
							int statusInt = 0;
							if(status != null)
								statusInt = Integer.parseInt(status);
							unitEFCONTimestampList = mUnitEFCONTimestampHash.values();
							unitEFCONTimestampIter =  unitEFCONTimestampList.iterator();
							while(unitEFCONTimestampIter.hasNext()){
								MobileUnitTimestamp unitTimestamp = unitEFCONTimestampIter.next();
								if(unitTimestamp.getLicensePlate().equals(vehicleNo)) {
									//The following function is commented due to performance issue on 27JUL2026
									// InsertGPSAPIPolling(unitTimestamp, statusInt, true);
									log.info("Complete Start Processing for Unit: " + unitTimestamp.getMobileUnitId() );
									break;
								}
							}
						}
					}else {
						String[] str = addVehicleIds.split(",");
						for(int w = 0; w < str.length; w++) {
							unitEFCONTimestampList = mUnitEFCONTimestampHash.values();
							unitEFCONTimestampIter =  unitEFCONTimestampList.iterator();
							while(unitEFCONTimestampIter.hasNext()){
								MobileUnitTimestamp unitTimestamp = unitEFCONTimestampIter.next();
								if(unitTimestamp.getLicensePlate().equals(str[w])) {
									//The following function is commented due to performance issue on 27JUL2026
									// InsertGPSAPIPolling(unitTimestamp, -2, true);
									break;
								}
							}
						}
					}
					
				}
			}
			if(removeVehicleIds != null) {
				String urlParameters  = requestParams+removeVehicleIds;
				log.info("Stop API method URL : "+ stopAPI + ", Data : "+ urlParameters);
				String sb = ProcessEFCONAPI(stopAPI, urlParameters);
				log.info("Stop API method response : " + sb.toString());
				DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				InputSource src = new InputSource();
				src.setCharacterStream(new StringReader(sb.toString()));
				Document doc = builder.parse(src);
				if(doc.getElementsByTagName("ActionResult") != null) {
					if(doc.getElementsByTagName("actionData") != null) {
						if(doc.getElementsByTagName("actionData").getLength() > 0) {
							for(int i = 0 ; i < doc.getElementsByTagName("actionData").getLength(); i++) {
								String vehicleNo = doc.getElementsByTagName("vehicleNo").item(i).getTextContent();
								String status = doc.getElementsByTagName("status").item(i).getTextContent();
								String errorCode = doc.getElementsByTagName("errorCode").item(i).getTextContent();
								
								int statusInt = 0;
								if(errorCode != null)
									statusInt = Integer.parseInt(errorCode);
								if(statusInt == 1000 || statusInt == 1007) {
									unitEFCONTimestampList = mUnitEFCONTimestampHash.values();
									unitEFCONTimestampIter =  unitEFCONTimestampList.iterator();
									while(unitEFCONTimestampIter.hasNext()){
										MobileUnitTimestamp unitTimestamp = unitEFCONTimestampIter.next();
										if(unitTimestamp.getLicensePlate().equals(vehicleNo)) {
											//The following function is commented due to performance issue on 27JUL2026
											// InsertGPSAPIPolling(unitTimestamp, statusInt, false);
											mUnitEFCONTimestampHash.remove(unitTimestamp.getMobileUnitId());
											log.info("Complete Stop Processing for Unit: " + unitTimestamp.getMobileUnitId() );
											break;
										}
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
	
	private String ProcessEFCONAPI(String urlStop, String urlParameters) {
		String responseStr = null;
		HttpURLConnection conn = null;
		int serverResponseCode;
		StringBuilder sb = null;
		InputStream in;
		InputStreamReader inReader;
		try {
			byte[] postData = urlParameters.getBytes( StandardCharsets.UTF_8 );
			int postDataLength = postData.length;
			String request = urlStop;

			// configure the SSLContext with a TrustManager
			/*SSLContext ctx = SSLContext.getInstance("TLS");
			ctx.init(new KeyManager[0], new TrustManager[] {new DefaultTrustManager()}, new SecureRandom());
			SSLContext.setDefault(ctx);*/

			URL url1 = new URL( request );
			conn= (HttpURLConnection) url1.openConnection();           
			/*((HttpsURLConnection) conn).setHostnameVerifier(new HostnameVerifier() {
				@Override
				public boolean verify(String arg0, SSLSession arg1) {
					return true;
				}
			});*/
			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(false);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
			conn.setRequestProperty("charset", "utf-8");
			conn.setRequestProperty("Content-Length", Integer.toString(postDataLength ));
			conn.setUseCaches(false);
			DataOutputStream osw = new DataOutputStream(conn.getOutputStream());
			osw.write( postData );
			osw.flush();
			osw.close();
			conn.connect();
			serverResponseCode = conn.getResponseCode();

			switch (serverResponseCode) {
			case HttpsURLConnection.HTTP_OK:
			{
				sb = new StringBuilder();
				in = conn.getInputStream();
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
				in = conn.getErrorStream();
				inReader = new InputStreamReader(in);
				BufferedReader reader1 = new BufferedReader(inReader);
				String line1;
				while ((line1 = reader1.readLine()) != null) {
					sb.append(line1);
				}
			}
			//log.error("Data : " + sb.toString());
			responseStr = sb.toString();
		}catch (Exception e) {
			log.error("Error in news parsing"+ e.toString());
			e.printStackTrace();
			return null;
		}finally {
			if(conn != null) {
				conn.disconnect();
			}		 
			in = null;		
			inReader = null;
			conn = null;
		}
		return responseStr;
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
}
