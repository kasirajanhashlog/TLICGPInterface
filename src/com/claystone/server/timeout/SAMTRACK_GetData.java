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

public class SAMTRACK_GetData {
	private Logger log;

	public SAMTRACK_GetData() {
		log = Logger.getLogger(SAMTRACK_GetData.class);

	}
	public ArrayList<GPSDataModel> GetDataFromSAMTRACK(String URL, String companyID,
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
				JSONObject jo = new JSONObject(data);
				JSONArray details = jo.getJSONArray("Vehicle");
				if(details!=null && details.length() > 0) {
					System.out.println("details.length:"+details.length());
					for(int lCount=0;lCount < details.length();lCount++ ) {
						JSONObject llData = details.getJSONObject(lCount);
						
						String licensePlate = llData.getString("VehicleNo");
						
					     double latitude = llData.getDouble("Lat");
					     double longitude = llData.getDouble("Long");
					     
					     String ignitionStatus = llData.getString("Ignition");
					     String speedStr = llData.getString("Speed");
					     Integer speedValue = Integer.parseInt(speedStr);
					     
					     String receivedTs = llData.getString("Date");
					     Date gpsDate=null;
					     gpsDate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(receivedTs);
					     
					     String directionStr = llData.getString("Angle");
					     Integer directionValue = Integer.parseInt(directionStr);
//					     Date gpsDate=null;
//							Timestamp timestamp = new Timestamp(receivedTs);
//							SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//					        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
//					        String istTimestamp = sdf.format(timestamp);
//					        gpsDate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(istTimestamp);  
					        
						     String ignitionStr = "Ignition Off";
						     Boolean llAcc = false;
						     if(ignitionStatus.equals("1")) {
						    	 ignitionStr = "Ignition On";
						    	 llAcc = true;
						     }
		  				       GPSDataModel model = new GPSDataModel();
								model.setLicensePlate(licensePlate);
								model.setLatitude(latitude);
								model.setLongitude(longitude);
								model.setSpeed((short) speedValue.intValue());
								model.setAcc(llAcc);
								model.setGpsdate(gpsDate);
								model.setIgnition(ignitionStr);
								model.setGpsstatus(true);
								model.setDirection((short)directionValue.intValue());
;								dataList.add(model);
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
