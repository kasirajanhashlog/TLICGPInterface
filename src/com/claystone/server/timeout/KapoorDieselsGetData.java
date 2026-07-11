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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.claystone.common.utils.CommonConstants;
import com.claystone.db.CanbusVehicleParams;
import com.claystone.db.DataProvider;

import com.claystone.server.util.HibernateUtil;

import org.hibernate.Session;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONException;
import org.json.JSONObject;

public class KapoorDieselsGetData {
	private static Logger log;

	public KapoorDieselsGetData() {
		log = Logger.getLogger(KapoorDieselsGetData.class);
	}

	public ArrayList<GPSDataModel> GetDataKapoorDieselsFromAPI(String URL, String unitID,
			String timeformat, String tokenURL, String clientID,
			String clientSecret, String grantType,String llVehicleLicensePlate,int mileage,
			int dataProviderId,String dataProviderName,MobileUnitTimestamp mobileUnitTimestamp) {

		ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
		InputStream in = null;
		Reader r = null;
		HttpURLConnection http = null;
		HttpURLConnection http2 = null;
		int serverResponseCode;
		StringBuilder sb = null;
		InputStreamReader inReader;

//		String llToken = GetKapoorDieselsToken(tokenURL, clientID, clientSecret, grantType);
		String llToken = clientSecret;
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
//			serverResponseCode = 401;
			if(serverResponseCode == CommonConstants.UNAUTHORISED_ACCESS) {
				if (http != null) {
					http.disconnect();
					http = null;
				}
				URL url2 = new URL(URL);
				URLConnection con2 = url2.openConnection();
				http2 = (HttpURLConnection) con2;
				http2.setRequestMethod("GET"); // GET
				http2.setDoOutput(true);
				http2.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

				//Get the token from table and check with token in the memory(hashmap).If both are same then regenerate the token by calling
				//another api
				boolean isvalidToken = false;
				isvalidToken = CheckToken(clientSecret,dataProviderId,dataProviderName);
				if(isvalidToken == true) {
					String newToken = "";
					newToken = GetKapoorDieselsToken(tokenURL, clientID, clientSecret, grantType);
					if(newToken!=null || !newToken.equals("") && !clientSecret.equals(newToken) ) {
						http2.setRequestProperty("Authorization", "Bearer " + newToken);
						http2.connect();
					
						serverResponseCode = http2.getResponseCode();
							//Now update the newToken in hashmap and as well as in table
							if (mobileUnitTimestamp.getMobileUnitId().equals(unitID)) {
								mobileUnitTimestamp.setApiReqParams(newToken);
								UpdateToken(newToken,dataProviderId,dataProviderName);
						}
					}
				}
			}
			if(http!=null) {
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
			}
			if(http2!=null) {
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
					try {
						in = http2.getInputStream();
						inReader = new InputStreamReader(in);
					}catch (Exception e) {
						log.error("Error in news parsing" + e.toString());
						in = http2.getErrorStream();
						inReader = new InputStreamReader(in);
					}				
					BufferedReader reader1 = new BufferedReader(inReader);
					String line1;
					while ((line1 = reader1.readLine()) != null) {
						sb.append(line1);
					}
				}
			}

			String data = sb.toString();
			
