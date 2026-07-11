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

public class GLOBALNISSANGetData {

	private Logger log;

	public GLOBALNISSANGetData() {
		log = Logger.getLogger(GLOBALNISSANGetData.class);

	}

	public ArrayList<GPSDataModel> GetDataFromGLOBALNISSAN(String URL, String companyID,
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

			String result = sb.toString();
			for (Map.Entry<String, String> entry : columnList.entrySet()) {
				result = result.replace(entry.getValue(), entry.getKey());
			}
			if (result.startsWith("[") && result.endsWith("]")) {
				Gson gson = GsonBuilderWithoutTimezone(timeformat);
				GPSDataModel[] dataArray = gson.fromJson(result, GPSDataModel[].class);
				for (GPSDataModel dataModel : dataArray) {
					// Added on 2020-OCT-15 Ignition remains false/OFF even when speed is > 0 so
					// checking speed for all devices
					if (dataModel.getSpeed() > 0) {
						dataModel.setAcc(true);
					} else {
						dataModel.setAcc(false);
					}
					// Commented on 2020-OCT-15 Ignition remains false/OFF even when speed is > 0 so
					// checking speed for all devices
					/*
					 * if(dataModel.getIgnition().equalsIgnoreCase("ON")) { dataModel.setAcc(true);
					 * }
					 */
					String licensePlate="";
					String licensePlateTemp="";
					if(dataModel.getLicensePlate().contains("-")) {
						licensePlateTemp = (dataModel.getLicensePlate()).replaceAll("[\\s\\-()]", "");
						licensePlate = licensePlateTemp.toUpperCase();
						dataModel.setLicensePlate(licensePlate);
					}else {
						licensePlate =  dataModel.getLicensePlate().toUpperCase();
						dataModel.setLicensePlate(licensePlate);
					}
					 
					int companyId = Integer.valueOf(companyID);
					String mUnitId = "";
					mUnitId = CommonMethods.getVehicleMobileUnitId(companyId, licensePlate);
//					log.info("LicensePlate in  GLOBALNISSAN : " +licensePlate);
					if(!mUnitId.equals("")) {
						dataModel.setMobileunitid(mUnitId);
						dataList.add(dataModel);
//						log.info(companyID + " dataList GLOBALNISSAN : " + dataList.size() + " Time : " + new Date());
					}else {
//						log.info("data field not available in  GLOBALNISSAN : " +dataList);
					}

				}
			}
			
			
			
			
//			JSONObject jo = new JSONObject(data);
//			log.info("Inside GLOBALNISSAN get data ");
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
//				log.info("data field not available in  GLOBALNISSAN : " +data);
//			}
			

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
}
