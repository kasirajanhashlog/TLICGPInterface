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

public class EGTRACKERS_GetData {
	private Logger log;

	public EGTRACKERS_GetData() {
		log = Logger.getLogger(EGTRACKERS_GetData.class);

	}
	public ArrayList<GPSDataModel> GetDataFromEGTRACKERS(String URL, String companyID,
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
						String licensePlate = obj.getString("vehicleNumber");
					     double latitude = obj.getDouble("lat");
					     double longitude = obj.getDouble("long");
					     int speedValue = obj.getInt("speed");
					     boolean accStatus = obj.getBoolean("ignition");
					     int directionStr = obj.getInt("direction");
					     String dateTimeStr = obj.getString("last_updated");

						Date gpsDate=null;
						DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						if(!dateTimeStr.equals("")) {
							SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss");
							// input is in UTC
							sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
							Date utcTimeStamp = sdf.parse(dateTimeStr);
							// another formatter for output
							SimpleDateFormat outputFormat= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							outputFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
							outputFormat.format(utcTimeStamp);
							log.info("outputFormat:- "+outputFormat.format(utcTimeStamp));
							gpsDate=formatstr.parse(outputFormat.format(utcTimeStamp));
						} 
				        
				        
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
