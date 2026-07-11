package com.claystone.server.timeout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class TataMotorsGetData {
	private Logger log;

	public TataMotorsGetData() {
		log = Logger.getLogger(TataMotorsGetData.class);
	}

	public ArrayList<GPSDataModel> GetDataTataMotorsFromAPI(String URL, String unitID,
			HashMap<String, String> columnList, String timeformat, String tokenURL, String clientID,
			String clientSecret, String grantType, String llVehicleLicensePlate) {

		ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
		InputStream in = null;
		Reader r = null;
		HttpURLConnection http = null;
		int serverResponseCode;
		StringBuilder sb = null;
		InputStreamReader inReader;

		String llToken = GetTataMotorToken(tokenURL, clientID, clientSecret, grantType);
		try {
			URL url = new URL(URL);
			URLConnection con = url.openConnection();
			http = (HttpURLConnection) con;
			http.setRequestMethod("GET"); // GET
			http.setDoOutput(true);
			http.setRequestProperty("Authorization", "Bearer " + llToken);
			http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
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
				try {
					in = http.getInputStream();
					inReader = new InputStreamReader(in);
				}catch (Exception e) {
					log.error("Error in news parsing" + e.toString());
					in = http.getErrorStream();
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
					JSONObject llTMData = new JSONObject(data);
					if(llTMData.has("vehicleId")&& llTMData.getString("vehicleId") != null)
					{
						log.info("Inside Tata Motors get data ");
						Double llLat = llTMData.getDouble("gpsLatitude");
						Double llLon = llTMData.getDouble("gpsLongitude");
						DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
						String lDt = llTMData.getString("eventDateTime");
						Date llGpSt = null;
						try {
							llGpSt = formatter.parse(lDt);
	//					 Calendar cal = Calendar.getInstance();
	//					 cal.setTime(llGpSt);
	//					 cal.add(Calendar.HOUR_OF_DAY, 5);
	//					 cal.add(Calendar.MINUTE, 30);
	//					 llGpSt = cal.getTime();
						} catch (ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							log.error(e.getMessage());
						}
						boolean llAcc = false;
						int llSpeed = llTMData.getInt("speed");
						int llMileage = llTMData.getInt("odometer");
						llAcc = llTMData.getBoolean("ignitionOn");
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
						model.setMileage(llMileage);
						dataList.add(model);
					}
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.error(e.getMessage());
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error(e.getMessage());
		}
		return dataList;

	}

	private String GetTataMotorToken(String tokenURL, String clientID, String clientSecret, String grantType) {
		String data = "";
		URL url;
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("client_id", clientID);
		params.put("client_secret", clientSecret);
		params.put("grant_type", grantType);
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
				log.info("Tata Motors token returned");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return data;
	}
}
