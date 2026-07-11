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

public class MOBILFOX_GetData {
	private Logger log;

	public MOBILFOX_GetData() {
		log = Logger.getLogger(MOBILFOX_GetData.class);

	}
	public ArrayList<GPSDataModel> GetDataFromMOBILFOX(String URL, String companyID,
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
			if(data!=null) {
				JSONObject jsonObject = new JSONObject(data);
				// Extract simple fields
		        int code = jsonObject.getInt("code");
		        String status = jsonObject.getString("status");
		        
		     // Extract the data array
//		        JSONArray dataArray = jsonObject.getJSONArray("data");
		        JSONArray dataArray = jsonObject.optJSONArray("data");
		        if (dataArray != null) {
			        for (int i = 0; i < dataArray.length(); i++) {
			        	JSONObject dataObj = dataArray.getJSONObject(i);
			        	String licensePlate = dataObj.getString("vehicleNo");
					    double latitude = dataObj.getDouble("latitude");
					    double longitude = dataObj.getDouble("longitude");
			        	int speedValue = dataObj.getInt("speed");
			        	boolean accStatus = dataObj.getBoolean("ignition");
			        	int directionStr = dataObj.getInt("direction");
			        	int mileage = dataObj.getInt("totalGpsOdometer");
					    long receivedTs = dataObj.getLong("timestamp");
					    Date gpsDate=null;
							Timestamp timestamp = new Timestamp(receivedTs);
							SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
					        String istTimestamp = sdf.format(timestamp);
					        gpsDate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(istTimestamp);  
			        	
					        boolean lAcc = false; 
					        if(accStatus) {
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
								model.setDirection((short) directionStr);
								System.out.println("GpsData:"+model.getGpsdate());
								dataList.add(model);
			        }
		        } else {
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
