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
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import com.claystone.common.utils.CommonConstants;
import com.claystone.server.util.HibernateUtil;

public class SAMRXGetData {
	private Logger log;
	private Properties properties = new Properties();
	private String lSAMRXLoginURL = "";
	private String lSAMRXClientKey = "";
	private String lSAMRXVehicleURL = "";

	public SAMRXGetData() {
		log = Logger.getLogger(SAMRXGetData.class);
		try {
			properties.load(new FileInputStream("server.properties"));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			log.error("Exception", e1);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			log.error("Exception", e1);
		}
		lSAMRXLoginURL = properties.getProperty("SAMRXTokenURL");
		lSAMRXClientKey = properties.getProperty("SAMRXClientKey");
		lSAMRXVehicleURL = properties.getProperty("SAMRXVehicleURL");

	}

	public ArrayList<GPSDataModel> GetDataFromSAMRX(String URL, String companyID, HashMap<String, String> columnList,
			String timeformat, String dataProviderName) {

		ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
		InputStream in = null;
		Reader r = null;
		HttpURLConnection http = null;
		int serverResponseCode;
		StringBuilder sb = null;
		InputStreamReader inReader;

		try {

			JSONArray llJSArr = new JSONArray();
			String lToken = GetSAMRXToken();
			Session session = HibernateUtil.beginTransaction();
			List llVehList = session.createSQLQuery(" select distinct v.license_plate,dp.data_provider_name "
					+ " from vehicle v," + " vehicle_mobile_unit vm ," + " mobile_unit m," + " data_provider dp"
					+ " where" + " v.company_id = " + companyID + " and v.record_status = "
					+ CommonConstants.RECORD_STATUS_ACTIVE + " and vm.vehicle_id = v.vehicle_id"
					+ " and vm.is_active = true " + " and vm.last_effective_date >= 'now()' "
					+ " and m.mobileunitid = vm.mobileunitid " + " and dp.data_provider_id = m.data_provider_id"
					+ " and dp.data_provider_name = 'SAMRX' " + " order by v.license_plate ").list();

			HibernateUtil.commit();
			if (llVehList.size() > 0) {
				for (int llInd = 0; llInd < llVehList.size(); llInd++) {
					Object[] obj = (Object[]) llVehList.get(llInd);
					if ((String) obj[0] != null) {
						llJSArr.put((String) obj[0]);
					}
				}
			}

		//	llJSArr.put("NL01AE6311");
		//	llJSArr.put("NL01AE6319");
			JSONObject llCredVal = new JSONObject();
			llCredVal.put("vehicleNameList", llJSArr);
			llCredVal.put("isName", true);
			String llTokVal = "Bearer " + lToken;

			URL url = new URL(URL);
			URLConnection con = url.openConnection();
			http = (HttpURLConnection) con;
			http.setRequestMethod("POST"); // POST
			http.setDoOutput(true);
			http.setRequestProperty("Authorization", llTokVal);
			http.setRequestProperty("Content-Type", "application/json");
			OutputStream osw = http.getOutputStream();
			osw.write(llCredVal.toString().getBytes());
			osw.flush();
			osw.close();
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
			String llStatus = jo.getString("status");
			log.info("Status Result from SAMRX Response :- " + llStatus);
			if (llStatus.equals("SUCCESS")) {
				JSONObject llPayLd = jo.getJSONObject("payLoad");
				JSONArray details = llPayLd.getJSONArray("realTimeData");

				for (int i = 0; i < details.length(); i++) {
					JSONObject llData = details.getJSONObject(i);
					boolean llAcc = false;
					String llVehicleLicensePlate = llData.getString("vehicleNo");
					Double llLat = llData.getDouble("latitude");
					Double llLon = llData.getDouble("longitude");
					long llTime = llData.getLong("serverTime");
					Date llGpsDt = new Date(llTime);
					int llSpeed = llData.getInt("speed");
					//On 16SEP2023...SAMRX confirmed that the speed value we are receiving is Km/Sec.So we have to multiply it by 3.6
					//to get actual speed.
					llSpeed = (int) (llSpeed * 3.6);
					
					if (llSpeed > 0) {
						llAcc = true;
					}
					int llDirection = llData.getInt("direction");
					GPSDataModel model = new GPSDataModel();
					model.setLicensePlate(llVehicleLicensePlate);
					model.setLatitude(llLat);
					model.setLongitude(llLon);
					model.setGpsdate(llGpsDt);
					model.setSpeed((short) llSpeed);
					model.setAcc(llAcc);
					model.setDirection((short) llDirection);
					dataList.add(model);

				}
			}

			log.info(companyID + " dataList : " + dataList.size() + " Time : " + new Date());

		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
		return dataList;

	}

	private String GetSAMRXToken() {
		String data = "";
		InputStream in = null;
		Reader r = null;
		GetMethod request = null;
		HttpClient httpclient = null;
		try {

			request = new GetMethod(lSAMRXLoginURL);
			request.setRequestHeader("authToken", lSAMRXClientKey);
			request.setRequestHeader("Content-Type", "application/json; charset=utf-8");
			request.setRequestHeader("cache-control", "no-cache");

			httpclient = new HttpClient();
			String msg = "";
			String lStatus = "";
			int lLgCode = -1;

			lLgCode = httpclient.executeMethod(request);

			if (lLgCode == 200) {
				msg = request.getResponseBodyAsString();

				JSONObject jsonObject = new JSONObject(msg);
				JSONObject c = jsonObject.getJSONObject("payLoad");
				data = c.getString("authToken");

			}

		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		} finally {
			// Release current connection to the connection pool once you are done
			request.releaseConnection();
			((SimpleHttpConnectionManager) httpclient.getHttpConnectionManager()).shutdown();
		}

		return data;

	}
}
