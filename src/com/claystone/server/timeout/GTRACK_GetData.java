package com.claystone.server.timeout;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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

public class GTRACK_GetData {

	private Logger log;

	public GTRACK_GetData() {
		log = Logger.getLogger(GTRACK_GetData.class);

	}

	public ArrayList<GPSDataModel> GetDataFromGTRACK(String URL, String companyID,
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
			http.setRequestMethod("POST"); // POST
			
			String[] tokens=accessKey.split(":");  
			String key = tokens[0];
			String value = tokens[1];
			http.setRequestProperty(key, value);
			http.setDoOutput(true);
//			http.setRequestProperty("Content-Type", "application/json");
			OutputStream osw = http.getOutputStream();
			
//			http.connect();
	           // (Optional) send an empty body since original curl sends none
            try (OutputStream os = http.getOutputStream()) {
            	osw.write(new byte[0]);
            }

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
					String logDateTime = obj.getString("dttime");
		            double latitude = obj.getDouble("lat");
		            double longitude = obj.getDouble("lngt");
		            String location = obj.isNull("location") ? "null" : obj.getString("location");
		            int speed = obj.getInt("speed");
					String milageStr = (String)obj.get("odo");
					int milage = (Integer.valueOf(milageStr));	
					
		            String vehicleNumber = obj.getString("vname");
		            
		            String ignitionStatusStr = obj.getString("ignition");
		            boolean ignitionStatus = Boolean.parseBoolean(ignitionStatusStr);
		            
		            double directionDbl = obj.getDouble("angle");
		            short direction = (short)directionDbl;
		            
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
						String year = logDateTime.substring(0, 4);
						String month = logDateTime.substring(5, 7);
						String day = logDateTime.substring(8, 10);
						String hour = logDateTime.substring(11,13);
						String minute = logDateTime.substring(14,16);
						String sec = logDateTime.substring(17,19);
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
					model.setDirection(direction);
					model.setMileage(milage);
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
