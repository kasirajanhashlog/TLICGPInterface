package com.claystone.server.timeout;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.json.JSONObject;

import com.claystone.common.utils.CommonConstants;
import com.claystone.db.GpsApiPolling;
import com.claystone.db.GpsApiPollingId;
import com.claystone.server.util.HibernateUtil;

public class CheckDBENMOVIL extends TimerTask {
	private Session mSession;
	private Logger log;

	private Hashtable<String, MobileUnitTimestamp> mUnitENMOVILTimestampHash;
	private String startDate = null;
	
	private LinkedBlockingQueue<Runnable> queue;
	private Properties properties = new Properties();

	private String lEnMovilCompanyIds = "";
	private String lEnMovilProviderIds = "";
	private String lEnMovilStartAPI = "";
	private String lEnMovilStopAPI = "";

	private String lEnMovilUserName = "";
	private String lEnMovilPassword = "";
	private String lEnMovilEmail = "";
	private String lEnMovilLoginURL = "";
	private String lEnMovilKeyValue = "";

	public CheckDBENMOVIL(String strtDate) {
		log = Logger.getLogger(CheckDBENMOVIL.class);
		try {
			properties.load(new FileInputStream("server.properties"));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			log.error("Exception", e1);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			log.error("Exception", e1);
		}

		strtDate = properties.getProperty("StartDate");

		mUnitENMOVILTimestampHash = new Hashtable<String, MobileUnitTimestamp>();
		lEnMovilCompanyIds = properties.getProperty("EnMovilCompanyIds");
		lEnMovilProviderIds = properties.getProperty("EnMovilProviderIds");
		lEnMovilStartAPI = properties.getProperty("EnMovilStartAPI");
		lEnMovilStopAPI = properties.getProperty("EnMovilStopAPI");

		lEnMovilUserName = properties.getProperty("EnMovilUsrName");
		lEnMovilPassword = properties.getProperty("EnMovilPwd");
		lEnMovilEmail = properties.getProperty("EnMovilEmailID");
		lEnMovilLoginURL = properties.getProperty("EnMovilLoginUrl");
		lEnMovilKeyValue = properties.getProperty("EnMovilKeyValue");

		if (strtDate != null) {
			this.startDate = strtDate;
		}

	}

	@Override
	public void run() {
		CheckMobileUnitENMOVILData();
	}

