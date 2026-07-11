package com.claystone.server.timeout;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;


import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.claystone.common.utils.CommonConstants;
import com.claystone.db.DataProvider;
import com.claystone.server.util.HibernateUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class EICHER_GetData {
	private static Logger log;
	private Properties properties = new Properties();
	private int vehicleLimit = 1;
	private String vehicleLimitStr = "1";
//	private String lSAMRXVehicleURL = "";

	public EICHER_GetData() {
		log = Logger.getLogger(EICHER_GetData.class);
		try {
			properties.load(new FileInputStream("server.properties"));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			log.error("Exception", e1);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			log.error("Exception", e1);
		}
		vehicleLimitStr = properties.getProperty("TATAMOTOR-VEHICLE_LIMIT");
		if(!vehicleLimitStr.equals("")) {
			vehicleLimit = Integer.parseInt(vehicleLimitStr); 
		}

	}

	public ArrayList<GPSDataModel> GetDataFromEICHER(String URL, String companyID, HashMap<String, String> columnList,
			String timeformat,String tokenURL,String tokenReqParams,String apiUrl,String apiReqParams, int dataProviderId, String dataProviderName,
			MobileUnitTimestamp companyTimestampHash) throws IOException {

		ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
		InputStream in = null;
		Reader r = null;
		HttpURLConnection http = null;
		HttpURLConnection http2 = null;
		int serverResponseCode;
		StringBuilder sb = null;
		InputStreamReader inReader;
//		String llToken = clientSecret;
		String llToken = null;
		String clientID = "";
		String grantType = "";
		String clientSecret="";
		
		//To get the API-KEY value
		String s = tokenReqParams.trim();               // {"API-KEY":"06af..."}
		s = s.substring(1, s.length() - 1);             // "API-KEY":"06af..."

		String[] parts = s.split(":", 2);
		String apiHeaderName  = parts[0].trim().replace("\"", "");   // API-KEY
		String apiHeaderValue = parts[1].trim().replace("\"", "");   // 06af85bc...



		
		
		llToken = GetEICHERToken(tokenURL, apiHeaderName, apiHeaderValue);
		if(llToken!=null && !llToken.isEmpty()) {
			URL url = null;
			try {
				url = new URL(URL);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			URLConnection con = null;
			try {
				con = url.openConnection();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			http = (HttpURLConnection) con;
			try {
				http.setRequestMethod("POST");
			} catch (ProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} // GET
			
			


			String jsonapi = apiReqParams;

			JsonParser parser = new JsonParser();
			JsonObject jsonObj = parser.parse(jsonapi).getAsJsonObject();

			String apiKey      = jsonObj.get("API-KEY").getAsString();
			String clientId    = jsonObj.get("clientId").getAsString();
			
			http.setRequestProperty("API-KEY", apiKey);
			http.setRequestProperty("Authorization", "Bearer " + llToken);
			http.setRequestProperty("Content-Type", "application/json");
			http.setRequestProperty("Accept", "application/json");
			http.setDoOutput(true);

	        // JSON body equivalent to --data '{ "clientId":"06af85bc-..." }'
	        String jsonBody = "{"
	                + "\"clientId\":\"" + clientId + "\""
	                + "}";
	        
	        try (OutputStream os = http.getOutputStream()) {
	            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
	            os.write(input);
	        } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        int status = 0;
			try {
				status = http.getResponseCode();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

	        InputStream is = null;
			try {
				is = (status >= 200 && status < 300)
				        ? http.getInputStream()
				        : http.getErrorStream();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

	        StringBuilder response = new StringBuilder();
	        try (BufferedReader br = new BufferedReader(
	                new InputStreamReader(is, StandardCharsets.UTF_8))) {
	            String line;
	            try {
					while ((line = br.readLine()) != null) {
					    response.append(line);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        }

	        if (status < 200 || status >= 300) {
	            throw new IOException("HTTP " + status + " from VECV: " + response);
	        }

	        String data =  response.toString();
			System.out.println("EicherData:"+data);
			
//			if(data!=null) {
//				JsonParser parserData = new JsonParser();
//				JsonElement rootEl = parserData.parse(data);
//
//				if (!rootEl.isJsonObject()) {
//				    // invalid JSON structure
//					dataList = null;
//				    return dataList;
//				}
//				JsonObject root = rootEl.getAsJsonObject();
//
//				// errorMessage (optional)
//				String errorMessage = root.has("errorMessage") && !root.get("errorMessage").isJsonNull()
//				        ? root.get("errorMessage").getAsString()
//				        : null;
//
//				// locationData must be an array with at least one element
//				if (!root.has("locationData") || !root.get("locationData").isJsonArray()) {
//				    // handle: missing locationData
//					dataList = null;
//				    return dataList;
//				}
//
//				JsonArray locArray = root.getAsJsonArray("locationData");
//				if (locArray.size() == 0) {
//				    // handle: empty array
//					dataList = null;
//				    return dataList;
//				}
//
//				JsonElement firstEl = locArray.get(0);
//				if (!firstEl.isJsonObject()) {
//				    // handle: unexpected structure
//					dataList = null;
//				    return dataList;
//				}
//				JsonObject loc = firstEl.getAsJsonObject();
//
//				// Safe getter helper
//				String regNo = loc.has("regNo") && !loc.get("regNo").isJsonNull()
//				        ? loc.get("regNo").getAsString()
//				        : null;
//
//				String chassisNo = loc.has("chassisNo") && !loc.get("chassisNo").isJsonNull()
//				        ? loc.get("chassisNo").getAsString()
//				        : null;
//
//				Double latitude = loc.has("latitude") && !loc.get("latitude").isJsonNull()
//				        ? loc.get("latitude").getAsDouble()
//				        : null;
//
//				Double longitude = loc.has("longitude") && !loc.get("longitude").isJsonNull()
//				        ? loc.get("longitude").getAsDouble()
//				        : null;
//
//				String vehicleStatus = loc.has("vehicleStatus") && !loc.get("vehicleStatus").isJsonNull()
//				        ? loc.get("vehicleStatus").getAsString()
//				        : null;
//				boolean lAcc = false;
//				if(vehicleStatus.equalsIgnoreCase("MOVING")||vehicleStatus.equalsIgnoreCase("IDLING")) {
//					lAcc = true;
//				}
//				Long lastUpdated = loc.has("lastUpdated") && !loc.get("lastUpdated").isJsonNull()
//				        ? loc.get("lastUpdated").getAsLong()
//				        : null;
//				String lastUpdatedStr = lastUpdated.toString();
//				DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//				Date gpsDate = null;
//				if(!lastUpdatedStr.equals("")) {
//					String year = lastUpdatedStr.substring(0, 4);
//					String month = lastUpdatedStr.substring(4, 6);
//					String day = lastUpdatedStr.substring(6, 8);
//					String hour = lastUpdatedStr.substring(8,10);
//					String minute = lastUpdatedStr.substring(10, 12);
//					String sec = lastUpdatedStr.substring(12, 14);
//					String tStamp = year+"-"+month+"-"+day+" "+hour+":"+minute+":"+sec;
//					try {
//						gpsDate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(tStamp);
//					} catch (ParseException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}  
//				}
//				
//
//				Long epochTime = loc.has("epochTime") && !loc.get("epochTime").isJsonNull()
//				        ? loc.get("epochTime").getAsLong()
//				        : null;
//
//				Double vehicleSpeed = loc.has("vehicleSpeed") && !loc.get("vehicleSpeed").isJsonNull()
//				        ? loc.get("vehicleSpeed").getAsDouble()
//				        : null;
//				
//				short speedShort = 0;
//				speedShort = (short) vehicleSpeed.doubleValue(); 
//
//				Double odometer = loc.has("odometer") && !loc.get("odometer").isJsonNull()
//				        ? loc.get("odometer").getAsDouble()
//				        : null;
//				Integer milageInt = 0;
//				milageInt = (Integer) odometer.intValue();
//				
//				Double vehicleDir = loc.has("vehicleDirection") && !loc.get("vehicleDirection").isJsonNull()
//				        ? loc.get("vehicleDirection").getAsDouble()
//				        : null;
//				short directionShort = 0;
//				directionShort = (short) vehicleDir.doubleValue(); 
//				
//				String deviceId = loc.has("deviceId") && !loc.get("deviceId").isJsonNull()
//				        ? loc.get("deviceId").getAsString()
//				        : null;
//				
//				if (speedShort > 0 &&  !lAcc) {
//					lAcc = true;
//					
//				}
//	            String ignitionStr = "Ignition Off";
//	            if(lAcc) {
//	            	ignitionStr = "Ignition On";
//	            }
//				
//			    GPSDataModel model = new GPSDataModel();
//				model.setLicensePlate(regNo);
//				model.setLatitude(latitude);
//				model.setLongitude(longitude);
//				model.setSpeed(speedShort);
//				model.setAcc(lAcc);
//				model.setGpsdate(gpsDate);
//				model.setIgnition(ignitionStr);
//				model.setGpsstatus(true);
//				model.setMileage(milageInt);
//				model.setDirection(directionShort);
//				System.out.println("GpsData:"+model.getGpsdate());
//				dataList.add(model);
//				
//
//			}else {
//				dataList = new ArrayList<GPSDataModel>();
//			}
			
			try {
				JSONObject result = new JSONObject(data);
		        // Validate errorMessage
		        String errorMessage = result.optString("errorMessage", "");
		        if (!"None".equalsIgnoreCase(errorMessage)) {
		        	dataList = new ArrayList<GPSDataModel>();
		            throw new IllegalStateException("API error: " + errorMessage);
		        }
		        // Validate and get locationData array
		        JSONArray locArray = result.optJSONArray("locationData");
		        if (locArray == null || locArray.length() == 0) {
		        	dataList = new ArrayList<GPSDataModel>();
		            throw new IllegalStateException("locationData is missing or empty");
		        }
		        
		        for (int i = 0; i < locArray.length(); i++) {
		            JSONObject obj = locArray.getJSONObject(i);

		            // Required strings
		            String licensePlate       = obj.getString("regNo");
		            String chassisNo   = obj.getString("chassisNo");
		            String vehicleStatus = obj.getString("vehicleStatus");
//		            String deviceId    = obj.getString("deviceId");

		            if (licensePlate == null || licensePlate.trim().isEmpty()) {
//		            	dataList = new ArrayList<GPSDataModel>();
//		                throw new IllegalStateException("regNo is blank at index " + i);
		            }
		            if (chassisNo == null || chassisNo.trim().isEmpty()) {
		            	dataList = new ArrayList<GPSDataModel>();
		                throw new IllegalStateException("chassisNo is blank at index " + i);
		            }

		            // Required numeric values
		            double latitude  = obj.getDouble("latitude");
		            double longitude = obj.getDouble("longitude");
		            double vehicleSpeed = obj.getDouble("vehicleSpeed");
		            double odometer  = obj.getDouble("odometer");
		            double vehicleDirection = obj.getDouble("vehicleDirection");

		            long lastUpdated = obj.getLong("lastUpdated"); // yyyymmddHHMMss as long
		            String lastUpdatedStr = String.valueOf(lastUpdated);
					DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					Date gpsDate = null;
					if(!lastUpdatedStr.equals("")) {
						String year = lastUpdatedStr.substring(0, 4);
						String month = lastUpdatedStr.substring(4, 6);
						String day = lastUpdatedStr.substring(6, 8);
						String hour = lastUpdatedStr.substring(8,10);
						String minute = lastUpdatedStr.substring(10, 12);
						String sec = lastUpdatedStr.substring(12, 14);
						String tStamp = year+"-"+month+"-"+day+" "+hour+":"+minute+":"+sec;
						try {
							gpsDate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(tStamp);
						} catch (ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}  
					}
		            
		            
		            
		            long epochTime   = obj.getLong("epochTime");   // seconds since epoch

		            // Coordinate validation
		            if (latitude < -90 || latitude > 90) {
		                throw new IllegalStateException("Invalid latitude at index " + i + ": " + latitude);
		            }
		            if (longitude < -180 || longitude > 180) {
		                throw new IllegalStateException("Invalid longitude at index " + i + ": " + longitude);
		            }

		            // Epoch basic validation (must be positive)
		            if (epochTime <= 0) {
		                throw new IllegalStateException("Invalid epochTime at index " + i + ": " + epochTime);
		            }

		            // Use data as needed
		            System.out.println("Vehicle " + licensePlate +
		                    " lat=" + latitude +
		                    " lon=" + longitude +
		                    " speed=" + vehicleSpeed +
		                    " status=" + vehicleStatus +
		                    " epochTime=" + epochTime +
		                    " lastUpdated=" + lastUpdated +
		                    " gpsDate="+ gpsDate);
		            
				     String ignitionStr = "Ignition Off";
				     Boolean llAcc = false;
				     if(vehicleSpeed > 0) {
				    	 ignitionStr = "Ignition On";
				    	 llAcc = true;
				     }
		            
				       GPSDataModel model = new GPSDataModel();
//						model.setLicensePlate(licensePlate);
						model.setLatitude(latitude);
						model.setLongitude(longitude);
						model.setSpeed((short) vehicleSpeed);
						model.setAcc(llAcc);
						model.setGpsdate(gpsDate);
						model.setIgnition(ignitionStr);
						model.setGpsstatus(true);
						model.setDirection((short)vehicleDirection);
						model.setMileage((int)odometer);
						model.setMobileunitid(chassisNo);
						dataList.add(model);
		        }
		        
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
		return dataList;
	}
	

	private String GetEICHERToken(String tokenURL, String apiHeaderName, String apiHeaderValue) {
		String data = "";
		URL url;
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(apiHeaderName, apiHeaderValue);

		Set set = params.entrySet();
		Iterator i = set.iterator();
		try {

			url = new URL(tokenURL);
//			byte[] postDataBytes = postData.toString().getBytes("UTF-8");
			HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty(apiHeaderName,apiHeaderValue);
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setDoOutput(true);
//			conn.getOutputStream().write(postDataBytes);
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			StringBuilder builder = new StringBuilder();
			for (String line = null; (line = reader.readLine()) != null;) {
				builder.append(line).append("\n");
			}
			reader.close();
			conn.disconnect();
			try {
				JSONObject lEicherToken = new JSONObject(builder.toString());
				data = lEicherToken.getString("token");
				log.info("Eicher token returned");
			} catch (JSONException e) {
				log.error(e.getMessage());
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error(e.getMessage());
		}

		return data;
	}
	public static boolean CheckToken(String lToken,int dataProviderId,String dataProviderName ) {
		try {
			Session session = HibernateUtil.beginTransaction();
			ArrayList<DataProvider> reqToken = HibernateUtil.castList(DataProvider.class,
					session.createQuery(" from DataProvider	" + " where id.dataProviderId = '" + dataProviderId + "'"
							+ " and api_req_params ='" + lToken + "'"
							+ " and id.dataProviderName = '" + dataProviderName + "' and recordStatus = "
							+ CommonConstants.RECORD_STATUS_ACTIVE).list());
			if (reqToken.size() > 0) {
				return true;
			}
			HibernateUtil.commit();
		} catch (Throwable e) {
			log.error(e.getMessage());
			e.printStackTrace();
			HibernateUtil.rollback();
		}
		return false;
	}
	public static void UpdateToken(String newToken,int dataProviderId,String dataProviderName ) {
		try {
			Session session = HibernateUtil.beginTransaction();
			ArrayList<DataProvider> lDataProviderList = HibernateUtil.castList(DataProvider.class,
					session.createQuery(" from DataProvider	" + " where id.dataProviderId = '" + dataProviderId + "'"
							+ " and id.dataProviderName = '" + dataProviderName + "' and recordStatus = "
							+ CommonConstants.RECORD_STATUS_ACTIVE).list());
			if (lDataProviderList.size() > 0) {
				DataProvider lDataProvider = lDataProviderList.get(0);
				lDataProvider.setApiReqParams(newToken);
				session.update(lDataProvider);
				HibernateUtil.commit();
			}

		} catch (Throwable e) {
			e.printStackTrace();
			log.error(e.getMessage());
			HibernateUtil.rollback();
		}
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
