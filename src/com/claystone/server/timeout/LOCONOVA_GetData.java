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

public class LOCONOVA_GetData {
	private Logger log;

	public LOCONOVA_GetData() {
		log = Logger.getLogger(LOCONOVA_GetData.class);

	}
	public ArrayList<GPSDataModel> GetDataFromLOCONOVA(String URL, String companyID,
			HashMap<String, String> columnList, String timeformat, String dataProviderName,String userAthentication,String userId) {
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
			
			String[] tokens0=userAthentication.split(":");  
			String authkey = tokens0[0];
			String authvalue = tokens0[1];
			
			String[] tokens1=userId.split(":");  
			String idkey = tokens1[0];
			String idvalue = tokens1[1];
			
			http.setRequestProperty(authkey, authvalue);
			http.setRequestProperty(idkey, idvalue);
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
			if(jo.has("status")) {
				Boolean successStr = jo.getBoolean("status");
				if(successStr) {
					JSONArray details = jo.getJSONArray("data");
					if(details!=null && details.length() > 0) {
						for (int i = 0; i < details.length(); i++) {
							System.out.println("Index:"+i);
							JSONObject llData = details.getJSONObject(i);
							String licensePlate = llData.getString("number");
							 JSONObject jsonObject = new JSONObject(llData.toString());
							 if (!jsonObject.has("additional_attributes")) {
								 continue;
							}else {
								if (jsonObject.isNull("additional_attributes")) {
								    continue;
								}
							}
							 JSONObject additionalAttributes = jsonObject.getJSONObject("additional_attributes");
							 
							 if (!additionalAttributes.has("movement_metrics")) {
								 continue;
							}else {
								if (additionalAttributes.isNull("movement_metrics")) {
								    continue;
								}
							}
							 
						     JSONObject movementMetrics = additionalAttributes.getJSONObject("movement_metrics");
						     JSONObject location = movementMetrics.getJSONObject("location");
						     long receivedTs = location.getLong("received_ts");
						     double latitude = location.getDouble("lat");
						     double longitude = location.getDouble("long");
						     receivedTs*=1000;//Assume the given value is in seconds so it is multiplied by 1000 to get milliseconds

						     String ignitionStatus = movementMetrics.getString("ignition");
						     JSONObject speedObject = movementMetrics.getJSONObject("speed");
						     int speedValue = speedObject.getInt("value");
						     String ignitionStr = "Ignition Off";
						     if(ignitionStatus.equalsIgnoreCase("on")) {
						    	 ignitionStr = "Ignition On";
						     }
						     boolean llAcc = false;
								if (speedValue > 0) {
									llAcc = true;
								}
								Date gpsDate=null;
								Timestamp timestamp = new Timestamp(receivedTs);
								SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
						        String istTimestamp = sdf.format(timestamp);
						        gpsDate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(istTimestamp);  
//						     System.out.println("Ignition: " + movementMetrics.getString("ignition"));
//						     System.out.println("Location Address: " + location.getString("address"));
//						     System.out.println("Latitude: " + location.getDouble("lat"));
//						     System.out.println("Longitude: " + location.getDouble("long"));
//						     System.out.println("Received Timestamp: " + receivedTs);
//	  				         System.out.println("Speed: " + speedValue + " " + speedObject.getString("unit"));
	  				       GPSDataModel model = new GPSDataModel();
							model.setLicensePlate(licensePlate);
							model.setLatitude(latitude);
							model.setLongitude(longitude);
							model.setSpeed((short) speedValue);
							model.setAcc(llAcc);
							model.setGpsdate(gpsDate);
							model.setIgnition(ignitionStr);
							model.setGpsstatus(true);
							dataList.add(model);

							
						}
					}else {
						dataList = new ArrayList<GPSDataModel>(); 
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
