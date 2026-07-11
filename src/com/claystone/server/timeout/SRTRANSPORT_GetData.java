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

public class SRTRANSPORT_GetData {
	private Logger log;

	public SRTRANSPORT_GetData() {
		log = Logger.getLogger(SRTRANSPORT_GetData.class);

	}
	public ArrayList<GPSDataModel> GetDataFromSRTRANSPORT(String URL, String companyID,
			HashMap<String, String> columnList, String timeformat, String dataProviderName,String userAthentication) {
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
			
		
			http.setRequestProperty(authkey, authvalue);
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
			Integer code = 0;
			if(jo.has("code")) {
				code = jo.getInt("code");
				if(code == 0) {
					JSONArray details = jo.getJSONArray("data");
					if(details!=null && details.length() > 0) {
						for(int lCount=0;lCount < details.length();lCount++) {
							JSONObject llData = details.getJSONObject(lCount);
							String licensePlate = llData.getString("vehicleNo");
							
						     double latitude = llData.getDouble("latitude");
						     double longitude = llData.getDouble("longitude");
						     
						     Boolean ignitionStatus = llData.getBoolean("ignition");
						     Integer speedValue = llData.getInt("speed");
						     
						     long receivedTs = llData.getLong("timestamp");
						     Date gpsDate=null;
								Timestamp timestamp = new Timestamp(receivedTs);
								SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
						        String istTimestamp = sdf.format(timestamp);
						        gpsDate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(istTimestamp);  
						        
							     String ignitionStr = "Ignition Off";
							     Boolean llAcc = false;
							     if(ignitionStatus) {
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
									dataList.add(model);
						}
					}else {
						dataList = new ArrayList<GPSDataModel>();
					}
				}else {
					//failure case
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
