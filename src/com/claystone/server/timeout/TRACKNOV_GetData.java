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

public class TRACKNOV_GetData {
	private static Logger log;
	private Properties properties = new Properties();
	private int vehicleLimit = 1;
	private String vehicleLimitStr = "1";

	public TRACKNOV_GetData() {
		log = Logger.getLogger(TRACKNOV_GetData.class);
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

	public ArrayList<GPSDataModel> GetDataFromTRACKNOV(String URL, String companyID, HashMap<String, String> columnList,
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

		String[] parts = s.split(",", 2);
		String tokenUserNameName  = parts[0].trim().replace("\"", "");   // API-KEY
		String tokenPasswordValue = parts[1].trim().replace("\"", "");   // 06af85bc...
		
		
		llToken = GetTRACKNOVToken(tokenURL, tokenUserNameName, tokenPasswordValue);
		
		if(llToken!=null && !llToken.isEmpty()) {
			String jsonapi = apiReqParams;
			//To get the API-KEY value
			String ap = apiReqParams.trim();               // {"API-KEY":"06af..."}
			ap = ap.substring(1, ap.length() - 1);             // "API-KEY":"06af..."

			String[] apiParamparts = ap.split(",", 2);
			String apiUserNameName  = apiParamparts[0].trim().replace("\"", "");   // API-KEY
			String apiPasswordValue = apiParamparts[1].trim().replace("\"", "");   // 06af85bc...
			
			String userNameString = apiUserNameName;
			String passwordString = apiPasswordValue;
			
			String[] userNameArray = userNameString.split(":", 2);
			String userNameLabel = userNameArray[0];
			String userNameValue = userNameArray[1];
			
			String[] passwordArray = passwordString.split(":", 2);
			String passwordLabel = passwordArray[0];
			String passwordValue = passwordArray[1];
			
			String body = String.format(
				    "{\"company_names\":\"%s\",\"format\":\"%s\"}",
				    userNameValue, passwordValue
				);
			String AUTH_CODE ="7TOwJGy66FQXwI9k6MZu+lj6ecJCLWI/yl/PzUWScIne4YgxroMuUHtVmsgAy81ppojf33uwDYUUFo4hb9/HqRKJQpWNcCJxJkdSERPNudKayw5ZlEiOU9wbSq6Hftq75JmfDPb+sCBo61miuTng7tbFxWz19qi8SVFp2A/Wco139Lp6xiQY15ku7OOg8OCudK6h1S+Ohw5tdhrGRwmtr2C+A3HpDJTO9DlxxbmU3M/wLSwQ4cx7K5SKJjggoVCsrPtAa0S5jj3nPkxmXY1FIimSYsHFBEiSo0eHcO6XNdrnE2hc+m6XTQ6klCdFSOCQgpQu0O9WZxO1VhNI3HB3rKIiWo5Zw3v1oJI7QgyLLbYk2wSC+KOVPl3k045bC+1gMGdHr67w3JIkqUilLv39o1iI/oqRfXPVKmtSvos/lQ1O3tUZkgJWR3iF7U39c8jPsXaTt3phHLRh6rbVyIfOMsbaxd9K3aBRwD1XGkPkhXCRI8R5Vpg5llWCwJjFUAEEOG1WFt8FTE2tmT7e9XUGxuez07GQ83AlH5tqnzEW8Lf9JED3OTCnKCfnm1hMhIdlWnzU1e8/tPpznbx504oJJGa2z+MuIFvLp5eBJ64DDx96/VIZ9PIp1yIz/96Nz2rUj0HbKA3qnvmORuToJgyS+EgAnGPTfEqrBtELioavXEudpRa+eggLPzL7z74q02jElfyD3HONthHNZGh18Gl+jdJGNhXDfxNHNWBbrk2wob9ceKsFlLDqzQ==";
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            //  Request setup
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            //  Headers
            conn.setRequestProperty("Auth-code",llToken );
            conn.setRequestProperty("Authorization", "Bearer "+AUTH_CODE);

            conn.setDoOutput(true);

            //  JSON Body
            String requestBody = "{"
                    + "\"company_names\":\"JimmyIndore\","
                    + "\"format\":\"json\""
                    + "}";

            //  Send request
            OutputStream os = conn.getOutputStream();
            os.write(requestBody.getBytes("utf-8"));
            os.flush();
            os.close();

            //  Response handling
            int responseCode = conn.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            BufferedReader br;
            if (responseCode >= 200 && responseCode < 300) {
                br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            } else {
                br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }

            System.out.println("Response: " + response.toString());
			
            String data =  response.toString();
			
			
//			System.out.println("EicherData:"+data);
			

			
			try {
				JSONObject rootObject = new JSONObject(data);
				if(rootObject.has("root")) {
					JSONObject root = rootObject.getJSONObject("root");
					if(root!=null) {
						if(root.has("VehicleData")&& !root.isNull("VehicleData")) {
							JSONArray vehicleDataArray = root.getJSONArray("VehicleData");
							if(vehicleDataArray!=null && vehicleDataArray.length() > 0) {
								for (int i = 0; i < vehicleDataArray.length(); i++) {
									 JSONObject vehicle = vehicleDataArray.getJSONObject(i);
									 
									String licensePlate = vehicle.getString("Vehicle_No");
					                String latitude = vehicle.getString("Latitude");
					                String longitude = vehicle.getString("Longitude");
					                String ignitionStatusStr = vehicle.getString("IGN");
					                String milageStr = vehicle.getString("Odometer");
					                String speedStr = vehicle.getString("Speed");
					                String directionStr = vehicle.getString("Angle");
					                String gpsActualTime = vehicle.getString("GPSActualTime");
					                
					                boolean acc = false;
					                String ignitionStr = "Ignition Off";
					                if(ignitionStatusStr.equals("ON")) {
					                	ignitionStr = "Ignition On";
					                	acc = true;
					                }
									DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
									Date gpsDate = null;
									if(!gpsActualTime.equals("")) {
										String day = gpsActualTime.substring(0, 2);
										String month = gpsActualTime.substring(3, 5);
										String year = gpsActualTime.substring(6, 10);
										String hour = gpsActualTime.substring(11,13);
										String minute = gpsActualTime.substring(14, 16);
										String sec = gpsActualTime.substring(17, 19);
										String tStamp = year+"-"+month+"-"+day+" "+hour+":"+minute+":"+sec;
										gpsDate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(tStamp);  
									}

					                
					                GPSDataModel model = new GPSDataModel();
									model.setLicensePlate(licensePlate);
									model.setLatitude(Double.parseDouble(latitude));
									model.setLongitude(Double.parseDouble(longitude));
									model.setSpeed((short)Integer.parseInt(speedStr));
									model.setDirection((short)Integer.parseInt(directionStr));
									model.setAcc(acc);
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
					}else {
						dataList = new ArrayList<GPSDataModel>(); 
					}
				}else {
					dataList = new ArrayList<GPSDataModel>(); 
				}
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
		return dataList;
	}
	

	private String GetTRACKNOVToken(String tokenURL, String apiHeaderName, String apiHeaderValue) {
		String tokenData = "";
		URL url;
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(apiHeaderName, apiHeaderValue);

		Set set = params.entrySet();
		Iterator i = set.iterator();
		try {

			url = new URL(tokenURL);
			
			String userNameString = apiHeaderName;
			String passwordString = apiHeaderValue;
			
			String[] userNameArray = userNameString.split(":", 2);
			String userNameLabel = userNameArray[0];
			String userNameValue = userNameArray[1];
			
			String[] passwordArray = passwordString.split(":", 2);
			String passwordLabel = passwordArray[0];
			String passwordValue = passwordArray[1];
			
			String body = String.format(
				    "{\"username\":\"%s\",\"password\":\"%s\"}",
				    userNameValue, passwordValue
				);
			
			
//			byte[] postDataBytes = postData.toString().getBytes("UTF-8");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setDoOutput(true);
			


            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes("utf-8"));
                os.flush();
                os.close();
            }

            int responseCode = conn.getResponseCode();
//            System.out.println("Response Code: " + responseCode);

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            responseCode >= 200 && responseCode < 300
                                    ? conn.getInputStream()
                                    : conn.getErrorStream(),
                            "utf-8"
                    )
            );

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }

//            System.out.println("Response: " + response.toString());
			
            String tokenString = response.toString();   // HTTP/body response

            JSONObject root = new JSONObject(tokenString);

            // Debug: see what you really got
//            System.out.println("TRACKNOV response: " + root.toString());

            // Safe extraction supporting both shapes:
            // 1) {"result":1,"data":{"token":"MDUwMTA"},"message":""}
            // 2) {"result":1,"token":"MDUwMTA","message":""}
            String token = null;

            if (root.has("data") && root.opt("data") instanceof JSONObject) {
                token = root.getJSONObject("data").optString("token", null);
            } else if (root.has("token")) {
                token = root.optString("token", null);
            }

            tokenData = token;    // or return token
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error(e.getMessage());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return tokenData;
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
