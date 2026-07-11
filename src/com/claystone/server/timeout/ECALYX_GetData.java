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
import java.text.ParseException;
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

public class ECALYX_GetData {
	private Logger log;

	public ECALYX_GetData() {
		log = Logger.getLogger(ECALYX_GetData.class);

	}
	public ArrayList<GPSDataModel> GetDataFromECALYX(String URL, String companyID,
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

			String result = sb.toString();
			
			
			if(result!=null) {
				// Basic validation: non-null and non-empty
				JSONArray dataArr = new JSONArray(result);
				if (dataArr.length() == 0) {
				    throw new IllegalStateException("No job records present");
				}
				
				// iterate vehicles
				SimpleDateFormat inFmt  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
				SimpleDateFormat outFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				
				for (int i = 0; i < dataArr.length(); i++) {
				    JSONObject obj = dataArr.getJSONObject(i);
				    Date gpsdate =null;

				    // Required fields (will throw JSONException if missing / wrong type)
				    int custId = obj.getInt("CUST_ID");
				    String custName = obj.getString("CUST_NAME");
				    String licensePlate = obj.getString("VEH_NO");
				    double latitude = obj.getDouble("JOB_LAT");
				    double longitude = obj.getDouble("JOB_LNG");
				    String jobCurrAddress = obj.getString("JOB_CURR_ADDRESS");
				    String jobResDateStr = obj.getString("JOB_RES_DATE");
				    try {
				        Date d = inFmt.parse(jobResDateStr);
				        String formatted = outFmt.format(d);               // "2026-02-13 09:47:00"

				        // put back into JSON or use elsewhere
				        obj.put("JOB_RES_DATE", formatted);
//				        System.out.println("Converted JOB_RES_DATE: " + formatted);
				        try {
				            Date dt = outFmt.parse(formatted);              // d is a Date object
				            String back = outFmt.format(dt);        // "2026-02-13 09:47:00"
//				            System.out.println(back);
				            gpsdate = dt;
				        } catch (ParseException e) {
				            e.printStackTrace();
				        }
				        
				    } catch (ParseException e) {
				        throw new IllegalStateException("Invalid JOB_RES_DATE: " + jobResDateStr, e);
				    }
				
				    String jobCode = obj.getString("JOB_CODE");

				    // Validate some required strings
				    if (licensePlate == null || licensePlate.trim().isEmpty()) {
				        throw new IllegalStateException("VEH_NO is blank at index " + i);
				    }
				    
				    // Optional / messy field: SPEED is "" in your sample
				    // Use optString and handle empty string as "no speed"
				    String speedStr = obj.optString("SPEED", "");
				    Double speed = null;
				    int speedInt = 0;
				    if (speedStr != null && !speedStr.trim().isEmpty()) {
				        try {
				            speed = Double.valueOf(speedStr.trim());
				            speedInt = speed.intValue();
				        } catch (NumberFormatException e) {
				            throw new IllegalStateException("Invalid SPEED value at index " + i + ": " + speedStr, e);
				        }
				    }
				    
				    // use values as needed
				    
				     String ignitionStr = "Ignition Off";
				     Boolean llAcc = false;
				     if(speedInt > 0) {
				    	 ignitionStr = "Ignition On";
				    	 llAcc = true;
				     }else {
				    	 speedInt =0;
				     }
				    
				       GPSDataModel model = new GPSDataModel();
						model.setLicensePlate(licensePlate);
						model.setLatitude(latitude);
						model.setLongitude(longitude);
						model.setSpeed((short) speedInt);
						model.setAcc(llAcc);
						model.setGpsdate(gpsdate);
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
