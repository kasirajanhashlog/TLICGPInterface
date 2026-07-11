package com.claystone.server.timeout;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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

public class ROADO_GetData {
	private Logger log;

	public ROADO_GetData() {
		log = Logger.getLogger(ROADO_GetData.class);

	}
	public ArrayList<GPSDataModel> GetDataFromROADO(String URL, String companyID,
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
			http.setRequestMethod("POST"); // POST
			
			String[] apitokens=apiReqParam.split(",");  
			String authtoken = apitokens[0];
			String DNTtoken = apitokens[1];
			String Origintoken = apitokens[2];
			
			String[] tokens=authtoken.split("=");  
			String authkey = tokens[0];
			String authvalue = tokens[1];
			
			String[] dnttokens=DNTtoken.split("=");  
			String DNTkey = dnttokens[0];
			String DNTvalue = dnttokens[1];
			
			String[] Origintokens=Origintoken.split("=");  
			String Originkey = Origintokens[0];
			String Originvalue = Origintokens[1];
			
			http.setRequestProperty(authkey, authvalue);
			http.setRequestProperty(DNTkey, DNTvalue);
			http.setRequestProperty(Originkey, Originvalue);
			http.setDoOutput(true);
			http.setRequestProperty("Content-Type", "application/json");
			http.setDoOutput(true);
			
            String jsonInputString = "{"
                    + "\"query\":\"query FetchLRsForIndustry ($payload: LRsForIndustryInput!) \\r\\n{\\r\\n    fetchLRsForIndustry(payload: $payload) \\r\\n    {\\r\\n\\t\\t  uid\\r\\n      status\\r\\n      lrNumber\\r\\n      billTo \\r\\n      {\\r\\n        name\\r\\n      }\\r\\n      consignor \\r\\n      {\\r\\n        name\\r\\n      }\\r\\n      consignee\\r\\n      {\\r\\n        name\\r\\n      }\\r\\n      source \\r\\n      {\\r\\n          address\\r\\n          {\\r\\n            address\\r\\n          }\\r\\n      }\\r\\n      destination\\r\\n      {\\r\\n        address\\r\\n        {\\r\\n          address\\r\\n        }\\r\\n      }\\r\\n      assignment \\r\\n      {\\r\\n        vehicleNo\\r\\n        startedAt\\r\\n        lastLocation\\r\\n        {\\r\\n          address\\r\\n          latitude\\r\\n          longitude\\r\\n          timestamp\\r\\n        }\\r\\n      }\\r\\n      transporterName\\r\\n    }\\r\\n}\\r\\n\\r\\n  \","
                    + "\"variables\":{"
                    + "\"payload\":{"
                    + "\"sortFilter\":{\"sortBy\":\"createdAt\",\"sortValue\":\"DESC\"},"
                    + "\"pagination\":{\"limit\":50,\"page\":1}"
                    + "}}"
                    + "}";
            
            try (OutputStream os = http.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
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

			String rawdata = sb.toString();
			
			JSONObject dataObject = new JSONObject(rawdata);
			
			if(dataObject.has("data")) {
				JSONObject data = dataObject.getJSONObject("data");
				if(data!=null) {
					if(data.has("fetchLRsForIndustry") && !data.isNull("fetchLRsForIndustry")) {
						JSONArray vehicleDataArray = data.getJSONArray("fetchLRsForIndustry");
						if(vehicleDataArray!=null && vehicleDataArray.length() > 0) {
							for (int i = 0; i < vehicleDataArray.length(); i++) {
								JSONObject vehicle = vehicleDataArray.getJSONObject(i);
								if(vehicle.has("assignment")&& !vehicle.isNull("assignment") ) {
									JSONObject assignment = vehicle.getJSONObject("assignment");
									String licensePlate = assignment.getString("vehicleNo");
									if(assignment.has("lastLocation") && !assignment.isNull("lastLocation")) {
										JSONObject lastLocation = assignment.getJSONObject("lastLocation");
										if(lastLocation!=null) {
											double latitude = lastLocation.getDouble("latitude");
											double longitude = lastLocation.getDouble("longitude");
											String lDt = lastLocation.getString("timestamp");
											SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss");
											// input is in UTC
											sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
											Date utcTimeStamp = sdf.parse(lDt);
											// another formatter for output
											SimpleDateFormat outputFormat= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
											outputFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
											outputFormat.format(utcTimeStamp);
//											log.info("outputFormat:- "+outputFormat.format(utcTimeStamp));
											DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
											Date gpsDate=formatstr.parse(outputFormat.format(utcTimeStamp));
											
							                GPSDataModel model = new GPSDataModel();
											model.setLicensePlate(licensePlate);
											model.setLatitude(latitude);
											model.setLongitude(longitude);
											model.setGpsdate(gpsDate);
											model.setGpsstatus(true);
											dataList.add(model);
										}else {
											break;
										}
									}
								}
							}
						}else {
							dataList = new ArrayList<GPSDataModel>(); 
						}
					}else {
						dataList = new ArrayList<GPSDataModel>(); 
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