			try {
				if (data != null && !data.isEmpty()) {
					JSONObject llTMData = new JSONObject(data);
					if(llTMData.has("vehicleName")&& llTMData.getString("vehicleName") != null)
					{
						log.info("Inside Kapoor Dieseld get data ");
						Double llLat = llTMData.getDouble("latitude");
						Double llLon = llTMData.getDouble("longitude");
						DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						String lDt = llTMData.getString("lastStatusTime");
						Date llGpSt = null;
						try {
							llGpSt = formatstr.parse(lDt);
						} catch (ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							log.error(e.getMessage());
						}
						boolean llAcc = false;
						int llSpeed = llTMData.getInt("speed");
						String ignitionStatus = llTMData.getString("status");
						if(ignitionStatus.equalsIgnoreCase("Ignition On")) {
							llAcc = true;
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
						model.setIgnition(ignitionStatus);
						if (llLat == 0 || llLon == 0) {
							model.setGpsstatus(false);
						}
						if (llTMData.has("totalOdometer")) {
							int lOdometer = llTMData.getInt("totalOdometer");
							if(lOdometer == 0 && mileage > 0)
							{
								lOdometer = mileage;
							}
							mileage = lOdometer;
							model.setMileage(lOdometer);
						}
						dataList.add(model);
					}
				}else {
					log.info("KAPOOR DIESELS dataList  not available for mobileunitId : " + unitID + " Time : "
							+ new Date() + " MobileUnitId: " + unitID);
					log.info("KAPOOR DIESELS dataList not available for  " + " MobileUnitId: " + unitID
							+ " Time : " + new Date());
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
		}finally {
			if (http != null) {
				http.disconnect();
			}
			if (http2 != null) {
				http2.disconnect();
			}
			in = null;
			inReader = null;
			http = null;
			http2 = null;
		}
		return dataList;

	}

	private String GetKapoorDieselsToken(String tokenURL, String clientID, String clientSecret, String grantType) {
		String data = "";
		String accessToken="";
		String userName="";
		String password="";
		String grant_type="";
		String auth_string="";
		if(clientID!=null && !clientID.equals("")){
			try {
				if (clientID != null && !clientID.isEmpty()) {
					JSONObject clientParam = new JSONObject(clientID);
					if(clientParam.has("username")&& clientParam.getString("username") != null) {
						userName = clientParam.getString("username");
					}
					if(clientParam.has("password")&& clientParam.getString("password") != null) {
						password = clientParam.getString("password");
					}
					if(clientParam.has("grant_type")&& clientParam.getString("grant_type") != null) {
						grant_type = clientParam.getString("grant_type");
					}
					if(clientParam.has("auth_string")&& clientParam.getString("auth_string") != null) {
						auth_string = clientParam.getString("auth_string");
					}
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.error(e.getMessage());
			}
	}
		
//		String json = "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\nContent-Disposition: form-data; name=\"username\"\r\n\r\ndishant.kalra+Aetos-Zing@fleetx.io\r\n------WebKitFormBoundary7MA4YWxkTrZu0gW\r\nContent-Disposition: form-data; name=\"password\"\r\n\r\nYffcZD6s\r\n------WebKitFormBoundary7MA4YWxkTrZu0gW\r\nContent-Disposition: form-data; name=\"grant_type\"\r\n\r\npassword\r\n------WebKitFormBoundary7MA4YWxkTrZu0gW--";
		String json = "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\nContent-Disposition: form-data; name=\"username\"\r\n\r\n"+userName+"\r\n------WebKitFormBoundary7MA4YWxkTrZu0gW\r\nContent-Disposition: form-data; name=\"password\"\r\n\r\n"+password+"\r\n------WebKitFormBoundary7MA4YWxkTrZu0gW\r\nContent-Disposition: form-data; name=\"grant_type\"\r\n\r\n"+grant_type+"\r\n------WebKitFormBoundary7MA4YWxkTrZu0gW--";

		HttpsURLConnection connection = null;
		int serverResponseCode;
		StringBuilder sb = null;
		InputStream in;
		InputStreamReader inReader;
		try {
			// configure the SSLContext with a TrustManager
			SSLContext ctx = SSLContext.getInstance("TLS");
			ctx.init(new KeyManager[0], new TrustManager[] { new DefaultTrustManager() }, new SecureRandom());
			SSLContext.setDefault(ctx);
//			URL url = new URL("https://api.fleetx.io/api/v1/login");
			URL url = new URL(tokenURL);
			connection = (HttpsURLConnection) url.openConnection();
			connection.setHostnameVerifier(new HostnameVerifier() {
				@Override
				public boolean verify(String arg0, SSLSession arg1) {
					return true;
				}
			});
			connection.setDoOutput(true);
			connection.setRequestProperty("content-type", "multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW");
//			connection.setRequestProperty("Authorization", "Basic ZmxlZXR4OnNlY3JldA==");
			connection.setRequestProperty("Authorization", "Basic "+auth_string);
			OutputStream os = connection.getOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
			String objData = json;
			osw.write(objData);
			osw.flush();
			osw.close();
			os.close(); // don't forget to close the OutputStream
			connection.connect();
			serverResponseCode = connection.getResponseCode();

			switch (serverResponseCode) {
			case HttpsURLConnection.HTTP_OK: {
				sb = new StringBuilder();
				in = connection.getInputStream();
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
				in = connection.getErrorStream();
				inReader = new InputStreamReader(in);
				BufferedReader reader1 = new BufferedReader(inReader);
				String line1;
				while ((line1 = reader1.readLine()) != null) {
					sb.append(line1);
				}
			}
				data = sb.toString(); 
				try {
					if (data != null && !data.isEmpty()) {
						JSONObject lJData = new JSONObject(data);
						if(lJData.has("access_token")&& lJData.getString("access_token") != null) {
							accessToken = lJData.getString("access_token");
						}
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					log.error(e.getMessage());
				}
		} catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error(e.getMessage());
		}finally {
			if (connection != null) {
				connection.disconnect();
			}
			in = null;
			inReader = null;
			connection = null;
		}

		return accessToken;
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
	public static class DefaultTrustManager implements X509TrustManager {

		@Override
		public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	}
}
