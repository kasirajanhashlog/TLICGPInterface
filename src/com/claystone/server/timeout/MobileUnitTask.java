package com.claystone.server.timeout;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import com.claystone.common.utils.CommonConstants;
import com.claystone.db.CanbusGpsdata;
import com.claystone.db.CanbusVehicleParams;
import com.claystone.db.GpsApiPolling;
import com.claystone.db.Gpsdata;
import com.claystone.server.util.HibernateUtil;

public class MobileUnitTask extends TimerTask {
	private String mobileunitId;
	private Hashtable<String, MobileUnitTimestamp> mUnitTimestampHash;
	private Session mSession;
	private DatabasePoolManager databasePoolManager;
	private Logger log = Logger.getLogger(MobileUnitTask.class);

	public MobileUnitTask() {
		databasePoolManager = DatabasePoolManager.getDatabasePoolManager();
	}

	public void cancelTimer() {
		log.info("Timer Thread Cancelled : " + Thread.currentThread().getId() + "  unit " + mobileunitId + " date: "
				+ new Date());
		cancel();
	}

	@Override
	public void run() {
		try {
			// log.info("MobileUnitTaskRunnable start: " + Thread.currentThread().getId()
			// +" unit " + mobileunitId + " date: " + new Date());
			if (mUnitTimestampHash.containsKey(mobileunitId) == true) {
				MobileUnitTimestamp mobileUnitTimestamp = mUnitTimestampHash.get(mobileunitId);
				if (!mobileUnitTimestamp.isUnitActive()) {
					cancelTimer();
					mUnitTimestampHash.remove(mobileunitId);
					return;
				}
				ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
				String url = mobileUnitTimestamp.getApiUrl();
				String method = mobileUnitTimestamp.getApiResType();
				log.info("Thread :" + Thread.currentThread().getId() + "; Start Processing : " + mobileunitId + " - "
						+ mobileUnitTimestamp.getDataProviderName() + " - " + method + " ; " + new Date());
				if (method.equalsIgnoreCase("xml")) {
					XmlMethodAPI api = new XmlMethodAPI();
					if (mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("ATIC")) {
						String reqParam = mobileUnitTimestamp.getApiReqParams().replace("AT1976", mobileunitId);
						// log.info(mobileunitId+ " - " + mobileUnitTimestamp.getDataProviderName() +"
						// Before Get ATIC Data List : "+ new Date());
						dataList = api.GetDataFromAPI(url, reqParam, mobileunitId);
					} else if (mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("TCI")) {
						HashMap<String, String> columnList = new HashMap<String, String>();
						columnList.put("ignition", "ignition");
						columnList.put("gpsdate", "dateTime");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						// columnList.put("mileage", "odoMetere");
//						String timeFormat = "dd-MMM-yyyy hh:mm:ss";//09JAN2024
						String timeFormat = "dd-MMM-yyyy HH:mm:ss";
						String reqParam = mobileUnitTimestamp.getApiReqParams().replace("NL01Q3948",
								mobileUnitTimestamp.getLicensePlate());
						// log.info(mobileunitId+ " - " + mobileUnitTimestamp.getDataProviderName() +"
						// Before Get TCI Data List : "+ new Date());
						dataList = api.GetDataFromAPI(url, mobileunitId, columnList, timeFormat, reqParam);

					}
				} else if (method.equalsIgnoreCase("JSON")) { // JSON API
					JsonMethodAPI api = new JsonMethodAPI();
					/*
					 * String mobileunitid; Date gpsdate; Date gprsdate; boolean gpsstatus; int
					 * latitude; int longitude; short speed; short direction; boolean acc; Short
					 * eventcode; Short batt; int mileage;
					 */
					if (mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("GLOBAL")) {
						url = url + "/" + mobileUnitTimestamp.getLicensePlate() + "/"
								+ (mobileUnitTimestamp.getDataInterval() / 60);
						HashMap<String, String> columnList = new HashMap<String, String>();
						columnList.put("ignition", "Ignition");
						columnList.put("gpsdate", "Date");
						columnList.put("latitude", "Latitude");
						columnList.put("longitude", "Longitude");
						columnList.put("speed", "Speed");
						String timeFormat = "dd-MMM-yyyy hh:mm:ss aaa";
						// log.info(mobileunitId+ " - " + mobileUnitTimestamp.getDataProviderName() +"
						// Before Get GLOBAL Data List : "+ new Date());
						dataList = api.GetDataFromAPI(url, mobileunitId, columnList, timeFormat);

					}else if (mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("GLOBAL-NEW")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("GLOBAL_TVS")) {
						url = url + "/" + mobileUnitTimestamp.getLicensePlate();
						HashMap<String, String> columnList = new HashMap<String, String>();
						columnList.put("ignition", "ignition");
						columnList.put("gpsdate", "gpsDateTime");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						String timeFormat = "MM/dd/yyyy h:mm:ss a";
						
						dataList = api.GetDataFromAPI(url, mobileunitId, columnList, timeFormat);

					} else if (mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-DGFC")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-ITS")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-SWIFT")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-SUSHILA")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-RKR")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-PAPL")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-KMT")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-BRT")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-PTL")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-ALS")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-DGRC")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-MRB")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-DBRC")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-SKY")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-GRC")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-RTM")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-ALSNM")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-ABL")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-BLJL")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-SMNL")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-EKTA")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-SPRM")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-MLKL")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-SKYNM")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-YGN")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-HLSL")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-CLPL")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-SRML")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-SSL")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-VRPL")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-DAX")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-TAMANA")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-TRANS")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-RLPL")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-PRBD")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-GXL")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-RKRL")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-MAHA")
							|| mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("AXES-TKR")) {
						String reqParam = mobileUnitTimestamp.getApiReqParams();
						url = url + reqParam + mobileUnitTimestamp.getLicensePlate();
						HashMap<String, String> columnList = new HashMap<String, String>();
						columnList.put("ignition", "ignition");
						columnList.put("gpsdate", "dttime");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						String timeFormat = "dd MMM yyyy HH:mm:ss";
						// log.info(mobileunitId+ " - " + mobileUnitTimestamp.getDataProviderName() +"
						// Before Get AXES Data List : "+ new Date());
						dataList = api.GetDataFromAPIAxes(url, mobileunitId, columnList, timeFormat);

					} else if (mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("REALTRACK")) {
						String reqParam = mobileUnitTimestamp.getApiReqParams();
						url = url + reqParam + mobileUnitTimestamp.getLicensePlate();
						HashMap<String, String> columnList = new HashMap<String, String>();
						columnList.put("ignition", "ignition_status");
						columnList.put("gpsdate", "date_time");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						String timeFormat = "dd-MM-yyyy HH:mm:ss";
						// log.info(mobileunitId+ " - " + mobileUnitTimestamp.getDataProviderName() +"
						// Before Get REALTRACK Data List : "+ new Date());
						dataList = api.GetDataFromAPIRealTrack(url, mobileunitId, columnList, timeFormat);
					} else if (mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("ROOSTER")) {
						String reqParam = mobileUnitTimestamp.getApiReqParams();
						url = url + reqParam + '[' + '"' + mobileUnitTimestamp.getLicensePlate() + '"' + ']';
						HashMap<String, String> columnList = new HashMap<String, String>();
						columnList.put("ignition", "Ignition");
						columnList.put("gpsdate", "LastUpdate");
						columnList.put("latitude", "Lat");
						columnList.put("longitude", "Long");
						columnList.put("speed", "Speed");
						String timeFormat = "yyyy-MM-dd HH:mm:ss";
						// log.info(mobileunitId+ " - " + mobileUnitTimestamp.getDataProviderName() +"
						// Before Get ROOSTER Data List : "+ new Date());
						dataList = api.GetDataFromAPI(url, mobileunitId, columnList, timeFormat);
					} else if (mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("VAMOSYS-GPSVTS")) {
						String reqParam = mobileUnitTimestamp.getApiReqParams();
						//log.info("Inside VamosysGpsVTS Api");
						int llDtInt = (mobileUnitTimestamp.getDataInterval() * 1000);
						Date llToD = new Date();
						long llTo = llToD.getTime() - llDtInt;
						Date llFromD = new Date(llTo);
						if (mobileUnitTimestamp.getLastProcessedTimeVamosys() == null) {
							Session session = HibernateUtil.beginTransaction();
							List llGpsPollData = session.createQuery(" from GpsApiPolling"
									+ " where id.mobileunitid = '" + mobileunitId + "'" + " and id.companyId = "
									+ mobileUnitTimestamp.getCompanyId() + " and id.dataProviderName = '"
									+ mobileUnitTimestamp.getDataProviderName() + "'").list();
							HibernateUtil.commit();
							if (llGpsPollData.size() > 0) {
								GpsApiPolling lPollData = (GpsApiPolling) llGpsPollData.get(0);
								llFromD = lPollData.getLastGpsdateTime();
							}

						} else if (mobileUnitTimestamp.getLastProcessedTimeVamosys() != null) {
							llFromD = mobileUnitTimestamp.getLastProcessedTimeVamosys();
						}
						SimpleDateFormat datetimeFormatter = new SimpleDateFormat("yyyy-MM-dd");
						SimpleDateFormat dtFormat = new SimpleDateFormat("HH:mm:ss");
						String llFromDt = datetimeFormatter.format(llFromD);
						String llFromT = dtFormat.format(llFromD);
						String lToD = datetimeFormatter.format(llToD);
						String lToT = dtFormat.format(llToD);
						// url = url.replace("licensePlate", "NITTSU_TN04AS2637");
						if (mobileUnitTimestamp.getVehicleNativeName() != null
								&& !mobileUnitTimestamp.getVehicleNativeName().isEmpty()) {
							url = url.replace("licensePlate", mobileUnitTimestamp.getVehicleNativeName());
						}
						url = url.replace("startD", llFromDt);
						url = url.replace("startT", llFromT);
						url = url.replace("endD", lToD);
						url = url.replace("endT", lToT);

						HashMap<String, String> columnList = new HashMap<String, String>();
						columnList.put("ignition", "ignitionStatus");
						columnList.put("gpsdate", "lastSeen");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						String timeFormat = "yyyy-MM-dd HH:mm:ss";
						// log.info(mobileunitId+ " - " + mobileUnitTimestamp.getDataProviderName() +"
						// Before Get ROOSTER Data List : "+ new Date());
						VamosysGpsVTSGetData llGetVamosys = new VamosysGpsVTSGetData();
						log.info("Data fetching for unit : " + mobileunitId + " from : " + llFromD);
						dataList = llGetVamosys.GetDataVamosysGpsVTSFromAPI(url, mobileunitId, columnList, timeFormat);
						if (dataList != null && dataList.size() > 0) {
							log.info("Data fetching for unit : " + mobileunitId + " data size : " + dataList.size()
									+ " last gps update time : " + dataList.get(dataList.size() - 1).getGpsdate());
							//Added on 2022-Mar-30th null value received in Gpsdate
							if(dataList.get(dataList.size() - 1).getGpsdate() != null)
							{
								mobileUnitTimestamp
								.setLastProcessedTimeVamosys(dataList.get(dataList.size() - 1).getGpsdate());
							}else {
								for(int l = dataList.size();l > 0 ; l-- )
								{
									if(dataList.get(l).getGpsdate() != null)
									{
										log.info(" null value received in Gpsdate hence check for others data if gpsdate is available"
												+ " for last processed Time  : " +mobileunitId); //Added on 2022-Mar-30
										
										mobileUnitTimestamp
										.setLastProcessedTimeVamosys(dataList.get(l).getGpsdate());
										break;									
									}
								}
							}
						}
					} else if (mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("EICHER")) {
						String reqParam = mobileUnitTimestamp.getApiReqParams();
						// http://https://api.eu-de.apiconnect.appdomain.cloud/vecv-org-dev/production/v2/vehiclelocationdeviceidtracking/vehiclelocationdeviceiddetails
						// X-ibm-client-id: e035ba14-36d3-4ded-81e2-34168ed23a70
						// X-ibm-client-secret: vC3uD0wD1mT6uH5tO5tQ1uO1lB6dN4vB7wH3kB2tK1eS4eC8eL
						// {
						// "startDateTime": 20211006000040, "endDateTime": 20211006230001,
						// "chassisno": [
						// "MC2CASRF0LK067265"
						// ]
						// }

						//log.info("Inside EICHER Api");
						int llDtInt = (mobileUnitTimestamp.getDataInterval() * 1000);
						Date llToD = new Date();
						long llTo = llToD.getTime() - llDtInt;
						Date llFromD = new Date(llTo);

						ArrayList<CanbusVehicleParams> lCanbusParamList = new ArrayList<CanbusVehicleParams>();
						ArrayList<CanBusModel> lBusModelList = new ArrayList<CanBusModel>();
						Session session = HibernateUtil.beginTransaction();
						if (mobileUnitTimestamp.getLastProcessedTimeEICHER() == null) {
							List llGpsPollData = session.createQuery(" from GpsApiPolling"
									+ " where id.mobileunitid = '" + mobileunitId + "'" + " and id.companyId = "
									+ mobileUnitTimestamp.getCompanyId() + " and id.dataProviderName = '"
									+ mobileUnitTimestamp.getDataProviderName() + "'").list();
							if (llGpsPollData.size() > 0) {
								GpsApiPolling lPollData = (GpsApiPolling) llGpsPollData.get(0);
								llFromD = lPollData.getLastGpsdateTime();								
								long diff = (llToD.getTime() - llFromD.getTime());																
								 if(diff > (CommonConstants.TLI_EICHER_HOURS * 60 *60*1000) )
								 {
									 Calendar cal = Calendar.getInstance();
									 cal.setTime(llToD);
									 cal.add(Calendar.HOUR_OF_DAY, CommonConstants.TLI_EICHER_DIFF_HOURS);
									 llFromD = cal.getTime();
								 }
							}
						} else if (mobileUnitTimestamp.getLastProcessedTimeEICHER() != null) {
							llFromD = mobileUnitTimestamp.getLastProcessedTimeEICHER();
						}
						if (mobileUnitTimestamp.getMileage() == 0) {
							List lmGpsDataList = session.createQuery(" from Gpsdata " + " where id.mobileunitid = '"
									+ mobileunitId + "'" + " order by id.gpsdate desc ").setMaxResults(1).list();
							if (lmGpsDataList.size() > 0) {
								Gpsdata lGpsData = (Gpsdata) lmGpsDataList.get(0);
								mobileUnitTimestamp.setMileage(lGpsData.getMileage());
							}
						}
						List llCanBusDataList = session.createQuery(" from CanbusVehicleParams "
								+ " where id.vehicleId = '" + mobileUnitTimestamp.getVehicleID() + "'"
								+ " and id.companyId = " + mobileUnitTimestamp.getCompanyId()).list();

						if (llCanBusDataList.size() > 0) {
							mobileUnitTimestamp.setCanBusDataAvailable(true);
							for (int l = 0; l < llCanBusDataList.size(); l++) {
								CanbusVehicleParams lParams = (CanbusVehicleParams) llCanBusDataList.get(l);
								lCanbusParamList.add(lParams);
							}

							List llCanBusGpsDtList = session
									.createQuery(" from CanbusGpsdata " + " where id.mobileunitid = '"
											+ mobileUnitTimestamp.getMobileUnitId() + "'" + " order by id.gpsdate desc")
									.setMaxResults(1).list();
							if (llCanBusGpsDtList.size() > 0) {
								CanbusGpsdata lGpData = (CanbusGpsdata) llCanBusGpsDtList.get(0);

								for (int i = 0; i < lCanbusParamList.size(); i++) {
									CanbusVehicleParams lParam = lCanbusParamList.get(i);
									CanBusModel lBuModel = new CanBusModel();
									lBuModel.setCanVarId(lParam.getId().getCanVarId());
									lBuModel.setZtVarId(lParam.getId().getZtVarId());
									if (lParam.getId().getZtVarId().equals("output_1")) {
										lBuModel.setParamOutputValue(lGpData.getOutput1());
									}
									if (lParam.getId().getZtVarId().equals("output_2")) {
										lBuModel.setParamOutputValue(lGpData.getOutput2());
									}
									if (lParam.getId().getZtVarId().equals("output_3")) {
										lBuModel.setParamOutputValue(lGpData.getOutput3());
									}
									if (lParam.getId().getZtVarId().equals("output_4")) {
										lBuModel.setParamOutputValue(lGpData.getOutput4());
									}
									if (lParam.getId().getZtVarId().equals("output_5")) {
										lBuModel.setParamOutputValue(lGpData.getOutput5());
									}
									if (lParam.getId().getZtVarId().equals("output_6")) {
										lBuModel.setParamOutputValue(lGpData.getOutput6());
									}
									if (lParam.getId().getZtVarId().equals("output_7")) {
										lBuModel.setParamOutputValue(lGpData.getOutput7());
									}
									if (lParam.getId().getZtVarId().equals("output_8")) {
										lBuModel.setParamOutputValue(lGpData.getOutput8());
									}
									if (lParam.getId().getZtVarId().equals("output_9")) {
										lBuModel.setParamOutputValue(lGpData.getOutput9());
									}
									if (lParam.getId().getZtVarId().equals("output_10")) {
										lBuModel.setParamOutputValue(lGpData.getOutput10());
									}
									if (lParam.getId().getZtVarId().equals("output_11")) {
										lBuModel.setParamOutputValue(lGpData.getOutput11());
									}
									if (lParam.getId().getZtVarId().equals("output_12")) {
										lBuModel.setParamOutputValue(lGpData.getOutput12());
									}
									if (lParam.getId().getZtVarId().equals("output_13")) {
										lBuModel.setParamOutputValue(lGpData.getOutput13());
									}
									if (lParam.getId().getZtVarId().equals("output_14")) {
										lBuModel.setParamOutputValue(lGpData.getOutput14());
									}
									if (lParam.getId().getZtVarId().equals("output_15")) {
										lBuModel.setParamOutputValue(lGpData.getOutput15());
									}
									if (lParam.getId().getZtVarId().equals("output_16")) {
										lBuModel.setParamOutputValue(lGpData.getOutput16());
									}
									if (lParam.getId().getZtVarId().equals("output_17")) {
										lBuModel.setParamOutputValue(lGpData.getOutput17());
									}
									if (lParam.getId().getZtVarId().equals("output_18")) {
										lBuModel.setParamOutputValue(lGpData.getOutput18());
									}
									if (lParam.getId().getZtVarId().equals("output_19")) {
										lBuModel.setParamOutputValue(lGpData.getOutput19());
									}
									if (lParam.getId().getZtVarId().equals("output_20")) {
										lBuModel.setParamOutputValue(lGpData.getOutput20());
									}
									if (lParam.getId().getZtVarId().equals("output_21")) {
										lBuModel.setParamOutputValue(lGpData.getOutput21());
									}
									if (lParam.getId().getZtVarId().equals("output_22")) {
										lBuModel.setParamOutputValue(lGpData.getOutput22());
									}
									if (lParam.getId().getZtVarId().equals("output_23")) {
										lBuModel.setParamOutputValue(lGpData.getOutput23());
									}
									if (lParam.getId().getZtVarId().equals("output_24")) {
										lBuModel.setParamOutputValue(lGpData.getOutput24());
									}
									if (lParam.getId().getZtVarId().equals("output_25")) {
										lBuModel.setParamOutputValue(lGpData.getOutput25());
									}
									if (lParam.getId().getZtVarId().equals("output_26")) {
										lBuModel.setParamOutputValue(lGpData.getOutput26());
									}
									if (lParam.getId().getZtVarId().equals("output_27")) {
										lBuModel.setParamOutputValue(lGpData.getOutput27());
									}
									if (lParam.getId().getZtVarId().equals("output_28")) {
										lBuModel.setParamOutputValue(lGpData.getOutput28());
									}
									if (lParam.getId().getZtVarId().equals("output_29")) {
										lBuModel.setParamOutputValue(lGpData.getOutput29());
									}
									if (lParam.getId().getZtVarId().equals("output_30")) {
										lBuModel.setParamOutputValue(lGpData.getOutput30());
									}
									if (lParam.getId().getZtVarId().equals("output_31")) {
										lBuModel.setParamOutputValue(lGpData.getOutput31());
									}
									if (lParam.getId().getZtVarId().equals("output_32")) {
										lBuModel.setParamOutputValue(lGpData.getOutput32());
									}
									if (lParam.getId().getZtVarId().equals("output_33")) {
										lBuModel.setParamOutputValue(lGpData.getOutput33());
									}
									if (lParam.getId().getZtVarId().equals("output_34")) {
										lBuModel.setParamOutputValue(lGpData.getOutput34());
									}
									if (lParam.getId().getZtVarId().equals("output_35")) {
										lBuModel.setParamOutputValue(lGpData.getOutput35());
									}
									lBusModelList.add(lBuModel);
								}

								mobileUnitTimestamp.setlCanModelList(lBusModelList);
							}
						} else {
							mobileUnitTimestamp.setCanBusDataAvailable(false);
						}
						HibernateUtil.commit();

						SimpleDateFormat datetimeFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
						String llFromDt = datetimeFormatter.format(llFromD);
						String lToD = datetimeFormatter.format(llToD);

						HashMap<String, String> columnList = new HashMap<String, String>();
						columnList.put("ignition", "ignitionStatus");
						columnList.put("gpsdate", "lastSeen");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						String timeFormat = "yyyy-MM-dd HH:mm:ss";

						EicherGetData llGetEICHER = new EicherGetData();
						log.info("Data fetching for unit : " + mobileunitId + " from : " + llFromD);
						// Clear when actual data fromdate and todate to
//						url = "https://partnerapi.vecv.net/tracking/vehicletracking";
						dataList = llGetEICHER.GetDataFromEICHER(url, mobileUnitTimestamp.getTokenUrl(),
								mobileUnitTimestamp.getApiReqParams(), llFromDt, lToD, mobileunitId,
								mobileUnitTimestamp.isCanBusDataAvailable(), lCanbusParamList, lBusModelList,
								mobileUnitTimestamp.getMileage());

//						dataList = llGetEICHER.GetDataFromEICHER(url, mobileUnitTimestamp.getTokenUrl(),
//								mobileUnitTimestamp.getApiReqParams(), "20220110104000", "20220110214000", 
//								mobileunitId, mobileUnitTimestamp.isCanBusDataAvailable(),lCanbusParamList,lBusModelList,mobileUnitTimestamp.getMileage() );
						if (dataList != null && dataList.size() > 0) {
							log.info("Data fetching for unit : " + mobileunitId + " data size : " + dataList.size()
									+ " last gps update time : " + dataList.get(dataList.size() - 1).getGpsdate());
							mobileUnitTimestamp
									.setLastProcessedTimeEICHER(dataList.get(dataList.size() - 1).getGpsdate());
							mobileUnitTimestamp.setlCanModelList(dataList.get(dataList.size() - 1).getlCanModelList());
							mobileUnitTimestamp.setMileage(dataList.get(dataList.size() - 1).getMileage());
						}
					}
