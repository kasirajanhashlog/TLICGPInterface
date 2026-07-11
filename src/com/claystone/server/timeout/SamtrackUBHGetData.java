package com.claystone.server.timeout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

import com.claystone.common.utils.CommonConstants;
import com.claystone.common.utils.CommonMethods;
import com.claystone.db.DataProvider;
import com.claystone.server.util.HibernateUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SamtrackUBHGetData {
	private static Logger log;

	public SamtrackUBHGetData() {
		log = Logger.getLogger(SamtrackUBHGetData.class);
	}
	public ArrayList<GPSDataModel> GetDataSamtrackFromAPI(String URL, String companyID, HashMap<String, String> columnList,
			String timeformat,  String tokenURL, String clientID,
			String clientSecret, String grantType,String dataProviderName) throws JSONException, org.json.simple.parser.ParseException, ParseException {

		ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
		InputStream in = null;
		Reader r = null;
		HttpURLConnection http2 = null;
		int serverResponseCode;
		StringBuilder sb = null;
		InputStreamReader inReader;
		String llToken = null;
		String userName = null;
		String passwordStr = null;
		JSONArray array = new JSONArray('['+clientSecret+']');  
		for(int i=0; i < array.length(); i++)   
		{  
		JSONObject object = array.getJSONObject(i);  
//		log.info(object.getString("username"));  
		userName = object.getString("username");
//		log.info(object.getString("password"));  
		passwordStr= object.getString("password");
		}  
		 String userpass = userName + ":" + passwordStr;  
//		 llToken =  new String(Base64.getEncoder().encode(userpass.getBytes()));  
		 
			String lAccessToken = GetSAMTRACKUBHAccessToken(tokenURL,userName,passwordStr);
	        String json =lAccessToken;
	        JSONObject obj = new JSONObject(json);
	        String accessToken = obj.getString("access_token");

	        System.out.println("Access Token: " + accessToken);
	        llToken = accessToken;
		 
		try {
			Session session = HibernateUtil.beginTransaction();
			List llVehList = session.createSQLQuery(" select distinct v.license_plate,dp.data_provider_name,m.mobileunitid "
					+ " from vehicle v," + " vehicle_mobile_unit vm ," + " mobile_unit m," + " data_provider dp"
					+ " where" + " v.company_id = " + companyID + " and v.record_status = "
					+ CommonConstants.RECORD_STATUS_ACTIVE + " and vm.vehicle_id = v.vehicle_id"
					+ " and vm.is_active = true " + " and vm.last_effective_date >= 'now()' "
					+ " and m.mobileunitid = vm.mobileunitid " + " and dp.data_provider_id = m.data_provider_id"
					+ " and dp.data_provider_name = 'SAMTRACK_UBH' " + " order by v.license_plate ").list();

			HibernateUtil.commit();
			String vehicleStr = "";
			if (llVehList.size() > 0) {
				for (int llInd = 0; llInd < llVehList.size(); llInd++) {
					Object[] objVeh = (Object[]) llVehList.get(llInd);
					if ((String) objVeh[0] != null) {
						if(vehicleStr.equals("")) {
							vehicleStr=(String) objVeh[0];
						}else {
							vehicleStr+=","+(String) objVeh[0];
						}
					}
				}
			}
//			System.out.println("vehicleStr:"+vehicleStr);
			
//			String urlParameters = "vehicles=NL02Q3641,NL01AA3852,NL01AJ6454";
			String urlParameters = "vehicles="+vehicleStr;
            byte[] input = urlParameters.getBytes(StandardCharsets.UTF_8);
		
			URL url = new URL(URL);
			URLConnection con = url.openConnection();
			http2 = (HttpURLConnection) con;
			http2.setRequestMethod("POST"); // GET
			http2.setDoOutput(true);
			http2.setRequestProperty("Authorization",llToken);
			http2.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			http2.setDoOutput(true);
			
		       try (OutputStream osw = con.getOutputStream()) {
		    	   osw.write(input, 0, input.length);
		    	   osw.flush();
	                http2.connect();
	            }
			serverResponseCode = http2.getResponseCode();

			switch (serverResponseCode) {
			case HttpsURLConnection.HTTP_OK: {
				sb = new StringBuilder();
				in = http2.getInputStream();
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
				in = http2.getErrorStream();
				inReader = new InputStreamReader(in);
				BufferedReader reader1 = new BufferedReader(inReader);
				String line1;
				while ((line1 = reader1.readLine()) != null) {
					sb.append(line1);
				}
			}

			String result = sb.toString();
//			System.out.println("serverResponseCode: "+serverResponseCode+";result: "+result);

			JSONObject root = new JSONObject(result);
			if (root.has("locationDetails")) {
	            JSONObject locationDetails = root.getJSONObject("locationDetails");
	            if (locationDetails.has("result")) {
	                JSONArray results = locationDetails.getJSONArray("result");
	                for (int i = 0; i < results.length(); i++) {
	                    JSONObject vehicleObj = results.getJSONObject(i);

	                    // Use optString/optDouble to avoid exceptions if missing
	                    String angle = vehicleObj.optString("Angle", "N/A");
	                    String ignition = vehicleObj.optString("Ignition", "N/A");
	                    String imei = vehicleObj.optString("Imei", "N/A");
	                    String lastUpdate = vehicleObj.optString("LastUpdate", "N/A");
	                    String lat = vehicleObj.optString("Lat", "N/A");
	                    String location = vehicleObj.optString("Location", "N/A");
	                    String longitude = vehicleObj.optString("Long", "N/A");
	                    int speed = vehicleObj.optInt("Speed", 0);
	                    String tempr = vehicleObj.optString("Tempr", "N/A");
	                    String licensePlate = vehicleObj.optString("Vehicle", "N/A");
	                    
	                    Double llLat = Double.parseDouble(lat);
						Double llLon = Double.parseDouble(longitude);
						boolean llAcc = false;
						if(ignition.equalsIgnoreCase("ON")){
							llAcc = true;
						}
						if (speed > 0) {
							llAcc = true;
						}
						DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						String lDt = lastUpdate;
						Date llGpSt = null;
						try {
							llGpSt = formatstr.parse(lDt);
						} catch (ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							log.error(e.getMessage());
						}
						GPSDataModel model = new GPSDataModel();
						model.setLicensePlate(licensePlate);
						model.setLatitude(llLat);
						model.setLongitude(llLon);
						model.setGpsdate(llGpSt);
						model.setSpeed((short) speed);
						model.setAcc(llAcc);
						for(int vCount = 0;vCount < llVehList.size();vCount++) {
							Object[] vehObj = (Object[]) llVehList.get(vCount);
							String licPlate = (String)vehObj[0];
							String mobUnit = (String)vehObj[2];
							if(licPlate.equalsIgnoreCase(licensePlate)) {
								model.setMobileunitid(mobUnit);
								break;
							}
						}
						dataList.add(model);
	                    
	                }
	            }else {
	            	dataList = new ArrayList<GPSDataModel>();
	            }
			}else {
				dataList = new ArrayList<GPSDataModel>();
			}
//			log.info("Samtrack_UBH-RawData" + " Response result : " + result + " Time : " + new Date());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(e.getMessage());
			e.printStackTrace();
		}finally {

			if (http2 != null) {
				http2.disconnect();
			}
			in = null;
			inReader = null;

			http2 = null;
		}
		return dataList;
		}
	private static String GetSAMTRACKUBHAccessToken(String tokenURL, String userName,String password) {
		StringBuilder response = new StringBuilder();
		String data = "";
		URL url;
		try {
			
			//In postman,it is working but in code it is not.It is giving following error
			//"javax.net.ssl.SSLHandshakeException: java.security.cert.CertificateException: No subject alternative DNS name matching sandbox-gps.anstracknology.com found"
			// Disable hostname verification globally (NOT safe for production!)
	        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
	            public boolean verify(String hostname, SSLSession session) {
	                return true;
	            }});
	        
            // Create Base64 encoded authorization header
            String auth = userName + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes("UTF-8"));
            String authHeader = "Basic " + encodedAuth;
			url = new URL(tokenURL);
			HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            // Setup request
            con.setRequestMethod("POST");
            con.setRequestProperty("Authorization", authHeader);
            con.setDoOutput(true);
            // Response code
            int responseCode = con.getResponseCode();
            System.out.println("Response Code : " + responseCode);
            // Read response body
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            (responseCode == 200) ? con.getInputStream() : con.getErrorStream()
                    )
            );
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            // Print result
            System.out.println("Response Body: " + response.toString());


		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error(e.getMessage());
		}
		return response.toString();
	}
	}