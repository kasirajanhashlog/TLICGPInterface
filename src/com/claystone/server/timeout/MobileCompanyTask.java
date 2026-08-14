package com.claystone.server.timeout;

import java.net.URLEncoder;
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

public class MobileCompanyTask extends TimerTask {
	private String mobileCompanyId;
	private Hashtable<String, MobileUnitTimestamp> mCompanyTimestampHash;
	private Session mSession;
	private DatabasePoolManager databasePoolManager;
	private Logger log = Logger.getLogger(MobileCompanyTask.class);
	private String dataProviderName;

	public MobileCompanyTask() {
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
			// log.info("MobileUnitTaskRunnable start: " + Thread.currentThread().getId()
			// +" unit " + mobileunitId + " date: " + new Date());
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
				if (tokenType.equalsIgnoreCase("xml") && method.equalsIgnoreCase("JSON")) {
					XmlMethodAPI api = new XmlMethodAPI();
					if (mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAFLEET")) {
						String tokenUrl = mobileCompanyTimestamp.getTokenUrl();
						String reqParam = mobileCompanyTimestamp.getTokenReqParams();
						String tokenRes = api.GetDataFromTokenAPI(tokenUrl, reqParam, mobileCompanyId);
						if (tokenRes != null) {
							if (method.equalsIgnoreCase("JSON")) { // JSON API
								JsonMethodAPI apiRes = new JsonMethodAPI();
								url = url + tokenRes;
								// url = url + "/"+
								// mobileCompanyTimestamp.getLicensePlate()+"/"+(mobileCompanyTimestamp.getDataInterval()/60);
								HashMap<String, String> columnList = new HashMap<String, String>();
								columnList.put("ignition", "VehicleStatus");
								columnList.put("gpsdate", "LastUpdateDateTimeInUTC");
								columnList.put("latitude", "Latitude");
								columnList.put("longitude", "Longitude");
								columnList.put("speed", "Speed");
								columnList.put("licensePlate", "RegistrationNumber");
								String timeFormat = "dd/MM/yyyy HH:mm:ss";
								// log.info(mobileCompanyId+ " - " +
								// mobileCompanyTimestamp.getDataProviderName() +" Before Get TATAFLEET Data
								// List : "+ new Date());
								dataList = apiRes.GetDataFromTataAPI(url, mobileCompanyId, columnList, timeFormat,
										mobileCompanyTimestamp.getDataProviderName());
							}
						}
					}
				} else if (method.equalsIgnoreCase("JSON")) {
					if (mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("FLEETJACK")) {
						JsonMethodAPI apiRes = new JsonMethodAPI();
						String tokenRes = mobileCompanyTimestamp.getTokenReqParams();
						HashMap<String, String> columnList = new HashMap<String, String>();
						columnList.put("ignition", "ignition");
						columnList.put("gpsdate", "lastHeardDateTime");
						columnList.put("latlng", "latlng");
						columnList.put("speed", "speed");
						columnList.put("licensePlate", "vehicleno");
						String timeFormat = "yyyy-MM-dd HH:mm:ss";
						// log.info(mobileCompanyId+ " - " +
						// mobileCompanyTimestamp.getDataProviderName() +" Before Get FLEETJACK Data
						// List : "+ new Date());
						dataList = apiRes.GetDataFromFleetAPI(url, mobileCompanyId, columnList, timeFormat, tokenRes);

					} else if (mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("GOODSMOVER")) {
						JsonMethodAPI apiRes = new JsonMethodAPI();
						url = url + "/" + mobileCompanyTimestamp.getApiReqParams();
						HashMap<String, String> columnList = new HashMap<String, String>();
						// columnList.put("ignition", "VehicleStatus");
						columnList.put("gpsdate", "on_date");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						columnList.put("licensePlate", "number");
						String timeFormat = "dd-MMM-yyyy HH:mm:ss";
						// log.info(mobileCompanyId+ " - " +
						// mobileCompanyTimestamp.getDataProviderName() +" Before Get GOODSMOVER Data
						// List : "+ new Date());
						dataList = apiRes.GetDataFromTataAPI(url, mobileCompanyId, columnList, timeFormat,
								mobileCompanyTimestamp.getDataProviderName());
					} else if (mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("GTROPY")) {
						JsonMethodAPI apiRes = new JsonMethodAPI();
						url = url + "/" + mobileCompanyTimestamp.getApiReqParams();
						HashMap<String, String> columnList = new HashMap<String, String>();
						// columnList.put("ignition", "VehicleStatus");
						columnList.put("licensePlate", "vname");
						columnList.put("gpsdate", "dttime");
						columnList.put("latitude", "lat");
						columnList.put("longitude", "lngt");
						columnList.put("speed", "speed");
						String timeFormat = "yyyy-MM-dd HH:mm:ss";
						// log.info(mobileCompanyId+ " - " +
						// mobileCompanyTimestamp.getDataProviderName() +" Before Get GTROPY Data List :
						// "+ new Date());
						dataList = apiRes.GetDataFromGTROPYAPI(url, mobileCompanyId, columnList, timeFormat,
								mobileCompanyTimestamp.getDataProviderName());
					} 
					else if (mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("VAMOSYS")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("VAMOSYSTRL")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("VAMOSYS-OEM")) {
						JsonMethodAPI apiRes = new JsonMethodAPI();
						url = url + mobileCompanyTimestamp.getApiReqParams();
						HashMap<String, String> columnList = new HashMap<String, String>();
						// columnList.put("ignition", "VehicleStatus");
						columnList.put("licensePlate", "shortName");
						columnList.put("date", "date");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						String timeFormat = "SSS";
						// log.info(mobileCompanyId+ " - " +
						// mobileCompanyTimestamp.getDataProviderName() +" Before Get GTROPY Data List :
						// "+ new Date());
						dataList = apiRes.GetDataFromVamosysAPI(url, mobileCompanyId, columnList, timeFormat,
								mobileCompanyTimestamp.getDataProviderName());
					} else if (mobileCompanyTimestamp.getDataProviderName()
							.equalsIgnoreCase("SAMRX")) { /* Added on 2021-Mar-09 for SAMRX */
						SAMRXGetData apiRes = new SAMRXGetData();
						url = url + mobileCompanyTimestamp.getApiReqParams();
						HashMap<String, String> columnList = new HashMap<String, String>();
						columnList.put("licensePlate", "vehicleNo");
						columnList.put("date", "timeStamp");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						String timeFormat = "yyyy-MM-dd HH:mm:ss";
						dataList = apiRes.GetDataFromSAMRX(url, mobileCompanyId, columnList, timeFormat,
								mobileCompanyTimestamp.getDataProviderName());
					} else if (mobileCompanyTimestamp.getDataProviderName()
							.equalsIgnoreCase("WHEELSEYE")) { /* Added on 2021-Mar-09 for SAMRX */
						WHEELSEYEGetData apiRes = new WHEELSEYEGetData();
						url = url + mobileCompanyTimestamp.getApiReqParams();
						HashMap<String, String> columnList = new HashMap<String, String>();
						columnList.put("licensePlate", "vehicleNumber");
						columnList.put("date", "createdDate");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("ignition", "ignition");
						columnList.put("gpsdate", "createdDate");
						columnList.put("speed", "speed");
						String timeFormat = "yyyy-MM-dd HH:mm:ss";
						dataList = apiRes.GetDataFromWHEELSEYE(url, mobileCompanyId, columnList, timeFormat,
								mobileCompanyTimestamp.getDataProviderName());
					}else if(mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("GLOB-NMIPL")) { /* Added on 2023-Mar-31 for GLOBAL NISSAN */
						GLOBALNISSANGetData apiRes = new GLOBALNISSANGetData();
//						url = url + mobileCompanyTimestamp.getApiReqParams();
						HashMap<String, String> columnList = new HashMap<String, String>();
						columnList.put("licensePlate", "vehicleNo");
						columnList.put("date", "createdDate");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("ignition", "ignition");
						columnList.put("gpsdate", "trackDatetime");
						columnList.put("speed", "speed");
						String timeFormat = "MM/dd/yyyy h:mm:ss a";
						dataList = apiRes.GetDataFromGLOBALNISSAN(url, mobileCompanyId, columnList, timeFormat,
								mobileCompanyTimestamp.getDataProviderName());
					}else if(mobileCompanyTimestamp.getDataProviderName()
							.equalsIgnoreCase("FLEETRADAR")) {
						FleetRadarGetData apiRes = new FleetRadarGetData();
						url = url + mobileCompanyTimestamp.getApiReqParams();
						String tokenURL = mobileCompanyTimestamp.getTokenUrl();
						String clientID = mobileCompanyTimestamp.getTokenReqParams();
						String clientSecret = mobileCompanyTimestamp.getApiReqParams();
						String grantType = mobileCompanyTimestamp.getTokenReqMethod();
						String dataProviderName = mobileCompanyTimestamp.getDataProviderName();

						HashMap<String, String> columnList = new HashMap<String, String>();
						columnList.put("licensePlate", "plateNo");
						columnList.put("ignition", "ignition");
						columnList.put("gpsdate", "gpsDateTime");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						columnList.put("speed", "speed");
						String timeFormat = "yyyy-MM-dd HH:mm:ss";
						dataList = apiRes.GetDataFleetRadarFromAPI(url, mobileCompanyId, columnList, timeFormat,
								  tokenURL,  clientID, clientSecret,  grantType,
								mobileCompanyTimestamp.getDataProviderName());
					}else if(mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-1")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-HARI-1")//Added on 15SEP2023--For Tata Motors
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-SHREE")) {//Added on 15SEP2023--For Tata Motors
						TATAMotorAllVehiclesGetData apiRes = new TATAMotorAllVehiclesGetData();
						String tokenURL = mobileCompanyTimestamp.getTokenUrl();
						String clientID = mobileCompanyTimestamp.getTokenReqParams();
//						String clientSecret = mobileCompanyTimestamp.getApiReqParams();
						String accessToken = mobileCompanyTimestamp.getApiReqParams();
						if(accessToken.equals("")) {
							accessToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJOamxPbGw0RURrVlFzMWlWYmpsWVgwWVdPQzlvSUdMdGpxZlp0ZUdOSldZIn0.eyJleHAiOjE2OTQ4NTI3MjAsImlhdCI6MTY5NDg0OTEyMCwianRpIjoiZjA2OWJkYjUtM2QxZC00NTFkLWIxYjMtMjlkZjNhYzRmNTE1IiwiaXNzIjoiaHR0cHM6Ly9jdnBhdXRoLmFwaS50YXRhbW90b3JzL2F1dGgvcmVhbG1zL2V4dGVybmFsIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6IjRlNmVlNzYwLWNjMjQtNGE0Ni04NmI0LTg0ZjVhZGM1MDJhNiIsInR5cCI6IkJlYXJlciIsImF6cCI6IjU2NGUxNjczLWNjOTktNDljOC05YjRjLTg1NjU3MjBiOWU3MSIsImFjciI6IjEiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiY3ZwX2V4dGVybmFsX3VzZXIiLCJvZmZsaW5lX2FjY2VzcyIsInVtYV9hdXRob3JpemF0aW9uIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiNTY0ZTE2NzMtY2M5OS00OWM4LTliNGMtODU2NTcyMGI5ZTcxIjp7InJvbGVzIjpbInVtYV9wcm90ZWN0aW9uIl19LCJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6InByb2ZpbGUgZW1haWwiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsImNsaWVudElkIjoiNTY0ZTE2NzMtY2M5OS00OWM4LTliNGMtODU2NTcyMGI5ZTcxIiwiY2xpZW50SG9zdCI6IjM0LjkzLjQ3LjYyIiwicHJlZmVycmVkX3VzZXJuYW1lIjoic2VydmljZS1hY2NvdW50LTU2NGUxNjczLWNjOTktNDljOC05YjRjLTg1NjU3MjBiOWU3MSIsImNsaWVudEFkZHJlc3MiOiIzNC45My40Ny42MiJ9.UX1ay61mN6JqrbR43MFlXnAozS7ygE87z49nqd_uZYdoSkJHiZvntZb4WxNhLxnFCNYmFQk4F0Gek58IgmVTjAbgIMU4K2pJOOmuqeLF4EBvzO1G_m8JWHNFjatzCglSxNildiLipaoKZUOziDllKVlHWCS-8Tr37qvV-93gRgvZf4zAR0FDHB3R_uM1FAuIuP8P0yRYG9sI59bUAFaMPtTXaSWvM0PN_k5TY_DKdPTDPHkrq7uiIljN6bkqgSs5WneBHIiErPXnqk7EWTuEyMov5yOnj2xVcbdYT8DwErzb7hK6iA-R6dmtx4ieGDuXeYYchlTOJIO84WipSX63oA";
						}
						String dataProviderName = mobileCompanyTimestamp.getDataProviderName();
						int dataProviderId = mobileCompanyTimestamp.getDataProviderId();

						HashMap<String, String> columnList = new HashMap<String, String>();
						columnList.put("ignition", "ignitionStatus");
						columnList.put("gpsdate", "lastSeen");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						String timeFormat = "yyyy-MM-dd HH:mm:ss";
//						dataList = apiRes.GetDataFromTATAMotors(url, mobileCompanyId, columnList, timeFormat,tokenURL,clientID,
//								clientSecret,dataProviderId,dataProviderName,mobileCompanyTimestamp);
						dataList = apiRes.GetDataFromTATAMotors(url, mobileCompanyId, columnList, timeFormat,tokenURL,clientID,
								accessToken,dataProviderId,dataProviderName,mobileCompanyTimestamp);
						
					}else if(mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-HARI-2")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-2")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-2-NEW")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-2-7352")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-MUTAMN")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-3519")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-RKT")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-3441")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-775")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-775N")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-RK850")
						    ||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-RK85079")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMR-TLISPD")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-0654")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-JPL")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-SMC")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-6842")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-885N")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-9187")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-4477")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-SKOD")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-4914")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-6883")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-0215")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-6761")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-67DFE")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-88096")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-D2FD")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-6093")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-1875")
						    ||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-3008")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-KA42C0")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-0610")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTORSVT")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTORSKO")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTORSVT2")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTORJPL2")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTORDMC")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR5808")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR8849")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-RAO")
						    ||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-SUB")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR-AMIT")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR8311")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TATAMOTOR_LAKMI")) {
						TATAMotorAllVehiclesGetData apiRes = new TATAMotorAllVehiclesGetData();
						String tokenURL = mobileCompanyTimestamp.getTokenUrl();
						String clientID = mobileCompanyTimestamp.getTokenReqParams();
//						String clientSecret = mobileCompanyTimestamp.getApiReqParams();
						String accessToken = mobileCompanyTimestamp.getApiReqParams();
						if(accessToken.equals("")) {
							accessToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJOamxPbGw0RURrVlFzMWlWYmpsWVgwWVdPQzlvSUdMdGpxZlp0ZUdOSldZIn0.eyJleHAiOjE2OTQ4NTI3MjAsImlhdCI6MTY5NDg0OTEyMCwianRpIjoiZjA2OWJkYjUtM2QxZC00NTFkLWIxYjMtMjlkZjNhYzRmNTE1IiwiaXNzIjoiaHR0cHM6Ly9jdnBhdXRoLmFwaS50YXRhbW90b3JzL2F1dGgvcmVhbG1zL2V4dGVybmFsIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6IjRlNmVlNzYwLWNjMjQtNGE0Ni04NmI0LTg0ZjVhZGM1MDJhNiIsInR5cCI6IkJlYXJlciIsImF6cCI6IjU2NGUxNjczLWNjOTktNDljOC05YjRjLTg1NjU3MjBiOWU3MSIsImFjciI6IjEiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiY3ZwX2V4dGVybmFsX3VzZXIiLCJvZmZsaW5lX2FjY2VzcyIsInVtYV9hdXRob3JpemF0aW9uIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiNTY0ZTE2NzMtY2M5OS00OWM4LTliNGMtODU2NTcyMGI5ZTcxIjp7InJvbGVzIjpbInVtYV9wcm90ZWN0aW9uIl19LCJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6InByb2ZpbGUgZW1haWwiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsImNsaWVudElkIjoiNTY0ZTE2NzMtY2M5OS00OWM4LTliNGMtODU2NTcyMGI5ZTcxIiwiY2xpZW50SG9zdCI6IjM0LjkzLjQ3LjYyIiwicHJlZmVycmVkX3VzZXJuYW1lIjoic2VydmljZS1hY2NvdW50LTU2NGUxNjczLWNjOTktNDljOC05YjRjLTg1NjU3MjBiOWU3MSIsImNsaWVudEFkZHJlc3MiOiIzNC45My40Ny42MiJ9.UX1ay61mN6JqrbR43MFlXnAozS7ygE87z49nqd_uZYdoSkJHiZvntZb4WxNhLxnFCNYmFQk4F0Gek58IgmVTjAbgIMU4K2pJOOmuqeLF4EBvzO1G_m8JWHNFjatzCglSxNildiLipaoKZUOziDllKVlHWCS-8Tr37qvV-93gRgvZf4zAR0FDHB3R_uM1FAuIuP8P0yRYG9sI59bUAFaMPtTXaSWvM0PN_k5TY_DKdPTDPHkrq7uiIljN6bkqgSs5WneBHIiErPXnqk7EWTuEyMov5yOnj2xVcbdYT8DwErzb7hK6iA-R6dmtx4ieGDuXeYYchlTOJIO84WipSX63oA";
						}
						String dataProviderName = mobileCompanyTimestamp.getDataProviderName();
						int dataProviderId = mobileCompanyTimestamp.getDataProviderId();

						HashMap<String, String> columnList = new HashMap<String, String>();
						columnList.put("ignition", "ignitionStatus");
						columnList.put("gpsdate", "lastSeen");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						String timeFormat = "yyyy-MM-dd HH:mm:ss";
						dataList = apiRes.GetDataFromTATAMotors_GET_METHORD(url, mobileCompanyId, columnList, timeFormat,tokenURL,clientID,
								accessToken,dataProviderId,dataProviderName,mobileCompanyTimestamp);
						
					}else if(mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("PAIZOGPS")
							|| mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("PAIZOGPS-6771")){
						PaizoGpsGetData apiRes = new PaizoGpsGetData();
//						JsonMethodAPI apiRes = new JsonMethodAPI();
						url = url + mobileCompanyTimestamp.getApiReqParams();
						String tokenURL = mobileCompanyTimestamp.getTokenUrl();
						String clientID = mobileCompanyTimestamp.getTokenReqParams();
						String clientSecret = mobileCompanyTimestamp.getApiReqParams();
						String grantType = mobileCompanyTimestamp.getTokenReqMethod();
						String dataProviderName = mobileCompanyTimestamp.getDataProviderName();

						HashMap<String, String> columnList = new HashMap<String, String>();
						columnList.put("licensePlate", "plateNo");
						columnList.put("ignition", "ignition");
						columnList.put("gpsdate", "gpsDateTime");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						columnList.put("speed", "speed");
						String timeFormat = "yyyy-MM-dd HH:mm:ss";
						dataList = apiRes.GetDataPaizoGpsFromAPI(url, mobileCompanyId, columnList, timeFormat,
								  tokenURL,  clientID, clientSecret,  grantType,
								mobileCompanyTimestamp.getDataProviderName());
//						dataList = apiRes.GetDataFromPaizoGpsAPI(url, mobileCompanyId, columnList, timeFormat,
//								mobileCompanyTimestamp.getDataProviderName());
					}else if(mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("ASHOK-LEYLAND")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("ASHOK-SKAT")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("ASHOK-GLOBAL")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("ASHOK-LLTCI")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("ASHOK-LAXMI")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("ASHOK-LL-TLISP")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("ASHOK-LL-JPL")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("ASHOK-LLHMTS")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("ASHOK-JPL-1233")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("ASHOK8884")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("ASHOK2867")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("ASHOKLEYL-SKT")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("ASHOK_VIKASH")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("ASHOK_SRTRANS")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("ASHOK-RK-0840")
							||mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("ASHOK_STLL")) {
						AshokLeylandGetData apiRes = new AshokLeylandGetData();
						url = url + mobileCompanyTimestamp.getApiReqParams();
						HashMap<String, String> columnList = new HashMap<String, String>();
						columnList.put("licensePlate", "vehicleNo");
						columnList.put("date", "createdDate");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("ignition", "ignition");
						columnList.put("gpsdate", "trackDatetime");
						columnList.put("speed", "speed");
						String timeFormat = "MM/dd/yyyy h:mm:ss a";
						dataList = apiRes.GetDataFromAshokLeyland(url, mobileCompanyId, columnList, timeFormat,
								mobileCompanyTimestamp.getDataProviderName());
					}else if(mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("ASSET_TRACKER") 
							|| mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("ASSETTRACK") /* Added on 2023-Mar-31 for GLOBAL NISSAN */
							|| mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("ASSETTRACKTVS")) {
						ASSET_TRACKETGetData apiRes = new ASSET_TRACKETGetData();
						String accessKey = mobileCompanyTimestamp.getApiReqParams();
						HashMap<String, String> columnList = new HashMap<String, String>();
						columnList.put("licensePlate", "vehicle");
						columnList.put("latitude", "lat");
						columnList.put("longitude", "lon");
						columnList.put("ignition", "ignition");
						columnList.put("gpsdate", "time");
						columnList.put("speed", "speed");
						String timeFormat = "MM/dd/yyyy h:mm:ss a";
						dataList = apiRes.GetDataFromASSET_TRACKET(url, mobileCompanyId, columnList, timeFormat,
								mobileCompanyTimestamp.getDataProviderName(),accessKey);
					}else if (mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("LOCONAV")) {
						LOCONOVA_GetData apiRes = new LOCONOVA_GetData();
						HashMap<String, String> columnList = new HashMap<String, String>();
						String apiReqParam = mobileCompanyTimestamp.getApiReqParams();
						columnList.put("licensePlate", "shortName");
						columnList.put("date", "date");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						String timeFormat = "SSS";
						
						String[] tokens=apiReqParam.split(",");  
						String userAthentication = tokens[0];
						String userId = tokens[1];
						
						dataList = apiRes.GetDataFromLOCONOVA(url, mobileCompanyId, columnList, timeFormat,
								mobileCompanyTimestamp.getDataProviderName(),userAthentication,userId);
					}else if (mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("SRTRANS")) {
						SRTRANSPORT_GetData apiRes = new SRTRANSPORT_GetData();
						HashMap<String, String> columnList = new HashMap<String, String>();
						String apiReqParam = mobileCompanyTimestamp.getApiReqParams();
						columnList.put("licensePlate", "shortName");
						columnList.put("date", "date");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						String timeFormat = "SSS";
						
						String[] tokens=apiReqParam.split(",");  
						String userAthentication = tokens[0];
						
						dataList = apiRes.GetDataFromSRTRANSPORT(url, mobileCompanyId, columnList, timeFormat,
								mobileCompanyTimestamp.getDataProviderName(),userAthentication);
					}else if (mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("SAMTRACK-2024")
							|| mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("SAMTRACK-TVS")) {
						SAMTRACK_GetData apiRes = new SAMTRACK_GetData();
						HashMap<String, String> columnList = new HashMap<String, String>();
						String apiReqParam = mobileCompanyTimestamp.getApiReqParams();
						url+=apiReqParam;
						columnList.put("licensePlate", "shortName");
						columnList.put("date", "date");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						String timeFormat = "SSS";
						
						String[] tokens=apiReqParam.split(",");  
						String userAthentication = tokens[0];
						
						dataList = apiRes.GetDataFromSAMTRACK(url, mobileCompanyId, columnList, timeFormat,
								mobileCompanyTimestamp.getDataProviderName());
					}else if (mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TRACKNOVAT")) {
						ISHIKA_GetData apiRes = new ISHIKA_GetData();
						HashMap<String, String> columnList = new HashMap<String, String>();
						String apiReqParam = mobileCompanyTimestamp.getApiReqParams();
						columnList.put("licensePlate", "shortName");
						columnList.put("date", "date");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						String timeFormat = "SSS";
						
						String[] apitokens=apiReqParam.split("&");  
						String token = apitokens[0];
						String user = apitokens[1];
						String pass = apitokens[2];
						String company = apitokens[3];
						String format = apitokens[4];
						
						String[] tokens=token.split("=");  
						String tokenkey = tokens[0];
						String tokenvalue = tokens[1];
						
						url+=tokenkey+"="+tokenvalue;
						
						String[] username=user.split("=");  
						String usernamekey = username[0];
						String usernamevalue = username[1];
						
						url+="&"+usernamekey+"="+usernamevalue;
						
						String[] passname=pass.split("=");  
						String passnamekey = passname[0];
						String passnamevalue = passname[1];
						
						url+="&"+passnamekey+"="+passnamevalue;
						
						String[] companyname=company.split("=");  
						String companynamekey = companyname[0];
						String companynamevalue = companyname[1];
						String encodedCompany = URLEncoder.encode(companynamevalue, "UTF-8");
						
						url+="&"+companynamekey+"="+encodedCompany;
						
						String[] formatname=pass.split("=");  
						String formatnamekey = formatname[0];
						String formatnamevalue = formatname[1];
						
						url+="&"+formatnamekey+"="+formatnamevalue;

						dataList = apiRes.GetDataFromISHIKA(url, mobileCompanyId, columnList, timeFormat,
								mobileCompanyTimestamp.getDataProviderName(),apiReqParam );
					}else if (mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("ROADO")) {
						ROADO_GetData apiRes = new ROADO_GetData();
						HashMap<String, String> columnList = new HashMap<String, String>();
						String apiReqParam = mobileCompanyTimestamp.getApiReqParams();
						columnList.put("licensePlate", "shortName");
						columnList.put("date", "date");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						String timeFormat = "SSS";
						
						dataList = apiRes.GetDataFromROADO(url, mobileCompanyId, columnList, timeFormat,
								mobileCompanyTimestamp.getDataProviderName(),apiReqParam);
					}else if(mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("ASSET_TRACKER_NIPPON")) { /* Added on 2023-Mar-31 for GLOBAL NISSAN */
						ASSET_TRACKETGetData apiRes = new ASSET_TRACKETGetData();
						String accessKey = mobileCompanyTimestamp.getApiReqParams();
						HashMap<String, String> columnList = new HashMap<String, String>();
						columnList.put("licensePlate", "vehicle");
						columnList.put("latitude", "lat");
						columnList.put("longitude", "lon");
						columnList.put("ignition", "ignition");
						columnList.put("gpsdate", "time");
						columnList.put("speed", "speed");
						String timeFormat = "MM/dd/yyyy h:mm:ss a";
						dataList = apiRes.GetDataFromASSET_TRACKET(url, mobileCompanyId, columnList, timeFormat,
								mobileCompanyTimestamp.getDataProviderName(),accessKey);
					}else if(mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("CJDARCL")) { /* Added on 2025-May-07 for TLICBU */
						CJDARCL_GetData apiRes = new CJDARCL_GetData();
						String accessKey = mobileCompanyTimestamp.getApiReqParams();
						HashMap<String, String> columnList = new HashMap<String, String>();
						columnList.put("licensePlate", "vehicle");
						columnList.put("latitude", "lat");
						columnList.put("longitude", "lon");
						columnList.put("ignition", "ignition");
						columnList.put("gpsdate", "time");
						columnList.put("speed", "speed");
						String timeFormat = "MM/dd/yyyy h:mm:ss a";
						dataList = apiRes.GetDataFromCJDARCL(url, mobileCompanyId, columnList, timeFormat,
								mobileCompanyTimestamp.getDataProviderName(),accessKey);
					}else if(mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("GTRACK")) { /* Added on 2025-May-07 for TLICBU */
						GTRACK_GetData apiRes = new GTRACK_GetData();
						String accessKey = mobileCompanyTimestamp.getApiReqParams();
						HashMap<String, String> columnList = new HashMap<String, String>();
						columnList.put("licensePlate", "vehicle");
						columnList.put("latitude", "lat");
						columnList.put("longitude", "lon");
						columnList.put("ignition", "ignition");
						columnList.put("gpsdate", "time");
						columnList.put("speed", "speed");
						String timeFormat = "MM/dd/yyyy h:mm:ss a";
						dataList = apiRes.GetDataFromGTRACK(url, mobileCompanyId, columnList, timeFormat,
								mobileCompanyTimestamp.getDataProviderName(),accessKey);
					}else if(mobileCompanyTimestamp.getDataProviderName()
							.equalsIgnoreCase("SAMTRACK_UBH")) {
						SamtrackUBHGetData apiRes = new SamtrackUBHGetData();
						String apiReqParams = mobileCompanyTimestamp.getApiReqParams();
						String tokenURL = mobileCompanyTimestamp.getTokenUrl();
						String clientID = mobileCompanyTimestamp.getTokenReqParams();
						String clientSecret = mobileCompanyTimestamp.getTokenReqParams();
						String grantType = mobileCompanyTimestamp.getTokenReqMethod();
						String dataProviderName = mobileCompanyTimestamp.getDataProviderName();

						HashMap<String, String> columnList = new HashMap<String, String>();
						columnList.put("licensePlate", "plateNo");
						columnList.put("ignition", "ignition");
						columnList.put("gpsdate", "gpsDateTime");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						columnList.put("speed", "speed");
						String timeFormat = "yyyy-MM-dd HH:mm:ss";
						dataList = apiRes.GetDataSamtrackFromAPI(url, mobileCompanyId, columnList, timeFormat,
								  tokenURL,  clientID, clientSecret,  grantType,
								mobileCompanyTimestamp.getDataProviderName());
					}else if (mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TRACKCON")) {
						TRACKCON_GetData apiRes = new TRACKCON_GetData();
						HashMap<String, String> columnList = new HashMap<String, String>();

						columnList.put("licensePlate", "shortName");
						columnList.put("date", "date");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						String timeFormat = "SSS";
					
						dataList = apiRes.GetDataFromTRACKCON(url, mobileCompanyId, columnList, timeFormat,
								mobileCompanyTimestamp.getDataProviderName());
					}else if (mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("DEVINFO")) {
						DEVINFO_GetData apiRes = new DEVINFO_GetData();
						HashMap<String, String> columnList = new HashMap<String, String>();

						columnList.put("licensePlate", "shortName");
						columnList.put("date", "date");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						String timeFormat = "SSS";
					
						dataList = apiRes.GetDataFromDEVINFO(url, mobileCompanyId, columnList, timeFormat,
								mobileCompanyTimestamp.getDataProviderName());
					}else if (mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("MOBILFOX")
							|| mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("MOBILFOX-TVS")) {
						MOBILFOX_GetData apiRes = new MOBILFOX_GetData();
						String accessKey = mobileCompanyTimestamp.getApiReqParams();
						HashMap<String, String> columnList = new HashMap<String, String>();

						columnList.put("licensePlate", "shortName");
						columnList.put("date", "date");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						String timeFormat = "SSS";
					
						dataList = apiRes.GetDataFromMOBILFOX(url, mobileCompanyId, columnList, timeFormat,
								mobileCompanyTimestamp.getDataProviderName(),accessKey);
					}else if(mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("EICHER-SKODA")
							|| mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("EICHER-KA42C01")
							|| mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("EICHER-SKT")
							|| mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("EICHER-5155")
							|| mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("EICHER_APR26")
							|| mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("EICHER-MH12YB46")
							|| mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("EICHER_SUMER")
							|| mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("EICHER-SST")
							|| mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("EICHER-4059")
							|| mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("EICHER_DEV")
							|| mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("EICHER_GLARE")
						|| mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("EICHER-KSSRPL")) {
						EICHER_GetData apiRes = new EICHER_GetData();
						String tokenURL = mobileCompanyTimestamp.getTokenUrl();
						String tokenReqParams = mobileCompanyTimestamp.getTokenReqParams();
						String apiUrl = mobileCompanyTimestamp.getApiUrl();
						String apiReqParams = mobileCompanyTimestamp.getApiReqParams();

						String dataProviderName = mobileCompanyTimestamp.getDataProviderName();
						int dataProviderId = mobileCompanyTimestamp.getDataProviderId();

						HashMap<String, String> columnList = new HashMap<String, String>();
						columnList.put("ignition", "ignitionStatus");
						columnList.put("gpsdate", "lastSeen");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						String timeFormat = "yyyy-MM-dd HH:mm:ss";
						dataList = apiRes.GetDataFromEICHER(url, mobileCompanyId, columnList, timeFormat,tokenURL,tokenReqParams,
								apiUrl,apiReqParams,dataProviderId,dataProviderName,mobileCompanyTimestamp);
						
					}else if (mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TBTRACK")) {
						TBTRACK_GetData apiRes = new TBTRACK_GetData();
						HashMap<String, String> columnList = new HashMap<String, String>();
						String apiReqParam = mobileCompanyTimestamp.getApiReqParams();

						columnList.put("licensePlate", "shortName");
						columnList.put("date", "date");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						String timeFormat = "SSS";
						
						String[] tokens=apiReqParam.split(",");  
						String userAthentication = tokens[0];
						
						dataList = apiRes.GetDataFromTBTRACK(url, mobileCompanyId, columnList, timeFormat,
								mobileCompanyTimestamp.getDataProviderName(),apiReqParam);
					}else if (mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("ECALYX")) {
						ECALYX_GetData apiRes = new ECALYX_GetData();
						HashMap<String, String> columnList = new HashMap<String, String>();

						columnList.put("licensePlate", "shortName");
						columnList.put("date", "date");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						String timeFormat = "SSS";
						
						dataList = apiRes.GetDataFromECALYX(url, mobileCompanyId, columnList, timeFormat,
								mobileCompanyTimestamp.getDataProviderName());
					}else if(mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("TRACKNOV")) {
						TRACKNOV_GetData apiRes = new TRACKNOV_GetData();
						String tokenURL = mobileCompanyTimestamp.getTokenUrl();
						String tokenReqParams = mobileCompanyTimestamp.getTokenReqParams();
						String apiUrl = mobileCompanyTimestamp.getApiUrl();
						String apiReqParams = mobileCompanyTimestamp.getApiReqParams();

						String dataProviderName = mobileCompanyTimestamp.getDataProviderName();
						int dataProviderId = mobileCompanyTimestamp.getDataProviderId();

						HashMap<String, String> columnList = new HashMap<String, String>();
						columnList.put("ignition", "ignitionStatus");
						columnList.put("gpsdate", "lastSeen");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						String timeFormat = "yyyy-MM-dd HH:mm:ss";
						dataList = apiRes.GetDataFromTRACKNOV(url, mobileCompanyId, columnList, timeFormat,tokenURL,tokenReqParams,
								apiUrl,apiReqParams,dataProviderId,dataProviderName,mobileCompanyTimestamp);
						
					}else if (mobileCompanyTimestamp.getDataProviderName().equalsIgnoreCase("EGTRACKERS")) {
						EGTRACKERS_GetData apiRes = new EGTRACKERS_GetData();
						HashMap<String, String> columnList = new HashMap<String, String>();

						columnList.put("licensePlate", "shortName");
						columnList.put("date", "date");
						columnList.put("latitude", "latitude");
						columnList.put("longitude", "longitude");
						columnList.put("speed", "speed");
						String timeFormat = "SSS";
					
						dataList = apiRes.GetDataFromEGTRACKERS(url, mobileCompanyId, columnList, timeFormat,
								mobileCompanyTimestamp.getDataProviderName());
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
						String mobileunitid = model.getMobileunitid();
						String licensePlate = model.getLicensePlate();
						DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						String gpsdateStr = formatstr.format(gpsdate);

						List<?> mobileUnitList = null;
						Session session = HibernateUtil.beginTransaction();
//						System.out.println("LicensePlate: "+licensePlate);
						if(licensePlate!=null && !licensePlate.equals("")) {
							mobileUnitList = session.createSQLQuery(
									" select vmu.mobileunitid ,v.transporter_name from vehicle_mobile_unit vmu,"
											+ " mobile_unit m, vehicle v where v.company_id = "
											+ mobileCompanyTimestamp.getCompanyId() + " and v.license_plate = '"
											+ model.getLicensePlate() + "'" + " and v.record_status = 0"
											+ " and vmu.vehicle_id = v.vehicle_id " + " and vmu.last_effective_date >= '"
											+ new Date() + "'" + " and vmu.is_active = true"
											+ " and m.mobileunitid = vmu.mobileunitid ")
									.list();
						}else if(mobileunitid!=null && !mobileunitid.equals("")) {
							mobileUnitList = session.createSQLQuery(
									" select vmu.mobileunitid ,v.transporter_name from vehicle_mobile_unit vmu,"
											+ " mobile_unit m, vehicle v where v.company_id = "
											+ mobileCompanyTimestamp.getCompanyId() + " and vmu.mobileunitid = '"
											+ model.getMobileunitid() + "'" + " and v.record_status = 0"
											+ " and vmu.vehicle_id = v.vehicle_id " + " and vmu.last_effective_date >= '"
											+ new Date() + "'" + " and vmu.is_active = true"
											+ " and m.mobileunitid = vmu.mobileunitid ")
									.list();
						}
						

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