//					else if (mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR")||
// 							mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTORS-MDE")
// 							||mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTORS-HARI")) {
//						String reqParam = mobileUnitTimestamp.getApiReqParams();						
//						//log.info("Inside TATAMOTOR Api");
//						String tokenURL = mobileUnitTimestamp.getTokenUrl();
//						String clientID = mobileUnitTimestamp.getTokenReqParams();
//						String clientSecret = mobileUnitTimestamp.getApiReqParams();
//						String grantType = mobileUnitTimestamp.getTokenReqMethod();
//
//						
//						log.info(mobileUnitTimestamp.getMobileUnitId()+ " time : "+new Date());
//						url = url.replace("licensePlate", mobileUnitTimestamp.getMobileUnitId());
//
//						HashMap<String, String> columnList = new HashMap<String, String>();
//						columnList.put("ignition", "ignitionStatus");
//						columnList.put("gpsdate", "lastSeen");
//						columnList.put("latitude", "latitude");
//						columnList.put("longitude", "longitude");
//						columnList.put("speed", "speed");
//						String timeFormat = "yyyy-MM-dd HH:mm:ss";
//
//						TataMotorsGetData llGetTataMotors = new TataMotorsGetData();	
//						dataList = llGetTataMotors.GetDataTataMotorsFromAPI(url, mobileunitId, columnList, timeFormat,tokenURL,clientID,
//								clientSecret,grantType,mobileUnitTimestamp.getLicensePlate());	
//					
//					//Added on 06MAR2023
//					}
					else if(mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("KAPOORDIESELS")) {
						
						
						url = url + "/" + mobileUnitTimestamp.getLicensePlate();
						int llDtInt = (mobileUnitTimestamp.getDataInterval() * 1000);
						Date llToD = new Date();
						long llTo = llToD.getTime() - llDtInt;
						Date llFromD = new Date(llTo);

						Session session = HibernateUtil.beginTransaction();
						if (mobileUnitTimestamp.getLastProcessedTimeKPR() == null) {
							List llGpsPollData = session.createQuery(" from GpsApiPolling"
									+ " where id.mobileunitid = '" + mobileunitId + "'" + " and id.companyId = "
									+ mobileUnitTimestamp.getCompanyId() + " and id.dataProviderName = '"
									+ mobileUnitTimestamp.getDataProviderName() + "'").list();
							if (llGpsPollData.size() > 0) {
								GpsApiPolling lPollData = (GpsApiPolling) llGpsPollData.get(0);
								llFromD = lPollData.getLastGpsdateTime();								
								long diff = (llToD.getTime() - llFromD.getTime());																
								 if(diff > (CommonConstants.KAPOOR_DIESELS_HOURS * 60 *60*1000) )
								 {
									 Calendar cal = Calendar.getInstance();
									 cal.setTime(llToD);
									 cal.add(Calendar.HOUR_OF_DAY, CommonConstants.KAPOOR_DIESELS_DIFF_HOURS);
									 llFromD = cal.getTime();
								 }
							}
						} else if (mobileUnitTimestamp.getLastProcessedTimeKPR() != null) {
							llFromD = mobileUnitTimestamp.getLastProcessedTimeKPR();
						}
						if (mobileUnitTimestamp.getMileage() == 0) {
							List lmGpsDataList = session.createQuery(" from Gpsdata " + " where id.mobileunitid = '"
									+ mobileunitId + "'" + " order by id.gpsdate desc ").setMaxResults(1).list();
							if (lmGpsDataList.size() > 0) {
								Gpsdata lGpsData = (Gpsdata) lmGpsDataList.get(0);
								mobileUnitTimestamp.setMileage(lGpsData.getMileage());
							}
						}
						
						String reqParam = mobileUnitTimestamp.getApiReqParams();			
						//log.info("Inside KAPOORDIESELS Api");
					
						String tokenURL = mobileUnitTimestamp.getTokenUrl();
						String clientID = mobileUnitTimestamp.getTokenReqParams();
						String clientSecret = mobileUnitTimestamp.getApiReqParams();
						String grantType = mobileUnitTimestamp.getTokenReqParams();
						String dataProviderName = mobileUnitTimestamp.getDataProviderName();
						int dataProviderId = mobileUnitTimestamp.getDataProviderId();
						
						log.info(mobileUnitTimestamp.getMobileUnitId()+ " time : "+new Date());
						url = url.replace("licensePlate", mobileUnitTimestamp.getMobileUnitId());
						
						HashMap<String, String> columnList = new HashMap<String, String>();

						String timeFormat = "MM/dd/yyyy h:mm:ss a";
						
						KapoorDieselsGetData llGetKapoorDiesels = new KapoorDieselsGetData();	
						dataList = llGetKapoorDiesels.GetDataKapoorDieselsFromAPI(url, mobileunitId, timeFormat,tokenURL,clientID,
								clientSecret,grantType,mobileUnitTimestamp.getLicensePlate(),mobileUnitTimestamp.getMileage(),dataProviderId,dataProviderName,mobileUnitTimestamp);	
						
						if (dataList != null && dataList.size() > 0) {
							log.info("Data fetching for unit : " + mobileunitId + " data size : " + dataList.size()
									+ " last gps update time : " + dataList.get(dataList.size() - 1).getGpsdate());
							mobileUnitTimestamp.setLastProcessedTimeKPR(dataList.get(dataList.size() - 1).getGpsdate());
							mobileUnitTimestamp.setMileage(dataList.get(dataList.size() - 1).getMileage());
						}
						//Addition ended here 06MAR2023
					}else if (mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("ATIC-MZONE")) {
//						url = "https://live.mzoneweb.net/mzone62.api/LastKnownPositions";
						url = url + "(" + mobileUnitTimestamp.getVehicleNativeName()+")";
//						String reqParam = mobileUnitTimestamp.getApiReqParams();						
						String tokenURL = mobileUnitTimestamp.getTokenUrl();
						String clientID = mobileUnitTimestamp.getAticClientId();
						String clientSecret = mobileUnitTimestamp.getAticClientSecret();
						String grantType = mobileUnitTimestamp.getAticGrantType();
						String userName = mobileUnitTimestamp.getAticUserName();
						String password = mobileUnitTimestamp.getAticPassword();
						String dataProviderName = mobileUnitTimestamp.getDataProviderName();
						int dataProviderId = mobileUnitTimestamp.getDataProviderId();
//						String reqParam = "JSON";						
//						String tokenURL = "https://login.mzoneweb.net/connect/token";
//						String clientID = "mz-aticapi";
//						String clientSecret = "Lj4f6NHn.Tz6#KycUgsvEMiL";
//						String grantType = "password";
//						String userName = "tliadmin";
//						String password = "tliadmin";
//						String dataProviderName = "ATIC";
//						String apiReqParam = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjlDNTg1RjFFODkzM0Q4RDJDMkJGRjdEQkIxQkRFMjBGRTFCNjVDNUEiLCJ0eXAiOiJKV1QiLCJ4NXQiOiJuRmhmSG9rejJOTEN2X2Zic2IzaUQtRzJYRm8ifQ.eyJuYmYiOjE2OTE0ODk4OTMsImV4cCI6MTY5MTQ5MzQ5MywiaXNzIjoiaHR0cHM6Ly9sb2dpbi5tem9uZXdlYi5uZXQiLCJhdWQiOlsiaHR0cHM6Ly9sb2dpbi5tem9uZXdlYi5uZXQvcmVzb3VyY2VzIiwibXo2LWFwaSJdLCJjbGllbnRfaWQiOiJtei1hdGljYXBpIiwic3ViIjoiYTdmNzlkMmItOGM1OS00ODcwLTkzMzUtMzA4NWFjYzczMmZlIiwiYXV0aF90aW1lIjoxNjkxNDg5ODkzLCJpZHAiOiJsb2NhbCIsIm16X3VzZXJuYW1lIjoidGxpYWRtaW4iLCJtel91c2VyZ3JvdXBfaWQiOiIwMDAwMDAwMC0wMDAwLTAwMDAtMDAwMC0wMDAwMDAwMDAwMDAiLCJtel9zaGFyZF9jb2RlIjoiRU1FQSIsInNjb3BlIjpbIm16X3VzZXJuYW1lIiwib3BlbmlkIiwibXo2LWFwaS5hbGwiXSwiYW1yIjpbInB3ZCJdfQ.LUAEQt7MR75edFzWcPZkdJLZ5YPoqVKAlqroGrg7L3pqKlQvR8ykeR7lrK_R7CZ7r0YwyMoHs49GVQdUiT1MoNawDrNIOnVd6MQhqkR6G6HI97q1WnP6casCwvShy1F1-GQD1-EmLBQTCaacJq_nvJ1BzWsV4Bk2kR1iOBhQYmWv6MIBfbBQUOehYCbXyKkeLZyRAflSG-yVKCoL6tTabDu5cClpl2mDmANTN1HAeB_5joOn1ijCq9H39jTansINN_CcB-LdAafFWAngEPr9HOby_1xgMShA934EF-0fIf_-2mEpheES_azPQOOaS1jfCngeWnQRiyW7W7Imu3MYikNB3CMtpUT6k0wu8CkwrcarC73YvtwdTpNmmadI7R5jUdrxDrSlR-CgW6uIq3UmUZS9s3nS0cY97264FP-wb3iROfs-hct3ZxuFd2gi8hB4EzvhysEftvP9HHdNKGhLwkUO-DHpt1G2cYifNBZY211VsUmkn9QGXsGLAMGh9VzvfiJr8ZDV7gPfaCf1qmrjHPb8O7sI_8nnlbdvSvfj6sLG6ic8X3HKF-flsy1vbG9E98xCDr15GpMNXSZBQiHie0ATC6fb-RBk0WfCj0F_NIGCga56--qXGWuiqX831A8MwIYYThaJVnsbak9nKGxIuhuZYvWvopEfN7RKt1jcU2c";
//						int dataProviderId = 14;
						log.info(mobileUnitTimestamp.getMobileUnitId()+ " time : "+new Date());
//						url = url.replace("licensePlate", mobileUnitTimestamp.getMobileUnitId());

						HashMap<String, String> columnList = new HashMap<String, String>();
						columnList.put("ignition", "ignitionStatus");
						columnList.put("gpsdate", "lastSeen");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						String timeFormat = "yyyy-MM-dd HH:mm:ss";

						ATICGetData lGetAtic = new ATICGetData();	
						dataList = lGetAtic.GetDataATICMzoneFromAPI(url, mobileunitId, columnList, timeFormat,tokenURL,clientID,
								clientSecret,grantType,mobileUnitTimestamp.getLicensePlate(),userName,password,dataProviderId,dataProviderName,mobileUnitTimestamp);	
					}else if (mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("TLI-GENTR")) {
//						url = "https://tracking.generic.net.in/api/latestlocationByvehicalNumber?username=rohtas.logistic@gmail.com&password=Rohtas@12345&vehicalNumber=";
						url = url + mobileUnitTimestamp.getLicensePlate();
						HashMap<String, String> columnList = new HashMap<String, String>();
						columnList.put("ignition", "ignition");
						columnList.put("gpsdate", "gpsDateTime");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						String timeFormat = "MM/dd/yyyy h:mm:ss a";
						
//						dataList = api.GetDataFromAPIGENTR(url, mobileunitId, columnList, timeFormat);
						dataList = api.GetDataFromAPIGENTRNew(url, mobileunitId, columnList, timeFormat);
					} else if(mobileUnitTimestamp.getDataProviderName().equalsIgnoreCase("XPINDIA")) {
						url = url + mobileUnitTimestamp.getApiReqParams() +mobileUnitTimestamp.getLicensePlate();
						HashMap<String, String> columnList = new HashMap<String, String>();
						columnList.put("ignition", "ignition");
						columnList.put("gpsdate", "gpsDateTime");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						String timeFormat = "MM/dd/yyyy h:mm:ss a";
						
						dataList = api.GetDataFromAPIZXPIndia(url, mobileunitId, columnList, timeFormat,mobileUnitTimestamp);
					}
				}
				if (dataList == null) {
					mobileUnitTimestamp.setLastResponseCode(-1);
					dataList = new ArrayList<GPSDataModel>();
				}

				// if(dataList.size() <= 0) {
				/*
				 * BambooDB lBambooDB = null; Date gpsdate = new Date(); gpsdate.setHours(0);
				 * gpsdate.setMinutes(0); gpsdate.setSeconds(0);
				 * 
				 * DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); String
				 * gpsdateStr = formatstr.format(gpsdate);
				 * 
				 * Session session = HibernateUtil.beginTransaction(); ArrayList<Gpsdata>
				 * gpsdataCheck = HibernateUtil.castList(Gpsdata.class,
				 * session.createQuery(" from Gpsdata" + " where id.mobileunitid = '" +
				 * mobileunitId + "'" + " and id.gprsdate >= '" + gpsdateStr
				 * +"' order by id.gprsdate desc").setMaxResults(1).list());
				 * 
				 * HibernateUtil.commit(); if(gpsdataCheck.size() > 0){ Gpsdata gpsdata =
				 * gpsdataCheck.get(0);
				 * 
				 * GPSDataModel model = new GPSDataModel(); model.setMobileunitid(mobileunitId);
				 * model.setAcc(gpsdata.isAcc());
				 * model.setGpsdate(gpsdata.getId().getGpsdate()); model.setGprsdate(new
				 * Date()); model.setSpeed(gpsdata.getSpeed());
				 * model.setDirection(gpsdata.getDirection());
				 * model.setLatitude(gpsdata.getLatitude());
				 * model.setLongitude(gpsdata.getLongitude());
				 * 
				 * BambooData lBambooData = BambooData.getInstance(model); if(lBambooData !=
				 * null) { // String mobileUnitID = lBambooData.mobileUnitID; lBambooDB = new
				 * BambooDB(lBambooData); lBambooDB.isLocationData = true;
				 * databasePoolManager.ProcessUnitData(lBambooDB); lBambooDB = null; } }
				 */

				/*
				 * GPSDataModel model = new GPSDataModel(); model.setMobileunitid(mobileunitId);
				 * model.setAcc(false); model.setGpsdate(new Date()); model.setGprsdate(new
				 * Date()); model.setSpeed((short) 0); model.setDirection((short) 0);
				 * model.setLatitude(0); model.setLongitude(0); dataList.add(model);
				 * 
				 * log.info("Dummy data Insert for unit " + mobileunitId + " date: " + new
				 * Date());
				 */
				// }/*else*/

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
								if (lBambooData != null) {
									// String mobileUnitID = lBambooData.mobileUnitID;
									lBambooDB = new BambooDB(lBambooData);
									lBambooDB.isLocationData = true;
									databasePoolManager.ProcessUnitData(lBambooDB);
									lBambooDB = null;

								}
							}
						}

						mobileUnitTimestamp.setLastResponseCode(0);
						mobileUnitTimestamp.setLastResponseTime(new Date());
					}

					log.info("Thread :" + Thread.currentThread().getId() + "; After Insert Data : " + mobileunitId + " - "
							+ mobileUnitTimestamp.getDataProviderName() + " - " + dataList.size() + " ; " + new Date());
					mobileUnitTimestamp.setLastProcessedTime(new Date());
					mUnitTimestampHash.put(mobileunitId, mobileUnitTimestamp);

					JsonMethodAPI lGPSAPIPolling = new JsonMethodAPI();
					//The following function is commented due to performance issue on 27JUL2026
					// lGPSAPIPolling.InsertGPSAPIPolling(mobileUnitTimestamp, dataList);


			}
			// log.info("MobileUnitTaskRunnable End: " + Thread.currentThread().getId() +"
			// unit " + mobileunitId + " date: " + new Date());

		} catch (Exception e) {
			log.info("Timeout Exception", e);
			e.printStackTrace();
			HibernateUtil.rollback();
		} finally {
			HibernateUtil.close();
		}
	}

	public String getMobileunitId() {
		return mobileunitId;
	}

	public void setMobileunitId(String mobileunitId) {
		this.mobileunitId = mobileunitId;
	}

	public Hashtable<String, MobileUnitTimestamp> getmUnitTimestampHash() {
		return mUnitTimestampHash;
	}

	public void setmUnitTimestampHash(Hashtable<String, MobileUnitTimestamp> mUnitTimestampHash) {
		this.mUnitTimestampHash = mUnitTimestampHash;
	}

}
