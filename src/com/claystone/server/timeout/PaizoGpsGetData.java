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

public class PaizoGpsGetData {
	private Logger log;

	public PaizoGpsGetData() {
		log = Logger.getLogger(PaizoGpsGetData.class);
	}
	public ArrayList<GPSDataModel> GetDataPaizoGpsFromAPI(String URL, String companyID, HashMap<String, String> columnList,
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
//		JSONArray array = new JSONArray('['+clientSecret+']');  
//		for(int i=0; i < array.length(); i++)   
//		{  
//		JSONObject object = array.getJSONObject(i);  
//
//		userName = object.getString("cmd");
//
//		passwordStr= object.getString("key");
//		}  
//		 String userpass = userName + ":" + passwordStr;  
//		 llToken =  new String(Base64.getEncoder().encode(userpass.getBytes()));  
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
					+ " and dp.data_provider_name = 'PAIZOGPS' " + " order by v.license_plate ").list();

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
//			URL = "https://paizogps.in/api/LiveData.php?cmd=Get_Gps&key=Paizo@Aditya1123";
			URL url = new URL(URL);
			URLConnection con = url.openConnection();
			http2 = (HttpURLConnection) con;
			http2.setRequestMethod("POST"); // GET
			http2.setDoOutput(true);
//			http2.setRequestProperty("Authorization", "Basic " + llToken);
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

			log.info("PaizoGps-RawData" + " Response result : " + result + " Time : " + new Date());
			
			
			

//			if (result.startsWith("[") && result.endsWith("]")) {
////				log.info("result"+result);  
//				Gson gson = GsonBuilderWithoutTimezone(timeformat);
//				PaizoGpsDataModel[] dataArray = gson.fromJson(result, PaizoGpsDataModel[].class);
//				for (PaizoGpsDataModel paizoGpsdataModel : dataArray) {
//					GPSDataModel dataModel = new GPSDataModel();
//					if (paizoGpsdataModel.getSpeed() > 0) {
//						dataModel.setAcc(true);
//					} else {
//						dataModel.setAcc(false);
//					}
//					dataModel.setLatitude(paizoGpsdataModel.getLat());
//					dataModel.setLongitude(paizoGpsdataModel.getLng());
//					DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//					String lDt = paizoGpsdataModel.getTime();
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
//				
//					boolean llAcc = false;
//					String ignitionStatus = paizoGpsdataModel.getIgnition();
//					if(ignitionStatus.equalsIgnoreCase("ON")) {
//						llAcc = true;
//					}
//					short lSpeed =0;
//					lSpeed = paizoGpsdataModel.getSpeed();
//					dataModel.setAcc(llAcc);
//					if (lSpeed > 0) {
//						llAcc = true;
//						dataModel.setSpeed(lSpeed);
//					}else {
//						dataModel.setSpeed(lSpeed);
//					}
//					
//					dataModel.setIgnition(ignitionStatus);
//					String licensePlate="";
//					String licensePlateTemp="";
//					if(paizoGpsdataModel.getRegNo().contains("-")) {
//						licensePlateTemp = (paizoGpsdataModel.getRegNo()).replaceAll("[\\s\\-()]", "");
//						licensePlate = licensePlateTemp.toUpperCase();
//						dataModel.setLicensePlate(licensePlate);
//					}else {
//						licensePlate =  paizoGpsdataModel.getRegNo().toUpperCase();
//						dataModel.setLicensePlate(licensePlate);
//					}
//					 
//					int companyId = Integer.valueOf(companyID);
//					String mUnitId = "";
//					mUnitId = CommonMethods.getVehicleMobileUnitId(companyId, licensePlate);
//
//					if(!mUnitId.equals("")) {
//						dataModel.setMobileunitid(mUnitId);
//						dataList.add(dataModel);
//					}
//				}
//			}
			
			JSONObject jo = new JSONObject(result);
			String  typeStatus = "",dataStr="";
			JSONArray gpsData =null;

			JSONArray data;
			
			
			typeStatus = jo.getString("type");
			if(typeStatus.equals("Success")) {
				gpsData = jo.getJSONArray("data");
				for (int i = 0; i < gpsData.length(); i++) {
					JSONObject paizoGpsData = gpsData.getJSONObject(i);
					GPSDataModel dataModel = new GPSDataModel();
					String plateNo="";
					String ignition= "";
					String lDt = "";
					double latitude;
					double longitude;
					
					short speed = 0;
					int Odometer=0;
					
					plateNo = paizoGpsData.getString("RegNo").trim();
					if(plateNo.contains(" ")) {
						plateNo = plateNo.replaceAll(" ", "");
					}
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
					boolean isFound = false;
					for(int lCount = 0;lCount < llVehList.size();lCount++) {
						Object[] obj = (Object[]) llVehList.get(lCount);
						if ((String) obj[0] != null) {
							if(obj[0].equals(licensePlate)) {
								isFound = true;
								break;
							}
						}
					}
					if(!isFound) break;

					
					latitude = paizoGpsData.getDouble("Lat");
					dataModel.setLatitude(latitude);
					
					longitude = paizoGpsData.getDouble("Lng");
					dataModel.setLongitude(longitude);
					
					
					
					DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					lDt = paizoGpsData.getString("Time");
					if(!lDt.equals("")) {
//						String year = lDt.substring(0, 4);
//						String month = lDt.substring(4, 6);
//						String day = lDt.substring(6, 8);
						
						String day = lDt.substring(0, 2);
						String month = lDt.substring(3, 5);
						String year = lDt.substring(6, 10);
						
						String hour = lDt.substring(11,13);
						String minute = lDt.substring(14, 16);
						String sec = lDt.substring(17, 19);
						String tStamp = year+"-"+month+"-"+day+" "+hour+":"+minute+":"+sec;
						Date gpsDate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(tStamp);  
						dataModel.setGpsdate(gpsDate);
					}
					boolean llAcc = false;
					ignition =  paizoGpsData.getString("Ignition");
					if(ignition.equalsIgnoreCase("0")) {
						llAcc = false;
					}else {
						llAcc = true; 
					}
					speed = (short)paizoGpsData.getInt("Speed");
					dataModel.setAcc(llAcc);
					if (speed > 0) {
						llAcc = true;
						dataModel.setSpeed(speed);
					}else {
						dataModel.setSpeed(speed);
					}
					Odometer = (int)paizoGpsData.getDouble("Odometer");
					dataModel.setMileage(Odometer);
					
					
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
			e.printStackTrace();
			log.error(e.getMessage());
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

}