	private void CheckMobileUnitENMOVILData() {

		log.info("Timeout processing cycle: Start ---> " + new Date());

		try {
			mSession = HibernateUtil.beginTransaction();

			String llEnMovilToken = LoginENMOVIL(); // To Get Token Before Processing

			Date currentDateTime = new Date();

			SimpleDateFormat datetimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			List unitidList = mSession
					.createSQLQuery(" select distinct vmu.mobileunitid , vmu.vehicle_id, vmu.company_id,"
							+ " dp.data_provider_name  ,dp.token_url   , "
							+ "dp.api_url  ,dp.token_req_params  ,dp.api_req_params  ,dp.token_req_method  ,dp.api_req_method  ,   "
							+ "dp.token_res_type  ,dp.api_res_type  ,dp.data_interval  , dp.search_key  ,dp.is_token_enable , v.license_plate , v.transporter_name "
							+ "from vehicle_mobile_unit vmu ,   "
							+ "data_provider dp , or_workgroup_vehicle wgv, mobile_unit mu , vehicle v   "
							+ "where wgv.group_owner_id in(" + lEnMovilCompanyIds + ") " + "and mu.data_provider_id in("
							+ lEnMovilProviderIds + ")   " + "and mu.mobileunitid= vmu.mobileunitid   "
							+ "and vmu.first_effective_date <= '" + datetimeFormatter.format(currentDateTime) + "' "
							+ "and vmu.last_effective_date >=  ' " + datetimeFormatter.format(currentDateTime) + "' "
							+ " and vmu.is_active = true " + "and mu.data_provider_id  = dp.data_provider_id   "
							+ "and dp.record_status =   " + CommonConstants.RECORD_STATUS_ACTIVE
							+ " and wgv.vehicle_id = vmu.vehicle_id   " + "and v.vehicle_id = vmu.vehicle_id   "
							+ "and v.record_status =   " + CommonConstants.RECORD_STATUS_ACTIVE
							+ " order by vmu.mobileunitid ")
					.list();

			HibernateUtil.commit();
			if (unitidList.size() <= 0) {
				log.info("VMU ENMOVIL list size 0");
			} else {
				log.info("VMU ENMOVIL list size " + unitidList.size());
			}

			if (mUnitENMOVILTimestampHash.size() == 0) {
				mSession = HibernateUtil.beginTransaction();
				ArrayList<GpsApiPolling> gpsdataCheck = HibernateUtil.castList(GpsApiPolling.class,
						mSession.createQuery(" from GpsApiPolling" + " where id.companyId in(" + lEnMovilCompanyIds
								+ ") " + " and id.dataProviderName = 'ENMOVIL'" + " and apiReqMethod = 'Start'"
								+ " and lastResponseCode = 0 ").list());
				HibernateUtil.commit();
				if (gpsdataCheck.size() > 0) {
					log.info("Build Existing Data : " + gpsdataCheck.size() + " / " + new Date());
					for (int y = 0; y < gpsdataCheck.size(); y++) {
						GpsApiPolling lGpsApiPolling = gpsdataCheck.get(y);
						MobileUnitTimestamp unitTimestamp = new MobileUnitTimestamp();
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
						unitTimestamp.setTokenUrl(lEnMovilStopAPI);
						unitTimestamp.setTokenReqMethod(null);
						unitTimestamp.setTokenReqParams("");
						unitTimestamp.setTokenResType(null);
						unitTimestamp.setLicensePlate(lGpsApiPolling.getLicensePlate());
						unitTimestamp.setTransporterName(lGpsApiPolling.getTransporterName());

						mUnitENMOVILTimestampHash.put(lGpsApiPolling.getId().getMobileunitid(), unitTimestamp);
					}
				}
			}
			Collection<MobileUnitTimestamp> unitENMOVILTimestampList = mUnitENMOVILTimestampHash.values();
			Iterator<MobileUnitTimestamp> unitENMOVILTimestampIter = unitENMOVILTimestampList.iterator();

			String addVehicleIds = null, removeVehicleIds = null;
			while (unitENMOVILTimestampIter.hasNext()) {
				MobileUnitTimestamp unitTimestamp = unitENMOVILTimestampIter.next();
				boolean unitFound = false;
				for (int i = 0; i < unitidList.size(); i++) {
					Object[] row = (Object[]) unitidList.get(i);
					String mobileUnitId = (String) row[0];
					if (unitTimestamp.getMobileUnitId().equals(mobileUnitId)) {
						unitFound = true;
						break;
					}
				}
				if (unitFound == false) {
					if (removeVehicleIds == null)
						removeVehicleIds = "" + unitTimestamp.getLicensePlate();
					else
						removeVehicleIds += "," + unitTimestamp.getLicensePlate();
					log.info("Initi Stop Processing for Unit: " + unitTimestamp.getMobileUnitId());
				}
			}

			for (int i = 0; i < unitidList.size(); i++) {
				Object[] row = (Object[]) unitidList.get(i);
				String mobileUnitId = (String) row[0];
				int vehicleId = (Integer) row[1];
				int vehicleCompanyId = (Integer) row[2];
				String dataProviderName = (String) row[3];
				String tokenURL = (String) row[4];
				String apiURL = (String) row[5];
				String tokenReqPara = (String) row[6];
				String apiReqPara = (String) row[7];
				String tokenReqMethod = (String) row[8];
				String apiReqMethod = (String) row[9];
				String tokenResType = (String) row[10];
				String apiResType = (String) row[11];
				int dataInterval = (Integer) row[12];
				String searchKey = (String) row[13];
				boolean tokenEnable = (Boolean) row[14];
				String licensePlate = (String) row[15];
				String transporterName = (String) row[16];

				log.info("Mobile Unit ID :" + mobileUnitId);
				if (mUnitENMOVILTimestampHash.containsKey(mobileUnitId) == false) {
					Date lastProcessedTime = new Date();
					MobileUnitTimestamp unitTimestamp = new MobileUnitTimestamp();
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

					if (addVehicleIds == null)
						addVehicleIds = "" + unitTimestamp.getLicensePlate();
					else
						addVehicleIds += "," + unitTimestamp.getLicensePlate();

					mUnitENMOVILTimestampHash.put(mobileUnitId, unitTimestamp);
					log.info("Initi Start Processing for Unit: " + unitTimestamp.getMobileUnitId());
				} else {
					MobileUnitTimestamp unitTimestamp = mUnitENMOVILTimestampHash.get(mobileUnitId);
					// unitTimestamp.setUnitActive(true);
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

					mUnitENMOVILTimestampHash.put(mobileUnitId, unitTimestamp);
				}
			}

			if (addVehicleIds != null) {

				log.info("Start API method URL : " + lEnMovilStartAPI + ", Data : " + addVehicleIds);
				String sb = ProcessENMOVILAPI(lEnMovilStartAPI, addVehicleIds, llEnMovilToken);
				if (sb != null) {
					log.info("Start API method response: " + sb.toString());
					JSONObject jo = new JSONObject(sb);
					String lStrStatus = "", lStrMsg = "";
					int lStrResCode = jo.getInt("responseCode");
					lStrStatus = jo.getString("status");
					lStrMsg = jo.getString("message");
					String[] lVehList = addVehicleIds.split(",");

					if (lStrResCode == 200 && lStrStatus != null && lStrStatus.equals("success")) {
						int statusInt = 0;
						for (int i = 0; i < lVehList.length; i++) {
							unitENMOVILTimestampList = mUnitENMOVILTimestampHash.values();
							unitENMOVILTimestampIter = unitENMOVILTimestampList.iterator();
							while (unitENMOVILTimestampIter.hasNext()) {
								MobileUnitTimestamp unitTimestamp = unitENMOVILTimestampIter.next();
								if (unitTimestamp.getLicensePlate().equals(lVehList[i])) {
									//The following function is commented due to performance issue on 27JUL2026
									// InsertGPSAPIPolling(unitTimestamp, statusInt, true);
									log.info("Complete Start Processing for Unit: " + unitTimestamp.getMobileUnitId());
									break;
								}
							}

						}

					} else {
						String[] str = addVehicleIds.split(",");
						for (int w = 0; w < str.length; w++) {
							unitENMOVILTimestampList = mUnitENMOVILTimestampHash.values();
							unitENMOVILTimestampIter = unitENMOVILTimestampList.iterator();
							while (unitENMOVILTimestampIter.hasNext()) {
								MobileUnitTimestamp unitTimestamp = unitENMOVILTimestampIter.next();
								if (unitTimestamp.getLicensePlate().equals(str[w])) {
									//The following function is commented due to performance issue on 27JUL2026
									// InsertGPSAPIPolling(unitTimestamp, -2, true);
									break;
								}
							}
						}
					}
				}

			}
			if (removeVehicleIds != null) {

				log.info("Stop API method URL : " + lEnMovilStopAPI + ", Data : " + removeVehicleIds);
				String sb = ProcessENMOVILAPI(lEnMovilStopAPI, removeVehicleIds, llEnMovilToken);
				if (sb != null) {
					log.info("Stop API method response : " + sb.toString());

					JSONObject jo = new JSONObject(sb);
					String lStrStatus = "", lStrMsg = "";
					int lStrResCode = jo.getInt("responseCode");
					lStrStatus = jo.getString("status");
					lStrMsg = jo.getString("message");
					String[] lVehList = removeVehicleIds.split(",");

					if (lStrResCode == 200 && lStrStatus != null && lStrStatus.equals("success")) {
						int statusInt = 0;
						for (int i = 0; i < lVehList.length; i++) {
							unitENMOVILTimestampList = mUnitENMOVILTimestampHash.values();
							unitENMOVILTimestampIter = unitENMOVILTimestampList.iterator();
							while (unitENMOVILTimestampIter.hasNext()) {
								MobileUnitTimestamp unitTimestamp = unitENMOVILTimestampIter.next();
								if (unitTimestamp.getLicensePlate().equals(lVehList[i])) {
									//The following function is commented due to performance issue on 27JUL2026
									// InsertGPSAPIPolling(unitTimestamp, statusInt, false);
									mUnitENMOVILTimestampHash.remove(unitTimestamp.getMobileUnitId());
									log.info("Complete Stop Processing for Unit: " + unitTimestamp.getMobileUnitId());
									break;
								}
							}

						}

					}
				}
			}

			log.info("Timeout processing cycle: End ---> " + new Date());
		} catch (Throwable e) {
			log.error("", e);
			HibernateUtil.rollback();
		} finally {
			HibernateUtil.close();
		}

	}

