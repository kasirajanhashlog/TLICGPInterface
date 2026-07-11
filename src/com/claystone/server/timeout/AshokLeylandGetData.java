package com.claystone.server.timeout;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.claystone.common.utils.CommonMethods;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class AshokLeylandGetData {

	private Logger log;

	public AshokLeylandGetData() {
		log = Logger.getLogger(AshokLeylandGetData.class);

	}

	public ArrayList<GPSDataModel> GetDataFromAshokLeyland(String URL, String companyID,
			HashMap<String, String> columnList, String timeformat, String dataProviderName) {
		ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
		InputStream in = null;
		Reader r = null;
		HttpURLConnection http = null;
		int serverResponseCode;
		StringBuilder sb = null;
		InputStreamReader inReader;

		try {

			URL url = new URL(URL);
			URLConnection con = url.openConnection();
			http = (HttpURLConnection) con;
			http.setRequestMethod("GET"); // POST
			http.setDoOutput(false);
			http.setRequestProperty("Content-Type", "application/json; charset=utf-8");
//			http.setRequestProperty("Content-Type", "application/json");
			http.setRequestProperty("Accept", "application/json");
	        http.setRequestProperty(
	                "User-Agent",
	                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
	              + "(KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36"
	            );
			http.connect();
//			log.error("I'm here-1");
			serverResponseCode = http.getResponseCode();
//			log.error("I'm here-2");
//			log.error("serverResponseCode: "+serverResponseCode);
//			log.error("I'm here-3");
			switch (serverResponseCode) {
		
			case HttpsURLConnection.HTTP_OK: {
//				log.error("I'm here-4");
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
//				log.error("I'm here-5");
				sb = new StringBuilder();
				in = http.getErrorStream();
				inReader = new InputStreamReader(in);
				BufferedReader reader1 = new BufferedReader(inReader);
				String line1;
				while ((line1 = reader1.readLine()) != null) {
					sb.append(line1);
				}
			}
//			log.error("I'm here-6");
			String result = sb.toString();
//			log.error("I'm here-7");
			log.error("AsokLeyland Res: "+result);
			for (Map.Entry<String, String> entry : columnList.entrySet()) {
				result = result.replace(entry.getValue(), entry.getKey());
			}
			if (result.startsWith("[") && result.endsWith("]")) {
//				log.error("I'm here-8");
				Gson gson = GsonBuilderWithoutTimezone(timeformat);
//				log.error("I'm here-9");
				AshokLeylandDataModel[] dataArray = gson.fromJson(result, AshokLeylandDataModel[].class);
//				log.error("I'm here-10");
				for (AshokLeylandDataModel ashokLeylandDataModel : dataArray) {
//					log.error("I'm here-11");
					GPSDataModel dataModel = new GPSDataModel();
					// Added on 2020-OCT-15 Ignition remains false/OFF even when speed is > 0 so
					// checking speed for all devices
//					log.error("ashokLeylandDataModel-getVehicleregnumber: "+ashokLeylandDataModel.getVehicleregnumber());
//					log.error("ashokLeylandDataModel-getSpeed: "+ashokLeylandDataModel.getSpeed());
//					log.error("ashokLeylandDataModel-getSpeed: "+Math.round(ashokLeylandDataModel.getSpeed()));

//					log.error("I'm here-12");
					if(ashokLeylandDataModel.getSpeed()> 0) {
//						int speed = Integer.valueOf(ashokLeylandDataModel.getSpeed());
//						int speed = Integer.parseInt(ashokLeylandDataModel.getSpeed());  
//						log.error("ashokLeylandDataModel-speed: "+speed);
						int speed = Math.round(ashokLeylandDataModel.getSpeed());
						if (speed > 0) {
							dataModel.setAcc(true);
							dataModel.setSpeed((short)speed);
						} else {
							dataModel.setAcc(false);
							dataModel.setSpeed((short)0);
						}
					}

					if(ashokLeylandDataModel.getIgnition()>0) {
						
					}
					dataModel.setGpsstatus(true);
					
					dataModel.setDirection((short)ashokLeylandDataModel.getHeading());
//					log.error("Lat: "+ashokLeylandDataModel.getLatitude());
//					log.error("Lon: "+ashokLeylandDataModel.getLongitude());
					
					double lat6 = truncate(ashokLeylandDataModel.getLatitude(),6);
					double lon6 = truncate(ashokLeylandDataModel.getLongitude(),6);
				    
//					log.error("lat6: "+lat6);
//					log.error("lon6: "+lon6);
					
					dataModel.setLatitude(lat6);
					dataModel.setLongitude(lon6);
					

					
					
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					Date locationDate = sdf.parse(ashokLeylandDataModel.getDatetime());
					dataModel.setGpsdate(locationDate);
					
					String licensePlate="";
					String licensePlateTemp="";
					if(ashokLeylandDataModel.getVehicleregnumber().contains("-")) {
						licensePlateTemp = (ashokLeylandDataModel.getVehicleregnumber()).replaceAll("[\\s\\-()]", "");
						licensePlate = licensePlateTemp.toUpperCase();
						dataModel.setLicensePlate(licensePlate);
					}else {
						licensePlate =  ashokLeylandDataModel.getVehicleregnumber().toUpperCase();
						dataModel.setLicensePlate(licensePlate);
					}
					if(ashokLeylandDataModel.getOdometer()>0) {
						dataModel.setMileage((int)ashokLeylandDataModel.getOdometer());
					}
//					 log.error("ashokLeylandDataModel.getBatlevel(): "+ashokLeylandDataModel.getBatlevel());
//					 log.error("I'm here-13");
					if(ashokLeylandDataModel.getBatlevel()>0) {
//						 log.error("I'm here-14");
						dataModel.setBatt((short)ashokLeylandDataModel.getBatlevel());
//						log.error("(short)ashokLeylandDataModel.getBatlevel(): "+(short)ashokLeylandDataModel.getBatlevel());
					}else {
//						 log.error("I'm here-15");
						dataModel.setBatt((short)0);
					}
					int companyId = Integer.valueOf(companyID);
					String mUnitId = "";
					log.error("AsokLeyland Api VehicleLicensePlate:"+licensePlate);
					mUnitId = CommonMethods.getVehicleMobileUnitId(companyId, licensePlate);
//					log.info("LicensePlate in  GLOBALNISSAN : " +licensePlate);
					if(!mUnitId.equals("")) {
						dataModel.setMobileunitid(mUnitId);
						dataList.add(dataModel);
						log.info(companyID + " dataList AsokLeyland : " + dataList.size() + " Time : " + new Date());
					}else {
						log.info("data field not available in  AsokLeyland : " +dataList);
					}

				}
			}
			
			
			
			

			

		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
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
	public double truncate(double number, int precision)
	{
	    double prec = Math.pow(10, precision);
	    int integerPart = (int) number;
	    double fractionalPart = number - integerPart;
	    fractionalPart *= prec;
	    int fractPart = (int) fractionalPart;
	    fractionalPart = (double) (integerPart) + (double) (fractPart)/prec;
	    return fractionalPart;
	}
}
