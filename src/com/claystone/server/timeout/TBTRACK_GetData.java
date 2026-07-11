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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TBTRACK_GetData {
	private Logger log;

	public TBTRACK_GetData() {
		log = Logger.getLogger(TBTRACK_GetData.class);

	}
	public ArrayList<GPSDataModel> GetDataFromTBTRACK(String URL, String companyID,
			HashMap<String, String> columnList, String timeformat, String dataProviderName,String apiReqParam) {
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
			
			String[] tokens=apiReqParam.split(":");  
			String key = tokens[0];
			String value = tokens[1];
			
			
			http.setDoOutput(true);
			http.setRequestProperty(key, value);
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
			if(result!=null) {
				JSONObject jo = new JSONObject(result);
				// basic validation
				if (jo.getInt("code") != 0) {
					dataList = new ArrayList<GPSDataModel>(); 
				    throw new IllegalStateException("API error, code=" + jo.getInt("code"));
				}
				if (!"SUCCESS".equals(jo.optString("status"))) {
					dataList = new ArrayList<GPSDataModel>(); 
				    throw new IllegalStateException("API status not SUCCESS: " + jo.optString("status"));
				}
				// data array (validate non-empty)
				JSONArray dataArr = jo.optJSONArray("data");
				if (dataArr == null || dataArr.length()<=0) {
					dataList = new ArrayList<GPSDataModel>(); 
				    throw new IllegalStateException("No data elements present");
				}
				
				// iterate vehicles
				for (int i = 0; i < dataArr.length(); i++) {
				    JSONObject obj = dataArr.getJSONObject(i);

				    // required fields with validation
				    int speed = obj.getInt("speed");                 // will throw if missing / not int
				    double latitude = obj.getDouble("latitude");
				    double longitude = obj.getDouble("longitude");
				    boolean ignition = obj.getBoolean("ignition");
				    String licensePlate = obj.getString("vehicleNo");
				    long receivedTs = obj.getLong("timestamp");
					Date gpsDate=null;
					Timestamp timestamp = new Timestamp(receivedTs);
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
			        String istTimestamp = sdf.format(timestamp);
			        gpsDate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(istTimestamp); 

				    if (licensePlate == null || licensePlate.trim().isEmpty()) {
				    	dataList = new ArrayList<GPSDataModel>(); 
				        throw new IllegalStateException("vehicleNo is blank at index " + i);
				    }

				    // optional fields with defaults
				    String alias = obj.optString("alias", "");
				    String imei = obj.optString("imei", "");
				    String vehicleStatus = obj.optString("vehicleStatus", "UNKNOWN");
				    int direction = obj.optInt("direction", -1);
				    String vendor = obj.optString("vendor", "");

				    double totalGpsDuration = obj.optDouble("totalGpsDuration", 0.0);
				    double totalGpsOdometer = obj.optDouble("totalGpsOdometer", 0.0);

				    // use values as needed
				    System.out.println("Vehicle: " + licensePlate +
				            " speed=" + speed +
				            " lat=" + latitude +
				            " lon=" + longitude +
				            " status=" + vehicleStatus);
				    
				     String ignitionStr = "Ignition Off";
				     Boolean llAcc = false;
				     if(ignition) {
				    	 ignitionStr = "Ignition On";
				    	 llAcc = true;
				     }
				    
				       GPSDataModel model = new GPSDataModel();
						model.setLicensePlate(licensePlate);
						model.setLatitude(latitude);
						model.setLongitude(longitude);
						model.setSpeed((short) speed);
						model.setAcc(llAcc);
						model.setGpsdate(gpsDate);
						model.setIgnition(ignitionStr);
						model.setGpsstatus(true);
						model.setDirection((short)direction);
						model.setMileage((int)totalGpsOdometer);
						dataList.add(model);
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
