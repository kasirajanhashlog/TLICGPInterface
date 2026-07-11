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

public class ASSET_TRACKETGetData {

	private Logger log;

	public ASSET_TRACKETGetData() {
		log = Logger.getLogger(ASSET_TRACKETGetData.class);

	}

	public ArrayList<GPSDataModel> GetDataFromASSET_TRACKET(String URL, String companyID,
			HashMap<String, String> columnList, String timeformat, String dataProviderName,String accessKey) {
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
			
			String[] tokens=accessKey.split(":");  
			String key = tokens[0];
			String value = tokens[1];
			
			http.setRequestProperty(key, value);
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
			if(jo.has("success")) {
				Boolean successStr = jo.getBoolean("success");
				if(successStr) {
					JSONArray details = jo.getJSONArray("results");
					for (int i = 0; i < details.length(); i++) {
						JSONObject llData = details.getJSONObject(i);
						Date gpsDate=null;
						boolean llAcc = false;
						String ignitionStr = "Ignition Off";
						String llVehicleLicensePlate = llData.getString("vehicle");
						Double llLat = llData.getDouble("lat");
						Double llLon = llData.getDouble("lon");
						String lDt = llData.getString("time");
						DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						if(!lDt.equals("")) {
							String year = lDt.substring(0, 4);
							String month = lDt.substring(5, 7);
							String day = lDt.substring(8, 10);
							String hour = lDt.substring(10,13);
							String minute = lDt.substring(14,16);
							String sec = lDt.substring(17,19);
							String tStamp = year+"-"+month+"-"+day+""+hour+":"+minute+":"+sec;
							gpsDate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(tStamp);  
						}

						int llSpeed = llData.getInt("speed");
						int ignitionInt = llData.getInt("ignition");
						if (llSpeed > 0) {
							llAcc = true;
						}
						if(ignitionInt > 0) {
							ignitionStr = "Ignition On";
						}
						GPSDataModel model = new GPSDataModel();
						model.setLicensePlate(llVehicleLicensePlate);
						model.setLatitude(llLat);
						model.setLongitude(llLon);
						model.setSpeed((short) llSpeed);
						model.setAcc(llAcc);
						model.setGpsdate(gpsDate);
						model.setIgnition(ignitionStr);
						model.setGpsstatus(true);
						dataList.add(model);

					}
				}else {
					dataList = new ArrayList<GPSDataModel>(); 
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
}
