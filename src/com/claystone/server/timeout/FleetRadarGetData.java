package com.claystone.server.timeout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

import com.claystone.common.utils.CommonConstants;
import com.claystone.common.utils.CommonMethods;
import com.claystone.db.DataProvider;
import com.claystone.server.util.HibernateUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class FleetRadarGetData {
	private static Logger log;

	public FleetRadarGetData() {
		log = Logger.getLogger(FleetRadarGetData.class);
	}
	public ArrayList<GPSDataModel> GetDataFleetRadarFromAPI(String URL, String companyID, HashMap<String, String> columnList,
			String timeformat,  String tokenURL, String clientID,
			String clientSecret, String grantType,String dataProviderName) throws JSONException, org.json.simple.parser.ParseException, ParseException {

		ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
		InputStream in = null;
		Reader r = null;
		HttpURLConnection http2 = null;
		int serverResponseCode;
		StringBuilder sb = null;
		InputStreamReader inReader;
		String llToken = null;
		String userName = null;
		String passwordStr = null;
		JSONArray array = new JSONArray('['+clientSecret+']');  
		for(int i=0; i < array.length(); i++)   
		{  
		JSONObject object = array.getJSONObject(i);  
//		log.info(object.getString("username"));  
		userName = object.getString("username");
//		log.info(object.getString("password"));  
		passwordStr= object.getString("password");
		}  
		 String userpass = userName + ":" + passwordStr;  
		 llToken =  new String(Base64.getEncoder().encode(userpass.getBytes()));  
		try {
			JSONArray llJSArr = new JSONArray();
			JSONObject llCredVal = new JSONObject();
			
			Session session = HibernateUtil.beginTransaction();
			List llVehList = session.createSQLQuery(" select distinct v.license_plate,dp.data_provider_name "
					+ " from vehicle v," + " vehicle_mobile_unit vm ," + " mobile_unit m," + " data_provider dp"
					+ " where" + " v.company_id = " + companyID + " and v.record_status = "
					+ CommonConstants.RECORD_STATUS_ACTIVE + " and vm.vehicle_id = v.vehicle_id"
					+ " and vm.is_active = true " + " and vm.last_effective_date >= 'now()' "
					+ " and m.mobileunitid = vm.mobileunitid " + " and dp.data_provider_id = m.data_provider_id"
//					+ " and vm.mobileunitid = 'FLRDR0348'"
					+ " and dp.data_provider_name = 'FLEETRADAR' " + " order by v.license_plate ").list();

			HibernateUtil.commit();
			if (llVehList.size() > 0) {
				for (int llInd = 0; llInd < llVehList.size(); llInd++) {
					Object[] obj = (Object[]) llVehList.get(llInd);
					if ((String) obj[0] != null) {
						llJSArr.put((String) obj[0]);
					}
				}
			}
			
//			llJSArr.put("NL01AB9508");
		
			URL url = new URL(URL);
			URLConnection con = url.openConnection();
			http2 = (HttpURLConnection) con;
			http2.setRequestMethod("POST"); // GET
			http2.setDoOutput(true);
			http2.setRequestProperty("Authorization", "Basic " + llToken);
			http2.setRequestProperty("Content-Type", "application/json");
			OutputStream osw = http2.getOutputStream();
//			osw.write(llCredVal.toString().getBytes());
			osw.write(llJSArr.toString().getBytes());
			osw.flush();
			osw.close();
			http2.connect();
			

			serverResponseCode = http2.getResponseCode();

			switch (serverResponseCode) {
			case HttpsURLConnection.HTTP_OK: {
				sb = new StringBuilder();
				in = http2.getInputStream();
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
				in = http2.getErrorStream();
				inReader = new InputStreamReader(in);
				BufferedReader reader1 = new BufferedReader(inReader);
				String line1;
				while ((line1 = reader1.readLine()) != null) {
					sb.append(line1);
				}
			}

			String result = sb.toString();
//			result = result.replaceAll("\\[", "").replaceAll("\\]","");

			log.info("FleetRadar-RawData" + " Response result : " + result + " Time : " + new Date());

			if (result.startsWith("[") && result.endsWith("]")) {
//				log.info("result"+result);  
				Gson gson = GsonBuilderWithoutTimezone(timeformat);
				FleetRadarDataModel[] dataArray = gson.fromJson(result, FleetRadarDataModel[].class);
				for (FleetRadarDataModel fleetRadardataModel : dataArray) {
					GPSDataModel dataModel = new GPSDataModel();
					if (fleetRadardataModel.getSpeed() > 0) {
						dataModel.setAcc(true);
					} else {
						dataModel.setAcc(false);
					}
					dataModel.setLatitude(fleetRadardataModel.getLatitude());
					dataModel.setLongitude(fleetRadardataModel.getLongitude());
					DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String lDt = fleetRadardataModel.getGpsDateTime();
					if(!lDt.equals("")) {
						String year = lDt.substring(0, 4);
						String month = lDt.substring(4, 6);
						String day = lDt.substring(6, 8);
						String hour = lDt.substring(8,10);
						String minute = lDt.substring(10, 12);
						String sec = lDt.substring(12, 14);
						String tStamp = year+"-"+month+"-"+day+" "+hour+":"+minute+":"+sec;
						Date gpsDate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(tStamp);  
						dataModel.setGpsdate(gpsDate);
					}
				
					boolean llAcc = false;
					String ignitionStatus = fleetRadardataModel.getIgnition();
					if(ignitionStatus.equalsIgnoreCase("ON")) {
						llAcc = true;
					}
					short lSpeed =0;
					lSpeed = fleetRadardataModel.getSpeed();
					dataModel.setAcc(llAcc);
					if (lSpeed > 0) {
						llAcc = true;
						dataModel.setSpeed(lSpeed);
					}else {
						dataModel.setSpeed(lSpeed);
					}
					
					dataModel.setIgnition(ignitionStatus);
					String licensePlate="";
					String licensePlateTemp="";
					if(fleetRadardataModel.getPlateNo().contains("-")) {
						licensePlateTemp = (fleetRadardataModel.getPlateNo()).replaceAll("[\\s\\-()]", "");
						licensePlate = licensePlateTemp.toUpperCase();
						dataModel.setLicensePlate(licensePlate);
					}else {
						licensePlate =  fleetRadardataModel.getPlateNo().toUpperCase();
						dataModel.setLicensePlate(licensePlate);
					}
					 
					int companyId = Integer.valueOf(companyID);
					String mUnitId = "";
					mUnitId = CommonMethods.getVehicleMobileUnitId(companyId, licensePlate);

					if(!mUnitId.equals("")) {
						dataModel.setMobileunitid(mUnitId);
						dataList.add(dataModel);
					}
				}
			}
			

		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(e.getMessage());
			e.printStackTrace();
		}finally {

			if (http2 != null) {
				http2.disconnect();
			}
			in = null;
			inReader = null;

			http2 = null;
		}
		return dataList;

	}
	public static Gson GsonBuilderWithoutTimezone(String timeformat) {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.setDateFormat(timeformat);
		// gsonBuilder.registerTypeAdapter(GPSDataModel.class, new
		// XModelJsonDeserializer());
		Gson gson = gsonBuilder.create();
		return gson;
	}
//	public ArrayList<GPSDataModel> GetHistoryDataFleetRadarFromAPI(String URL, String companyID, HashMap<String, String> columnList,
//			String timeformat,  String tokenURL, String clientID,
//			String clientSecret, String grantType,String dataProviderName,int dataProviderId,
//			String tokenReqParam,MobileUnitTimestamp mobileCompanyTimestamp) throws JSONException, org.json.simple.parser.ParseException, ParseException {
//
//		ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
//		ArrayList<GPSDataModel> finalList = new ArrayList<GPSDataModel>();
//		InputStream in = null;
//		Reader r = null;
//		HttpURLConnection http2 = null;
//		int serverResponseCode;
//		StringBuilder sb = null;
//		InputStreamReader inReader;
//		String llToken = null;
//		String userName = null;
//		String passwordStr = null;
//		JSONArray array = new JSONArray('['+clientSecret+']');  
//		for(int i=0; i < array.length(); i++)   
//		{  
//		JSONObject object = array.getJSONObject(i);  
////		log.info(object.getString("username"));  
//		userName = object.getString("username");
////		log.info(object.getString("password"));  
//		passwordStr= object.getString("password");
//		}  
//		 String userpass = userName + ":" + passwordStr;  
//		 llToken =  new String(Base64.getEncoder().encode(userpass.getBytes()));  
//		try {
//			JSONArray llJSArr = new JSONArray();
//			JSONObject llCredVal = new JSONObject();
//			
//			Session session = HibernateUtil.beginTransaction();
//			List llVehList = session.createSQLQuery(" select distinct v.license_plate,dp.data_provider_name "
//					+ " from vehicle v," + " vehicle_mobile_unit vm ," + " mobile_unit m," + " data_provider dp"
//					+ " where" + " v.company_id = " + companyID + " and v.record_status = "
//					+ CommonConstants.RECORD_STATUS_ACTIVE + " and vm.vehicle_id = v.vehicle_id"
//					+ " and vm.is_active = true " + " and vm.last_effective_date >= 'now()' "
//					+ " and m.mobileunitid = vm.mobileunitid " + " and dp.data_provider_id = m.data_provider_id"
////					+ " and vm.mobileunitid = 'FLRDR0348'"
//					+ " and dp.data_provider_name = 'FLEETRADAR-H' " + " order by v.license_plate ").list();
//
//			HibernateUtil.commit();
//			if (llVehList.size() > 0) {
//				for (int llInd = 0; llInd < llVehList.size(); llInd++) {
//					Object[] obj = (Object[]) llVehList.get(llInd);
//					if ((String) obj[0] != null) {
//						llJSArr.put((String) obj[0]);
//					}
//				}
//			}
////			llJSArr.put("NL01AG6041");
//		
//			URL url = new URL(URL);
//			URLConnection con = url.openConnection();
//			http2 = (HttpURLConnection) con;
//			http2.setRequestMethod("POST"); // GET
//			http2.setDoOutput(true);
//			
//			//Added on 07DEC2023
////			String data = URLEncoder.encode("key", "UTF-8") + "=" + URLEncoder.encode(lEnMovilKeyValue, "UTF-8");
//
////			data += "&" + URLEncoder.encode("vehicles", "UTF-8") + "=" + vehicleIDS;
//			
//			
//			JSONObject parent=new JSONObject();
//			if(!tokenReqParam.equals("")) {
//				parent.put("fromLogId", tokenReqParam);
//			}
//			parent.put("vehicles", llJSArr);
//			
////			String data =  URLEncoder.encode("vehicles", "UTF-8") + ":" + llJSArr;
//
////			byte[] out = data.toString().getBytes(StandardCharsets.UTF_8);
////			int length = out.length;
////			http2.setFixedLengthStreamingMode(length);
//			//Addition ended here 07DEC2023
//			
//			
//			http2.setRequestProperty("Authorization", "Basic " + llToken);
//			http2.setRequestProperty("Content-Type", "application/json");
//			OutputStream osw = http2.getOutputStream();
//
////			osw.write(llJSArr.toString().getBytes());
//			osw.write(parent.toString().getBytes("UTF-8"));
//			osw.flush();
//			osw.close();
//			http2.connect();
//			
//
//			serverResponseCode = http2.getResponseCode();
//
//			switch (serverResponseCode) {
//			case HttpsURLConnection.HTTP_OK: {
//				sb = new StringBuilder();
//				in = http2.getInputStream();
//				inReader = new InputStreamReader(in);
//				BufferedReader reader = new BufferedReader(inReader);
//				String line;
//				while ((line = reader.readLine()) != null) {
//					sb.append(line);
//				}
//				break;
//			}
//			default:
//				sb = new StringBuilder();
//				in = http2.getErrorStream();
//				inReader = new InputStreamReader(in);
//				BufferedReader reader1 = new BufferedReader(inReader);
//				String line1;
//				while ((line1 = reader1.readLine()) != null) {
//					sb.append(line1);
//				}
//			}
//
//			String result = sb.toString();
////			result = result.replaceAll("\\[", "").replaceAll("\\]","");
//
//			log.info("FleetRadar-RawData" + " Response result : " + result + " Time : " + new Date());
//
////			if (result.startsWith("[") && result.endsWith("]")) {
////				log.info("result"+result);  
////				Gson gson = GsonBuilderWithoutTimezone(timeformat);
////				FleetRadarDataModel[] dataArray = gson.fromJson(result, FleetRadarDataModel[].class);
////				for (FleetRadarDataModel fleetRadardataModel : dataArray) {
////					GPSDataModel dataModel = new GPSDataModel();
////					if (fleetRadardataModel.getSpeed() > 0) {
////						dataModel.setAcc(true);
////					} else {
////						dataModel.setAcc(false);
////					}
////					dataModel.setLatitude(fleetRadardataModel.getLatitude());
////					dataModel.setLongitude(fleetRadardataModel.getLongitude());
////					DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
////					String lDt = fleetRadardataModel.getGpsDateTime();
////					if(!lDt.equals("")) {
////						String year = lDt.substring(0, 4);
////						String month = lDt.substring(4, 6);
////						String day = lDt.substring(6, 8);
////						String hour = lDt.substring(8,10);
////						String minute = lDt.substring(10, 12);
////						String sec = lDt.substring(12, 14);
////						String tStamp = year+"-"+month+"-"+day+" "+hour+":"+minute+":"+sec;
////						Date gpsDate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(tStamp);  
////						dataModel.setGpsdate(gpsDate);
////					}
////				
////					boolean llAcc = false;
////					String ignitionStatus = fleetRadardataModel.getIgnition();
////					if(ignitionStatus.equalsIgnoreCase("ON")) {
////						llAcc = true;
////					}
////					short lSpeed =0;
////					lSpeed = fleetRadardataModel.getSpeed();
////					dataModel.setAcc(llAcc);
////					if (lSpeed > 0) {
////						llAcc = true;
////						dataModel.setSpeed(lSpeed);
////					}else {
////						dataModel.setSpeed(lSpeed);
////					}
////					
////					dataModel.setIgnition(ignitionStatus);
////					String licensePlate="";
////					String licensePlateTemp="";
////					if(fleetRadardataModel.getPlateNo().contains("-")) {
////						licensePlateTemp = (fleetRadardataModel.getPlateNo()).replaceAll("[\\s\\-()]", "");
////						licensePlate = licensePlateTemp.toUpperCase();
////						dataModel.setLicensePlate(licensePlate);
////					}else {
////						licensePlate =  fleetRadardataModel.getPlateNo().toUpperCase();
////						dataModel.setLicensePlate(licensePlate);
////					}
////					 
////					int companyId = Integer.valueOf(companyID);
////					String mUnitId = "";
////					mUnitId = CommonMethods.getVehicleMobileUnitId(companyId, licensePlate);
////
////					if(!mUnitId.equals("")) {
////						dataModel.setMobileunitid(mUnitId);
////						dataList.add(dataModel);
////					}
////				}
////			}
//			
//			JSONObject jo = new JSONObject(result);
//			boolean successStatus = false,hasMore = false;
//			
//			String  messageStatus = "",resultDataStr="";
//			String reqLogId = "", lastLogId = "";
//			int reqLimit=0;
//			
//			int dataCount =0;
//			String hasMoreStatus = "";
//			JSONObject resultData =null;
//			JSONArray gpsData =null;
//			
//			
//			successStatus = jo.getBoolean("success");
//			messageStatus = jo.getString("message");
//			resultData = jo.getJSONObject("result");
//
//			
//			if(successStatus == true && messageStatus.equals("OK")&&!resultData.equals("")) {
//
//				
//				reqLogId = resultData.getString("reqLogId");
//				log.info("reqLogId"+reqLogId); 
//				lastLogId = resultData.getString("lastLogId");
//				log.info("lastLogId"+lastLogId); 
//				//Now update the newToken in hashmap and as well as in table
//				mobileCompanyTimestamp.setTokenReqParams(lastLogId);
//				UpdateLastRequestId(lastLogId,dataProviderId,dataProviderName);
//				
//				reqLimit = resultData.getInt("reqLimit");
//				dataCount = resultData.getInt("dataCount");
//				hasMore = resultData.getBoolean("hasMore");
//				log.info("hasMore :"+hasMore+";DataCount:"+dataCount+";Time :"+new Date());  
//				gpsData = resultData.getJSONArray("gpsData");
////				System.out.println("gpsData: "+gpsData);
//
//				for (int i = 0; i < gpsData.length(); i++) {
//					JSONObject frGpsData = gpsData.getJSONObject(i);
//					GPSDataModel dataModel = new GPSDataModel();
//					
//					String plateNo="",location="",transporterName="";
//					String ignition= "";
//					String lDt = "";
//					double latitude;
//					double longitude;
//					
//					short speed = 0;
//					
//					plateNo = frGpsData.getString("plateNo");
//					String licensePlate="";
//					String licensePlateTemp="";
//					if(plateNo.contains("-")) {
//						licensePlateTemp = plateNo.replaceAll("[\\s\\-()]", "");
//						licensePlate = licensePlateTemp.toUpperCase();
//						dataModel.setLicensePlate(licensePlate);
//					}else {
//						licensePlate =  plateNo.toUpperCase();
//						dataModel.setLicensePlate(licensePlate);
//					}
//					location = frGpsData.getString("location");
//					transporterName = frGpsData.getString("transporterName");
//					
//					ignition =  frGpsData.getString("ignition");
//					
//					latitude = frGpsData.getDouble("latitude");
//					dataModel.setLatitude(latitude);
//					
//					longitude = frGpsData.getDouble("longitude");
//					dataModel.setLongitude(longitude);
//					
//					DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//					lDt = frGpsData.getString("gpsDateTime");
//					if(!lDt.equals("")) {
//						String year = lDt.substring(0, 4);
//						String month = lDt.substring(4, 6);
//						String day = lDt.substring(6, 8);
//						String hour = lDt.substring(8,10);
//						String minute = lDt.substring(10, 12);
//						String sec = lDt.substring(12, 14);
//						String tStamp = year+"-"+month+"-"+day+" "+hour+":"+minute+":"+sec;
//						Date gpsDate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(tStamp);  
//						dataModel.setGpsdate(gpsDate);
//					}
//					boolean llAcc = false;
//					if(ignition.equalsIgnoreCase("ON")) {
//						llAcc = true;
//					}else {
//						llAcc = false; 
//					}
//					speed = (short)frGpsData.getInt("speed");
//					dataModel.setAcc(llAcc);
//					if (speed > 0) {
//						llAcc = true;
//						dataModel.setSpeed(speed);
//					}else {
//						dataModel.setSpeed(speed);
//					}
//					
//					int companyId = Integer.valueOf(companyID);
//					String mUnitId = "";
//					mUnitId = CommonMethods.getVehicleMobileUnitId(companyId, licensePlate);
//
//					if(!mUnitId.equals("")) {
//						dataModel.setMobileunitid(mUnitId);
//						
//						
//						dataList.add(dataModel);
//					}
//				}
//			}
//			
//			ArrayList<GPSDataModel> dataCopyList = new ArrayList<GPSDataModel>();
//			if(dataList!=null) {
//				dataCopyList = dataList;
//				for(int lCount=0;lCount < dataList.size();lCount++) {
//					GPSDataModel lGpsModel = new GPSDataModel();
//					lGpsModel = dataList.get(lCount);
//					boolean dupFound = false;
//					int count = 0;
//					for(int mCount=0;mCount < dataCopyList.size();mCount++) {
//						
//						if(mCount == 0) {
//							boolean dupData = false;
//							if(finalList!=null && finalList.size()>0) {
//								for(int nCount=0;nCount < finalList.size();nCount++ ) {
//									GPSDataModel nGpsModel = new GPSDataModel();
//									nGpsModel = finalList.get(nCount);
//									if(lGpsModel.getLicensePlate().equalsIgnoreCase(nGpsModel.getLicensePlate())) {
//										if((lGpsModel.getLatitude() == nGpsModel.getLatitude())&& (lGpsModel.getLongitude() ==nGpsModel.getLongitude() ) ) {
//											dupData = true;
//										}
//									}
//								}
//								if(!dupData) {
//									finalList.add(lGpsModel);
////									System.out.println("finalList.size():"+(finalList.size()));
//								}
//							}else {
//								finalList.add(lGpsModel);
////								System.out.println("finalList.size():"+(finalList.size()));
//							}
////							System.out.println("finalList.size():"+(finalList.size()));
//						}
//						GPSDataModel mGpsModel = new GPSDataModel();
//						mGpsModel = dataCopyList.get(mCount);
//						if(lGpsModel.getLicensePlate().equalsIgnoreCase(mGpsModel.getLicensePlate())) {
//							if((lGpsModel.getLatitude() == mGpsModel.getLatitude())&& (lGpsModel.getLongitude() ==mGpsModel.getLongitude() ) ) {
//								count++;
//								if(count>1) {
//									dupFound =  true;
////									System.out.println("lGpsModel.getLicensePlate():"+lGpsModel.getLicensePlate()+";lGpsModel.getLatitude():"+lGpsModel.getLatitude()+";lGpsModel.getLongitude():"+lGpsModel.getLongitude());
////									System.out.println("mGpsModel.getLicensePlate():"+mGpsModel.getLicensePlate()+";mGpsModel.getLatitude():"+mGpsModel.getLatitude()+";mGpsModel.getLongitude():"+mGpsModel.getLongitude());
//									break;
//								}
//							}
//						}
//					}
//				}
//			}
//			
//				
//				
//				
////			finalList = new ArrayList<GPSDataModel>();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			log.error(e.getMessage());
//		}finally {
//
//			if (http2 != null) {
//				http2.disconnect();
//			}
//			in = null;
//			inReader = null;
//
//			http2 = null;
//		}
////		return dataList;
//		return finalList;
//
//
//	}
	
	public static void UpdateLastRequestId(String newToken,int dataProviderId,String dataProviderName ) {
		try {
			Session session = HibernateUtil.beginTransaction();
			ArrayList<DataProvider> lDataProviderList = HibernateUtil.castList(DataProvider.class,
					session.createQuery(" from DataProvider	" + " where id.dataProviderId = '" + dataProviderId + "'"
							+ " and id.dataProviderName = '" + dataProviderName + "' and recordStatus = "
							+ CommonConstants.RECORD_STATUS_ACTIVE).list());
			if (lDataProviderList.size() > 0) {
				DataProvider lDataProvider = lDataProviderList.get(0);
				lDataProvider.setTokenReqParams(newToken);
				session.update(lDataProvider);
				log.info("Inside UpdateLastRequestId"+newToken); 
				HibernateUtil.commit();
			}

		} catch (Throwable e) {
			e.printStackTrace();
			log.error(e.getMessage());
			HibernateUtil.rollback();
		}
	}
	
	//Added on 07OCT2025
	public ArrayList<GPSDataModel> GetHistoryDataFleetRadarFromAPI(String URL, String companyID, HashMap<String, String> columnList,
	String timeformat,  String tokenURL, String clientID,
	String clientSecret, String grantType,String dataProviderName,int dataProviderId,
	String tokenReqParam,MobileUnitTimestamp mobileCompanyTimestamp) throws JSONException, org.json.simple.parser.ParseException, ParseException {

ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
ArrayList<GPSDataModel> finalList = new ArrayList<GPSDataModel>();
InputStream in = null;
Reader r = null;
HttpURLConnection http2 = null;
int serverResponseCode;
StringBuilder sb = null;
InputStreamReader inReader;
String llToken = null;
String userName = null;
String passwordStr = null;
JSONArray array = new JSONArray('['+clientSecret+']');  
for(int i=0; i < array.length(); i++)   
{  
JSONObject object = array.getJSONObject(i);  
//log.info(object.getString("username"));  
userName = object.getString("username");
//log.info(object.getString("password"));  
passwordStr= object.getString("password");
}  
 String userpass = userName + ":" + passwordStr;  
 llToken =  new String(Base64.getEncoder().encode(userpass.getBytes()));  
try {
	
	boolean hasMore = true;
	String tokenParam = tokenReqParam;  // initial fromLogId (if available)

	while (hasMore) {
	    JSONArray llJSArr = new JSONArray();
	    JSONObject llCredVal = new JSONObject();

	    // Recreate vehicle list
	    Session session = HibernateUtil.beginTransaction();
	    List llVehList = session.createSQLQuery(" select distinct v.license_plate,dp.data_provider_name "
	            + " from vehicle v," + " vehicle_mobile_unit vm ," + " mobile_unit m," + " data_provider dp"
	            + " where" + " v.company_id = " + companyID + " and v.record_status = "
	            + CommonConstants.RECORD_STATUS_ACTIVE + " and vm.vehicle_id = v.vehicle_id"
	            + " and vm.is_active = true " + " and vm.last_effective_date >= 'now()' "
	            + " and m.mobileunitid = vm.mobileunitid " + " and dp.data_provider_id = m.data_provider_id"
	            + " and dp.data_provider_name = 'FLEETRADAR-H' " + " order by v.license_plate ").list();
	    HibernateUtil.commit();

	    if (llVehList.size() > 0) {
	        for (int llInd = 0; llInd < llVehList.size(); llInd++) {
	            Object[] obj = (Object[]) llVehList.get(llInd);
	            if ((String) obj[0] != null) {
	                llJSArr.put((String) obj[0]);
	            }
	        }
	    }

	    // Prepare API request
	    URL url = new URL(URL);
	    http2 = (HttpURLConnection) url.openConnection();
	    http2.setRequestMethod("POST");
	    http2.setDoOutput(true);
	    http2.setRequestProperty("Authorization", "Basic " + llToken);
	    http2.setRequestProperty("Content-Type", "application/json");

	    JSONObject parent = new JSONObject();
	    if (tokenParam != null && !tokenParam.isEmpty()) {
	        parent.put("fromLogId", tokenParam);
	    }
	    parent.put("vehicles", llJSArr);

	    OutputStream osw = http2.getOutputStream();
	    osw.write(parent.toString().getBytes("UTF-8"));
	    osw.flush();
	    osw.close();

	    serverResponseCode = http2.getResponseCode();
	    sb = new StringBuilder();

	    BufferedReader reader;

	    if (serverResponseCode == HttpsURLConnection.HTTP_OK) {
	        in = http2.getInputStream();
	    } else {
	        in = http2.getErrorStream();
	    }

	    inReader = new InputStreamReader(in);
	    reader = new BufferedReader(inReader);
	    String line;
	    while ((line = reader.readLine()) != null) {
	        sb.append(line);
	    }
	    reader.close();

	    String result = sb.toString();
	    JSONObject jo = new JSONObject(result);

	    boolean successStatus = jo.getBoolean("success");
	    if (successStatus && jo.has("result")) {
	        JSONObject resultData = jo.getJSONObject("result");

	        // Read pagination info
	        hasMore = resultData.getBoolean("hasMore");
	        String lastLogId = resultData.getString("lastLogId");
	        tokenParam = lastLogId; // for next iteration

	        // Save token to DB and object
	        mobileCompanyTimestamp.setTokenReqParams(lastLogId);
	        UpdateLastRequestId(lastLogId, dataProviderId, dataProviderName);

	        // Extract gpsData and process it
	        JSONArray gpsData = resultData.getJSONArray("gpsData");
	        for (int i = 0; i < gpsData.length(); i++) {
	            JSONObject frGpsData = gpsData.getJSONObject(i);
	            GPSDataModel dataModel = new GPSDataModel();

	            // Your existing logic for parsing GPS data...
	            // ...
	            // dataList.add(dataModel);
				String plateNo="",location="",transporterName="";
				String ignition= "";
				String lDt = "";
				double latitude;
				double longitude;
				
				short speed = 0;
				
				plateNo = frGpsData.getString("plateNo");
				String licensePlate="";
				String licensePlateTemp="";
				if(plateNo.contains("-")) {
					licensePlateTemp = plateNo.replaceAll("[\\s\\-()]", "");
					licensePlate = licensePlateTemp.toUpperCase();
					dataModel.setLicensePlate(licensePlate);
				}else {
					licensePlate =  plateNo.toUpperCase();
					dataModel.setLicensePlate(licensePlate);
				}
				location = frGpsData.getString("location");
				transporterName = frGpsData.getString("transporterName");
				
				ignition =  frGpsData.getString("ignition");
				
				latitude = frGpsData.getDouble("latitude");
				dataModel.setLatitude(latitude);
				
				longitude = frGpsData.getDouble("longitude");
				dataModel.setLongitude(longitude);
				
				DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				lDt = frGpsData.getString("gpsDateTime");
				if(!lDt.equals("")) {
					String year = lDt.substring(0, 4);
					String month = lDt.substring(4, 6);
					String day = lDt.substring(6, 8);
					String hour = lDt.substring(8,10);
					String minute = lDt.substring(10, 12);
					String sec = lDt.substring(12, 14);
					String tStamp = year+"-"+month+"-"+day+" "+hour+":"+minute+":"+sec;
					Date gpsDate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(tStamp);  
					dataModel.setGpsdate(gpsDate);
				}
				boolean llAcc = false;
				if(ignition.equalsIgnoreCase("ON")) {
					llAcc = true;
				}else {
					llAcc = false; 
				}
				speed = (short)frGpsData.getInt("speed");
				dataModel.setAcc(llAcc);
				if (speed > 0) {
					llAcc = true;
					dataModel.setSpeed(speed);
				}else {
					dataModel.setSpeed(speed);
				}
				
				int companyId = Integer.valueOf(companyID);
				String mUnitId = "";
				mUnitId = CommonMethods.getVehicleMobileUnitId(companyId, licensePlate);

				if(!mUnitId.equals("")) {
					dataModel.setMobileunitid(mUnitId);
					
					
					dataList.add(dataModel);
				}
	        }

	        log.info("Fetched batch with lastLogId=" + lastLogId + ", hasMore=" + hasMore);
	    } else {
	        hasMore = false; // stop if request fails
	    }

	    // Disconnect to prevent leaks
	    http2.disconnect();

	    // Optional: sleep between requests (avoid rate limits)
	    if (hasMore) {
	        Thread.sleep(2000); // 2-second delay before next call
	    }
	}

	ArrayList<GPSDataModel> dataCopyList = new ArrayList<GPSDataModel>();
	if(dataList!=null) {
		dataCopyList = dataList;
		for(int lCount=0;lCount < dataList.size();lCount++) {
			GPSDataModel lGpsModel = new GPSDataModel();
			lGpsModel = dataList.get(lCount);
			boolean dupFound = false;
			int count = 0;
			for(int mCount=0;mCount < dataCopyList.size();mCount++) {
				
				if(mCount == 0) {
					boolean dupData = false;
					if(finalList!=null && finalList.size()>0) {
						for(int nCount=0;nCount < finalList.size();nCount++ ) {
							GPSDataModel nGpsModel = new GPSDataModel();
							nGpsModel = finalList.get(nCount);
							if(lGpsModel.getLicensePlate().equalsIgnoreCase(nGpsModel.getLicensePlate())) {
								if((lGpsModel.getLatitude() == nGpsModel.getLatitude())&& (lGpsModel.getLongitude() ==nGpsModel.getLongitude() ) ) {
									dupData = true;
								}
							}
						}
						if(!dupData) {
							finalList.add(lGpsModel);
//							System.out.println("finalList.size():"+(finalList.size()));
						}
					}else {
						finalList.add(lGpsModel);
//						System.out.println("finalList.size():"+(finalList.size()));
					}
//					System.out.println("finalList.size():"+(finalList.size()));
				}
				GPSDataModel mGpsModel = new GPSDataModel();
				mGpsModel = dataCopyList.get(mCount);
				if(lGpsModel.getLicensePlate().equalsIgnoreCase(mGpsModel.getLicensePlate())) {
					if((lGpsModel.getLatitude() == mGpsModel.getLatitude())&& (lGpsModel.getLongitude() ==mGpsModel.getLongitude() ) ) {
						count++;
						if(count>1) {
							dupFound =  true;
//							System.out.println("lGpsModel.getLicensePlate():"+lGpsModel.getLicensePlate()+";lGpsModel.getLatitude():"+lGpsModel.getLatitude()+";lGpsModel.getLongitude():"+lGpsModel.getLongitude());
//							System.out.println("mGpsModel.getLicensePlate():"+mGpsModel.getLicensePlate()+";mGpsModel.getLatitude():"+mGpsModel.getLatitude()+";mGpsModel.getLongitude():"+mGpsModel.getLongitude());
							break;
						}
					}
				}
			}
		}
	}
	
		
		
		
//	finalList = new ArrayList<GPSDataModel>();
} catch (IOException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
	log.error(e.getMessage());
} catch (InterruptedException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
}finally {

	if (http2 != null) {
		http2.disconnect();
	}
	in = null;
	inReader = null;

	http2 = null;
}
//return dataList;
return finalList;


}
}