	private void InsertGPSAPIPolling(MobileUnitTimestamp model, int response, boolean processStatus) {
		try {
			if (model != null) {
				ArrayList<GpsApiPolling> gpsdataCheck = null;
				String mobileunit = CommonConstants.API_POLL_ALL;
				Session session = HibernateUtil.beginTransaction();
				if (model.getMobileUnitId() != null && !model.getMobileUnitId().trim().equals("")) {
					mobileunit = model.getMobileUnitId();
					gpsdataCheck = HibernateUtil.castList(GpsApiPolling.class,
							session.createQuery(" from GpsApiPolling" + " where id.mobileunitid = '"
									+ model.getMobileUnitId() + "'" + " and id.companyId = " + model.getCompanyId()
									+ " and id.dataProviderName = '" + model.getDataProviderName() + "'").list());
				} else {
					gpsdataCheck = HibernateUtil
							.castList(GpsApiPolling.class, session
									.createQuery(" from GpsApiPolling" + " where id.companyId = " + model.getCompanyId()
											+ " and id.dataProviderName = '" + model.getDataProviderName() + "'")
									.list());
				}

				GpsApiPolling apiPolling = null;
				if (gpsdataCheck != null && gpsdataCheck.size() > 0) {
					apiPolling = gpsdataCheck.get(0);
				} else {
					apiPolling = new GpsApiPolling();
					GpsApiPollingId apiPollingId = new GpsApiPollingId();
					apiPollingId.setCompanyId(model.getCompanyId());
					apiPollingId.setDataProviderName(model.getDataProviderName());
					apiPollingId.setMobileunitid(mobileunit);
					if (mobileunit.equals(CommonConstants.API_POLL_ALL))
						apiPollingId.setSearchKey(mobileunit);
					else
						apiPollingId.setSearchKey(CommonConstants.API_POLL_UNIT);
					apiPolling.setId(apiPollingId);
					apiPolling.setApiReqParams(model.getApiReqParams());
					apiPolling.setApiResType(model.getApiResType());
					apiPolling.setApiUrl(model.getApiUrl());
					apiPolling.setDataInterval(model.getDataInterval());
				}
				if (apiPolling != null) {
					if (processStatus == true)
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
					if (gpsdataCheck == null || gpsdataCheck.size() <= 0) {
						DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						Date gpsdateStr = formatstr.parse("2000-01-01 00:00:00");
						apiPolling.setLastResponseTime(gpsdateStr);
					}
					if (gpsdataCheck != null && gpsdataCheck.size() > 0) {
						if (apiPolling.getLastGpsdateTime() == null) {
							DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							Date gpsdateStr = formatstr.parse("2000-01-01 00:00:00");
							apiPolling.setLastGpsdateTime(gpsdateStr);
						}
					} else {
						DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						Date gpsdateStr = formatstr.parse("2000-01-01 00:00:00");
						apiPolling.setLastGpsdateTime(gpsdateStr);
					}

					if (gpsdataCheck != null && gpsdataCheck.size() > 0)
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
		} finally {
			HibernateUtil.close();
		}
	}

	private String ProcessENMOVILAPI(String urlStop, String vehicleIDS, String llEnMovilToken) {
		String responseStr = null;
		HttpURLConnection http = null;
		int serverResponseCode;
		StringBuilder sb = null;
		InputStream in;
		InputStreamReader inReader;
		try {

			String request = urlStop;
			URL url = new URL(request);
			URLConnection con = url.openConnection();
			http = (HttpURLConnection) con;
			http.setRequestMethod("POST"); // PUT is another valid option
			http.setDoOutput(true);
			String data = URLEncoder.encode("key", "UTF-8") + "=" + URLEncoder.encode(lEnMovilKeyValue, "UTF-8");

			data += "&" + URLEncoder.encode("vehicles", "UTF-8") + "=" + vehicleIDS;

			byte[] out = data.toString().getBytes(StandardCharsets.UTF_8);
			int length = out.length;
			http.setFixedLengthStreamingMode(length);
			http.setRequestProperty("Authorization", "Bearer " + llEnMovilToken);
			http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
			http.connect();
			try (OutputStream os = http.getOutputStream()) {
				os.write(out);
			}

			serverResponseCode = http.getResponseCode();

			switch (serverResponseCode) {
			case HttpsURLConnection.HTTP_OK: {
				sb = new StringBuilder();
				in = http.getInputStream();
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
				in = http.getErrorStream();
				inReader = new InputStreamReader(in);
				BufferedReader reader1 = new BufferedReader(inReader);
				String line1;
				while ((line1 = reader1.readLine()) != null) {
					sb.append(line1);
				}
			}

			responseStr = sb.toString();
		} catch (Exception e) {
			log.error("Error in news parsing" + e.toString());
			e.printStackTrace();
			if (http != null) {
				http.disconnect();
			}
			in = null;
			inReader = null;
			http = null;
			return null;
		} finally {
			if (http != null) {
				http.disconnect();
			}
			in = null;
			inReader = null;
			http = null;
		}
		return responseStr;
	}

	private String LoginENMOVIL() {
		String responseStr = null;
		HttpURLConnection conn = null;
		int serverResponseCode;
		StringBuilder sb = null;
		InputStream in;
		InputStreamReader inReader;
		try {
			String urlParameters = lEnMovilLoginURL;
			JSONObject ENMovilCred = new JSONObject();
			ENMovilCred.put("username", lEnMovilUserName);
			ENMovilCred.put("email", lEnMovilEmail);
			ENMovilCred.put("password", lEnMovilPassword);

			URL url1 = new URL(urlParameters);
			conn = (HttpURLConnection) url1.openConnection();
			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(false);
			conn.setRequestMethod("POST");
			conn.setUseCaches(false);
			conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			OutputStream osw = conn.getOutputStream();
			osw.write(ENMovilCred.toString().getBytes());
			osw.flush();
			osw.close();
			conn.connect();
			serverResponseCode = conn.getResponseCode();

			switch (serverResponseCode) {
			case HttpsURLConnection.HTTP_OK: {
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
			// log.error("Data : " + sb.toString());
			String llSamp = sb.toString();
			JSONObject jsonObject = new JSONObject(llSamp);
			if (jsonObject.has("token")) {
				responseStr = jsonObject.getString("token");
			}
		} catch (Exception e) {
			log.error("Error in news parsing" + e.toString());
			e.printStackTrace();
			return null;
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
			in = null;
			inReader = null;
			conn = null;
		}
		return responseStr;

	}
}
