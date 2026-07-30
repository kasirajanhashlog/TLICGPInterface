package com.claystone.server.timeout;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import com.claystone.db.Gpsdata;
import com.claystone.server.util.HibernateUtil;

public class FleetRadarCompanyTask extends TimerTask {
	private String mobileCompanyId;
	private Hashtable<String, MobileUnitTimestamp> mCompanyTimestampHash;
	private Session mSession;
	private DatabasePoolManager databasePoolManager;
	private Logger log = Logger.getLogger(FleetRadarCompanyTask.class);
	private String dataProviderName;

	public FleetRadarCompanyTask() {
		databasePoolManager = DatabasePoolManager.getDatabasePoolManager();
	}

	public void cancelTimer() {
		log.info("Timer Thread Cancelled : " + Thread.currentThread().getId() + "  Company " + mobileCompanyId
				+ " date: " + new Date());
		cancel();
	}

	@Override
	public void run() {
		try {
			if (mCompanyTimestampHash.containsKey(mobileCompanyId + dataProviderName) == true) {
				MobileUnitTimestamp mobileCompanyTimestamp = mCompanyTimestampHash
						.get(mobileCompanyId + dataProviderName);
				if (!mobileCompanyTimestamp.isUnitActive()) {
					cancelTimer();
					mCompanyTimestampHash.remove(mobileCompanyId + dataProviderName);
					return;
				}
				ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
				String url = mobileCompanyTimestamp.getApiUrl();
				String method = mobileCompanyTimestamp.getApiResType();
				String tokenType = mobileCompanyTimestamp.getTokenResType();
				log.info("Thread :" + Thread.currentThread().getId() + "; Start Processing  : " + mobileCompanyId
						+ " -  " + method + " - " + dataProviderName + " - " + new Date());
				if (method.equalsIgnoreCase("JSON")) {
						if(mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("FLEETRADAR-H")) {
						FleetRadarGetData apiRes = new FleetRadarGetData();
						String tokenReqParam  = mobileCompanyTimestamp.getTokenReqParams();
						log.info("Inside Request Params-tokenReqParam:"+tokenReqParam+";"+new Date()); 
						String tokenURL = mobileCompanyTimestamp.getTokenUrl();
						String clientID = mobileCompanyTimestamp.getTokenReqParams();
						String clientSecret = mobileCompanyTimestamp.getApiReqParams();
						String grantType = mobileCompanyTimestamp.getTokenReqMethod();
						String dataProviderName = mobileCompanyTimestamp.getDataProviderName();
						int dataProviderId = mobileCompanyTimestamp.getDataProviderId();

						HashMap<String, String> columnList = new HashMap<String, String>();
						columnList.put("licensePlate", "plateNo");
						columnList.put("ignition", "ignition");
						columnList.put("gpsdate", "gpsDateTime");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						columnList.put("speed", "speed");
						String timeFormat = "yyyy-MM-dd HH:mm:ss";
						dataList = apiRes.GetHistoryDataFleetRadarFromAPI(url, mobileCompanyId, columnList, timeFormat,
								  tokenURL,  clientID, clientSecret,  grantType,
								mobileCompanyTimestamp.getDataProviderName(),dataProviderId,tokenReqParam,mobileCompanyTimestamp);
					}

				}
				if (dataList == null) {
					mobileCompanyTimestamp.setLastResponseCode(-1);
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

						List<?> mobileUnitList = session.createSQLQuery(
								" select vmu.mobileunitid ,v.transporter_name from vehicle_mobile_unit vmu,"
										+ " mobile_unit m, vehicle v where v.company_id = "
										+ mobileCompanyTimestamp.getCompanyId() + " and v.license_plate = '"
										+ model.getLicensePlate() + "'" + " and v.record_status = 0"
										+ " and vmu.vehicle_id = v.vehicle_id " + " and vmu.last_effective_date >= '"
										+ new Date() + "'" + " and vmu.is_active = true"
										+ " and m.mobileunitid = vmu.mobileunitid ")
								.list();
						if (mobileUnitList.size() <= 0) {
							//log.info("Active Vehicle Not Found : " + model.getLicensePlate());
							HibernateUtil.commit();
						} else {
							Object[] row = (Object[]) mobileUnitList.get(0);
							String unitId = (String) row[0];
							String transCode = (String) row[1];
							this.log.info((Object) ("Unit : " + unitId + " / " + gpsdateStr));

							if (unitId != null && !unitId.equals("")) {
								ArrayList<Gpsdata> gpsdataCheck = HibernateUtil
										.castList(Gpsdata.class,
												session.createQuery(" from Gpsdata" + " where id.mobileunitid = '"
														+ unitId + "'" + " and id.gpsdate = '" + gpsdateStr + "'")
														.list());

								if (gpsdataCheck.size() <= 0) {
									model.setMobileunitid(unitId);
									BambooData lBambooData = BambooData.getInstance(model);
									if (lBambooData != null) {
										// String mobileUnitID = lBambooData.mobileUnitID;
										lBambooDB = new BambooDB(lBambooData);
										lBambooDB.isLocationData = true;
										databasePoolManager.ProcessUnitData(lBambooDB);
										lBambooDB = null;
									}

								}
								mobileCompanyTimestamp.setLastProcessedTime(new Date());
								mobileCompanyTimestamp.setLastResponseCode(0);
								mobileCompanyTimestamp.setLastResponseTime(new Date());
								JsonMethodAPI lGPSAPIPolling = new JsonMethodAPI();
								//The following function is commented due to performance issue on 27JUL2026
								// lGPSAPIPolling.InsertGPSAPIPollingCompany(mobileCompanyTimestamp, model, unitId,
								// 		transCode);
							}
							HibernateUtil.commit();

						}

					}
					// mobileCompanyTimestamp.setLastResponseCode(0);
					// mobileCompanyTimestamp.setLastResponseTime(new Date());
				} else {
					mobileCompanyTimestamp.setLastProcessedTime(new Date());
					JsonMethodAPI lGPSAPIPolling = new JsonMethodAPI();
					//The following function is commented due to performance issue on 27JUL2026
					// lGPSAPIPolling.InsertGPSAPIPolling(mobileCompanyTimestamp, dataList);
				}
				log.info("Thread :" + Thread.currentThread().getId() + "; After Insert Data : " + mobileCompanyId
						+ " - " + mobileCompanyTimestamp.getDataProviderName() + " - " + dataList.size() + " ; "
						+ new Date());

				mCompanyTimestampHash.put(mobileCompanyId + dataProviderName, mobileCompanyTimestamp);

				// JsonMethodAPI lGPSAPIPolling = new JsonMethodAPI();
				// lGPSAPIPolling.InsertGPSAPIPolling(mobileCompanyTimestamp, dataList);
			}
			// log.info("MobileUnitTaskRunnable End: " + Thread.currentThread().getId() +"
			// unit " + mobileunitId + " date: " + new Date());

		} catch (Exception e) {
			log.error("Timeout Exception", e);
			e.printStackTrace();
			HibernateUtil.rollback();
		} finally {
			HibernateUtil.close();
		}
	}

	public String getMobileCompanyId() {
		return mobileCompanyId;
	}

	public void setMobileCompanyId(String mobileCompanyId) {
		this.mobileCompanyId = mobileCompanyId;
	}

	public String getDataProviderName() {
		return dataProviderName;
	}

	public void setDataProviderName(String dataProviderName) {
		this.dataProviderName = dataProviderName;
	}

	public Hashtable<String, MobileUnitTimestamp> getmCompanyTimestampHash() {
		return mCompanyTimestampHash;
	}

	public void setmCompanyTimestampHash(Hashtable<String, MobileUnitTimestamp> mCompanyTimestampHash) {
		this.mCompanyTimestampHash = mCompanyTimestampHash;
	}

}
