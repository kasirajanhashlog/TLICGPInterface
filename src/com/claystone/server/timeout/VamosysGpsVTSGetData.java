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
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;


public class VamosysGpsVTSGetData {

	private Logger log;

	public VamosysGpsVTSGetData() {
		log = Logger.getLogger(VamosysGpsVTSGetData.class);

	}

	public ArrayList<GPSDataModel>  GetDataVamosysGpsVTSFromAPI(String URL, String unitID, HashMap<String, String> columnList,
			String timeformat) {
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
			JSONObject jo = new JSONObject(data);
			log.info("Inside VamosysGpsVTS get data:  ");
			if(jo.has("shortName") && !jo.isNull("shortName") )
			{
				String llVehicleLicensePlate = jo.getString("shortName");
				if(jo.has("vehicleLocations") && !jo.isNull("vehicleLocations")) {
					JSONArray details = jo.getJSONArray("vehicleLocations"); 
					for (int i = 0; i < details.length(); i++) {
						JSONObject llData = details.getJSONObject(i);
						boolean llAcc = false;				
						Double llLat = llData.getDouble("latitude");
						Double llLon = llData.getDouble("longitude");
						DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						//	String lDt = llData.getString("lastSeen");
						//Date llGpSt = formatstr.parse(lDt);			
						long llTime = llData.getLong("date");
						Date llGpSt = new Date(llTime);
						int llSpeed = llData.getInt("speed");
						String llIgnition = llData.getString("ignitionStatus");
						if(llIgnition.equals("ON"))
						{
							llAcc = true;
						} else if(llIgnition.equals("OFF")){
							llAcc = false;
						}
						if (llSpeed > 0) {
							llAcc = true;
						}
						GPSDataModel model = new GPSDataModel();
						model.setLicensePlate(llVehicleLicensePlate);
						model.setLatitude(llLat);
						model.setLongitude(llLon);
						model.setGpsdate(llGpSt);
						model.setSpeed((short) llSpeed);
						model.setAcc(llAcc);
						model.setMobileunitid(unitID);
						dataList.add(model);

					}
				}else {
					log.info("VamosysGpsVTS vehicleLocations not Available in input data :  " + data);		
				}

			}
			else {
				log.info("VamosysGpsVTS shortName not Available in input data :  " + data);				
				log.info("VamosysGpsVTS shortName not Available in input data URL hit :  " + URL);
			}

			log.info(" dataList VamosysGpsVTS : " + dataList.size() + " Time : " + new Date());

		} catch (Exception e) {log.error(e.getMessage());
			e.printStackTrace();
		}
		return dataList;

	}


}
