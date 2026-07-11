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

public class CJDARCL_GetData {

	private Logger log;

	public CJDARCL_GetData() {
		log = Logger.getLogger(CJDARCL_GetData.class);

	}

	public ArrayList<GPSDataModel> GetDataFromCJDARCL(String URL, String companyID,
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
			JSONArray details = new JSONArray(data);
			if(details!=null && details.length()>0) {
				for (int i = 0; i < details.length(); i++) {
					JSONObject obj = details.getJSONObject(i);
					String logDateTime = obj.getString("logDateTime");
		            double latitude = obj.getDouble("latitude");
		            double longitude = obj.getDouble("longitude");
		            String location = obj.isNull("location") ? "null" : obj.getString("location");
		            int speed = obj.getInt("speed");
		            String vehicleNumber = obj.getString("vehicleNumber");
		            String imeiNumber = obj.getString("imeiNumber");
		            boolean ignitionStatus = obj.getBoolean("ignitionStatus");
		            boolean gpsAvailable = obj.getBoolean("gpsAvailable");
		            String companyName = obj.getString("companyName");
		            
		            System.out.println("Vehicle: " + vehicleNumber + ", Speed: " + speed);
		            
		            if(latitude <= 0) {
		            	latitude =0;
		            }
		            if(longitude <= 0) {
		            	longitude =0;
		            }
		            boolean llAcc = false;
		            if(speed>0) {
		            	llAcc = true;
		            }
		            String ignitionStr = "Ignition Off";
		            if(ignitionStatus) {
		            	ignitionStr = "Ignition On";
		            }
		            
					DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					Date gpsDate=null;
					if(!logDateTime.equals("")) {
						String day = logDateTime.substring(0, 2);
						String month = logDateTime.substring(3, 5);
						String year = logDateTime.substring(6, 10);
						String hour = logDateTime.substring(12,14);
						String minute = logDateTime.substring(15,17);
						String sec = logDateTime.substring(18,20);
						String tStamp = year+"-"+month+"-"+day+" "+hour+":"+minute+":"+sec;
						gpsDate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(tStamp);  
					}
		            
		            GPSDataModel model = new GPSDataModel();

					model.setLicensePlate(vehicleNumber);
					model.setLatitude(latitude);
					model.setLongitude(longitude);
					model.setSpeed((short) speed);
					model.setAcc(llAcc);
					model.setGpsdate(gpsDate);
					model.setIgnition(ignitionStr);
					model.setGpsstatus(true);
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
