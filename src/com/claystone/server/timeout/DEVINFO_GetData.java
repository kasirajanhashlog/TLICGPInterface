package com.claystone.server.timeout;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DEVINFO_GetData {
	private Logger log;

	public DEVINFO_GetData() {
		log = Logger.getLogger(DEVINFO_GetData.class);

	}
	public ArrayList<GPSDataModel> GetDataFromDEVINFO(String URL, String companyID,
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

			String data = sb.toString();
			if(data!=null) {
				JSONArray jsonArray = new JSONArray(data);
				if(jsonArray!=null && jsonArray.length() > 0) {
					for (int i = 0; i < jsonArray.length(); i++) {
						JSONObject obj = jsonArray.getJSONObject(i);
						String licensePlate = obj.getString("vehicleName");
					     double latitude = obj.getDouble("lat");
					     double longitude = obj.getDouble("lng");
					     int speedValue = obj.getInt("speed");
					     int accStatus = obj.getInt("acc_status");
					     int directionStr = obj.getInt("heading");
					     int mileage = obj.getInt("odometer");
					     String dateTimeStr = obj.getString("datetime");

						Date gpsDate=null;
						DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						if(!dateTimeStr.equals("")) {
							String year = dateTimeStr.substring(0, 4);
							String month = dateTimeStr.substring(5, 7);
							String day = dateTimeStr.substring(8, 10);
							
							String hour = dateTimeStr.substring(11,13);
							String minute = dateTimeStr.substring(14, 16);
							String sec = dateTimeStr.substring(17, 19);
							String tStamp = year+"-"+month+"-"+day+" "+hour+":"+minute+":"+sec;
							gpsDate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(tStamp);  
						} 
				        
				        
				        boolean lAcc = false; 
				        if(accStatus>0) {
				        	lAcc = true;
				        }
					     String ignitionStr = "Ignition Off";
					     if(lAcc) {
					    	 ignitionStr = "Ignition On";
					     }
  				        GPSDataModel model = new GPSDataModel();
						model.setLicensePlate(licensePlate);
						model.setLatitude(latitude);
						model.setLongitude(longitude);
						model.setSpeed((short) speedValue);
						model.setAcc(lAcc);
						model.setGpsdate(gpsDate);
						model.setIgnition(ignitionStr);
						model.setGpsstatus(true);
						model.setMileage(mileage);
						dataList.add(model);
					}
				}else {
					dataList = new ArrayList<GPSDataModel>();
				}
			}else {
				dataList = new ArrayList<GPSDataModel>();
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
