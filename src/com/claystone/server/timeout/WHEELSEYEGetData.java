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
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class WHEELSEYEGetData {

	private Logger log;

	public WHEELSEYEGetData() {
		log = Logger.getLogger(WHEELSEYEGetData.class);

	}

//	public ArrayList<GPSDataModel> GetDataFromWHEELSEYE(String URL, String companyID,
//			HashMap<String, String> columnList, String timeformat, String dataProviderName) {
//		ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
//		InputStream in = null;
//		Reader r = null;
//		HttpURLConnection http = null;
//		int serverResponseCode;
//		StringBuilder sb = null;
//		InputStreamReader inReader;
//
//		try {
//
//			URL url = new URL(URL);
//			URLConnection con = url.openConnection();
//			http = (HttpURLConnection) con;
//			http.setRequestMethod("GET"); // POST
//			http.setDoOutput(true);
//			http.setRequestProperty("Content-Type", "application/json");
//			http.connect();
//
//			serverResponseCode = http.getResponseCode();
//
//			switch (serverResponseCode) {
//			case HttpsURLConnection.HTTP_OK: {
//				sb = new StringBuilder();
//				in = http.getInputStream();
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
//				in = http.getErrorStream();
//				inReader = new InputStreamReader(in);
//				BufferedReader reader1 = new BufferedReader(inReader);
//				String line1;
//				while ((line1 = reader1.readLine()) != null) {
//					sb.append(line1);
//				}
//			}
//
//			String data = sb.toString();
//			JSONObject jo = new JSONObject(data);
//			log.info("Inside WHEELSEYE get data ");
//			if(jo.has("data"))
//			{
//			JSONObject llPayLd = jo.getJSONObject("data");
//			JSONArray details = llPayLd.getJSONArray("list");
//
//			for (int i = 0; i < details.length(); i++) {
//				JSONObject llData = details.getJSONObject(i);
//				boolean llAcc = false;
//				String llVehicleLicensePlate = llData.getString("vehicleNumber");
//				Double llLat = llData.getDouble("latitude");
//				Double llLon = llData.getDouble("longitude");
//				long llTime = llData.getLong("createdDate");
//
//				Date lGpDate = new Date(TimeUnit.SECONDS.toMillis(llTime));
//				// TimeZone tz = TimeZone.getTimeZone("UTC");
//				DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//				// formatstr.setTimeZone(tz); // strip timezone
//				String llGpDt = formatstr.format(lGpDate);
//				Date llGpSt = formatstr.parse(llGpDt);
//				int llSpeed = llData.getInt("speed");
//				llAcc = llData.getBoolean("ignition");
//				if (llSpeed > 0) {
//					llAcc = true;
//				}
//				int llDirection = llData.getInt("angle");
//				GPSDataModel model = new GPSDataModel();
//				model.setLicensePlate(llVehicleLicensePlate);
//				model.setLatitude(llLat);
//				model.setLongitude(llLon);
//				model.setGpsdate(llGpSt);
//				model.setSpeed((short) llSpeed);
//				model.setAcc(llAcc);
//				model.setDirection((short) llDirection);
//				dataList.add(model);
//
//			}
//			}
//			else {
//				log.info("data field not available in  WHEELSEYE : " +data);
//			}
//			log.error(companyID + " dataList WHEELSEYE : " + dataList.size() + " Time : " + new Date());
//
//		} catch (Exception e) {log.error(e.getMessage());
//			e.printStackTrace();
//		}
//		return dataList;
//
//	}

	//Added on 30AUG2024--Api with pagination
	public ArrayList<GPSDataModel> GetDataFromWHEELSEYE(String URL, String companyID,
			HashMap<String, String> columnList, String timeformat, String dataProviderName) {
		ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
		InputStream in = null;
		Reader r = null;
		HttpURLConnection http = null;
		int serverResponseCode;
		StringBuilder sb = null;
		StringBuilder sb2 = null;
		InputStreamReader inReader;

		try {
			
			URL url = new URL(URL);
			//Added on 30AUG2024--From WHEELSEYE
			//Api performance can drastically improve if it is set to false,
			//as we use third
			//party api call to fetch location address which is a heavy call
			URL+="&isLocationReq=false";
			String baseUrl =URL.toString();
			
			URLConnection con = url.openConnection();
			http = (HttpURLConnection) con;
			http.setRequestMethod("GET"); // POST
			http.setDoOutput(true);
			http.setRequestProperty("Content-Type", "application/json");
			http.connect();

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

			String data = sb.toString();
			JSONObject jo = new JSONObject(data);
			log.info("Inside WHEELSEYE get data ");
			if(jo.has("data"))
			{
			JSONObject llPayLd = jo.getJSONObject("data");
			JSONArray details = llPayLd.getJSONArray("list");
			
			Integer totalPages = llPayLd.getInt("totalPages");
			if(totalPages > 1) {
				for(int pageCount=1;pageCount < totalPages;pageCount++) {
//					URL+="&isLocationReq=false&pageNo="+pageCount+"&size=100";
					URL = baseUrl;
					URL+="&pageNo="+pageCount+"&size=100";
//					System.out.println("URL:-> "+URL);
					url = new URL(URL);
//					String urlOriginal = url.getFile();
//					urlOriginal+="&isLocationReq=false&pageNo="+pageCount+"&size=100";
//					System.out.println("file: "+url.getFile());
					
//					url+="&isLocationReq=false&pageNo="+pageCount+"&size=100";
					con = url.openConnection();
					http = (HttpURLConnection) con;
					http.setRequestMethod("GET"); // POST
					http.setDoOutput(true);
					http.setRequestProperty("Content-Type", "application/json");
					http.connect();

					serverResponseCode = http.getResponseCode();

					switch (serverResponseCode) {
					case HttpsURLConnection.HTTP_OK: {
						sb2 = new StringBuilder();
						in = http.getInputStream();
						inReader = new InputStreamReader(in);
						BufferedReader reader = new BufferedReader(inReader);
						String line;
						while ((line = reader.readLine()) != null) {
							sb2.append(line);
						}
						break;
					}
					default:
						sb2 = new StringBuilder();
						in = http.getErrorStream();
						inReader = new InputStreamReader(in);
						BufferedReader reader1 = new BufferedReader(inReader);
						String line1;
						while ((line1 = reader1.readLine()) != null) {
							sb2.append(line1);
						}
					}

					String data2 = sb2.toString();
					JSONObject jo2 = new JSONObject(data2);
					if(jo2.has("data"))
					{
						JSONObject llPayLd2 = jo2.getJSONObject("data");
						JSONArray details2 = llPayLd2.getJSONArray("list");
						for(int dCount=0;dCount < details2.length();dCount++) {
							JSONObject dData = details2.getJSONObject(dCount);
							details.put(dData);
						}
						
					}
				}
			}
			for (int i = 0; i < details.length(); i++) {
				JSONObject llData = details.getJSONObject(i);
				boolean llAcc = false;
				String llVehicleLicensePlate = llData.getString("vehicleNumber");
				Double llLat = llData.getDouble("latitude");
				Double llLon = llData.getDouble("longitude");
				long llTime = llData.getLong("createdDate");
				
//				System.out.println("LicensePlate:-> "+llVehicleLicensePlate);
//				if(llVehicleLicensePlate.equalsIgnoreCase("HR55AW2522")) {
//					System.out.println("I'm here");
//				}

				Date lGpDate = new Date(TimeUnit.SECONDS.toMillis(llTime));
				// TimeZone tz = TimeZone.getTimeZone("UTC");
				DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				// formatstr.setTimeZone(tz); // strip timezone
				String llGpDt = formatstr.format(lGpDate);
				Date llGpSt = formatstr.parse(llGpDt);
				int llSpeed = llData.getInt("speed");
				llAcc = llData.getBoolean("ignition");
				if (llSpeed > 0) {
					llAcc = true;
				}
				int llDirection = llData.getInt("angle");
				GPSDataModel model = new GPSDataModel();
				model.setLicensePlate(llVehicleLicensePlate);
				model.setLatitude(llLat);
				model.setLongitude(llLon);
				model.setGpsdate(llGpSt);
				model.setSpeed((short) llSpeed);
				model.setAcc(llAcc);
				model.setDirection((short) llDirection);
				dataList.add(model);

			}
			}
			else {
				log.info("data field not available in  WHEELSEYE : " +data);
			}
			log.error(companyID + " dataList WHEELSEYE : " + dataList.size() + " Time : " + new Date());

		} catch (Exception e) {log.error(e.getMessage());
			e.printStackTrace();
		}
		return dataList;

	}
	
}
