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
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;

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

public class ATICGetData {
	private static Logger log;

	public ATICGetData() {
		log = Logger.getLogger(ATICGetData.class);
	}
	public ArrayList<GPSDataModel> GetDataATICMzoneFromAPI(String URL, String unitID,
			HashMap<String, String> columnList, String timeformat, String tokenURL, String clientID,
			String clientSecret, String grantType,String llVehicleLicensePlate, String userName,String password,
			int dataProviderId,String dataProviderName,MobileUnitTimestamp mobileUnitTimestamp) {

		ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
		InputStream in = null;
		Reader r = null;
		HttpsURLConnection https = null;
		HttpURLConnection https2 = null;
		int serverResponseCode;
		StringBuilder sb = null;
		InputStreamReader inReader;

//		String llToken = GetATICAccessToken(tokenURL, clientID, clientSecret, grantType,userName,password);
		String llToken = "";
		if (mobileUnitTimestamp.getMobileUnitId().equals(unitID)) {
			llToken = mobileUnitTimestamp.getAccessToken();
		}
//		String llToken = apiReqParam;
		try {
			URL url = new URL(URL);
			URLConnection con = url.openConnection();
			https = (HttpsURLConnection) con;
			https.setRequestMethod("GET"); // GET
			https.setDoOutput(true);
			https.setRequestProperty("Authorization", "Bearer " + llToken);
			https.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
			https.connect();
			serverResponseCode = https.getResponseCode();
			if(serverResponseCode == CommonConstants.UNAUTHORISED_ACCESS) {
				if (https2 != null) {
					https2.disconnect();
					https2 = null;
				}
				URL url2 = new URL(tokenURL);
				URLConnection con2 = url2.openConnection();
				https2 = (HttpURLConnection) con2;
				https2.setRequestMethod("POST"); // GET
				https2.setDoOutput(true);
				https2.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
				//Get the token from table and check with token in the memory(hashmap).If both are same then regenerate the token by calling
				//another api
				boolean isvalidToken = false;
//				isvalidToken = CheckToken(clientSecret,dataProviderId,dataProviderName);
				isvalidToken = CheckToken(llToken,dataProviderId,dataProviderName);
				log.info("unitID"+unitID);
				log.info("isvalidToken:"+isvalidToken);
				if(isvalidToken == false) {
					String newToken = "";
					newToken = GetATICAccessToken(tokenURL, clientID, clientSecret, grantType,userName,password);
					if(newToken!=null || !newToken.equals("") && !clientSecret.equals(newToken) ) {
						if(https!=null) {
							https.disconnect();
							https = null;
						}
						URL url3 = new URL(URL);
						URLConnection con3 = url3.openConnection();
						https = (HttpsURLConnection) con3;
						https.setRequestMethod("GET"); // GET
						https.setDoOutput(true);
						https.setRequestProperty("Authorization", "Bearer " + newToken);
						https.connect();
						serverResponseCode = https.getResponseCode();
							//Now update the newToken in hashmap and as well as in table
							if (mobileUnitTimestamp.getMobileUnitId().equals(unitID)) {
//								mobileUnitTimestamp.setApiReqParams(newToken);
								mobileUnitTimestamp.setAccessToken(newToken);
								UpdateToken(newToken,dataProviderId,dataProviderName);
						}
					}
				}
			}
			switch (serverResponseCode) {
			case HttpsURLConnection.HTTP_OK: {
				sb = new StringBuilder();
				in = https.getInputStream();
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
				try {
					in = https.getInputStream();
					inReader = new InputStreamReader(in);
				}catch (Exception e) {
					log.error("Error in news parsing" + e.toString());
					in = https.getErrorStream();
					inReader = new InputStreamReader(in);
				}				
				BufferedReader reader1 = new BufferedReader(inReader);
				String line1;
				while ((line1 = reader1.readLine()) != null) {
					sb.append(line1);
				}
			}
			String data = sb.toString();
			
			try {
				if (data != null && !data.isEmpty()) {
					JSONObject lAMZData = new JSONObject(data);
					if(lAMZData.has("vehicle_Id")&& lAMZData.getString("vehicle_Id") != null)
					{
						log.info("Inside ATIC MZone get data ");
						Double llLat = lAMZData.getDouble("latitude");
						Double llLon = lAMZData.getDouble("longitude");
//						int latitude = (int) ((llLat) * Double.valueOf(1000000.00)); 
//						int longitude = (int) ((llLon) * Double.valueOf(1000000.00));
						DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
						
						
						String lDt = lAMZData.getString("utcTimestamp");
						SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss");
						// input is in UTC
						sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
						Date utcTimeStamp = sdf.parse(lDt);
						
						// another formatter for output
						SimpleDateFormat outputFormat= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						outputFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
						outputFormat.format(utcTimeStamp);
						log.info("outputFormat:- "+outputFormat.format(utcTimeStamp));
						Date llGpSt=formatstr.parse(outputFormat.format(utcTimeStamp));  

						boolean llAcc = false;
						int llSpeed = lAMZData.getInt("speed");
						llAcc = lAMZData.getBoolean("ignitionOn");
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
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.error(e.getMessage());
			} catch (ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				log.error(e1.getMessage());
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error(e.getMessage());
		}
		return dataList;

	}
	private static String GetATICAccessToken(String tokenURL, String clientID, String clientSecret, String grantType,
			String userName,String password) {
		String data = "";
		URL url;
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("client_id", clientID);
		params.put("client_secret", clientSecret);
		params.put("grant_type", grantType);
		params.put("username", userName);
		params.put("password",password);
		Set set = params.entrySet();
		Iterator i = set.iterator();
		try {
			StringBuilder postData = new StringBuilder();
			for (Entry<String, String> param : params.entrySet()) {
				if (postData.length() != 0) {
					postData.append('&');
				}
				postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
				postData.append('=');
				postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
			}
			url = new URL(tokenURL);
			byte[] postDataBytes = postData.toString().getBytes("UTF-8");
			HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
			conn.setDoOutput(true);
			conn.getOutputStream().write(postDataBytes);
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			StringBuilder builder = new StringBuilder();
			for (String line = null; (line = reader.readLine()) != null;) {
				builder.append(line).append("\n");
			}
			reader.close();
			conn.disconnect();
			try {
				JSONObject llTataToken = new JSONObject(builder.toString());
				data = llTataToken.getString("access_token");
//				log.info("Tata Motors token returned");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				log.error(e.getMessage());
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
			e.printStackTrace();
			log.error(e.getMessage());
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
}
