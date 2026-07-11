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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
import com.claystone.db.CanbusGpsdata;
import com.claystone.db.CanbusGpsdataId;
import com.claystone.db.CanbusVehicleParams;
import com.claystone.server.util.HibernateUtil;

public class EicherGetData {
	private Logger log;

	public EicherGetData() {
		log = Logger.getLogger(EicherGetData.class);
	}

	public ArrayList<GPSDataModel> GetDataFromEICHER(String URL, String lClientID, String lClientSecretID,
			String llStartDateTime, String llEndDateTime, String mobileunitId,
			boolean lCanBusDataAvailable, ArrayList<CanbusVehicleParams> lCanbusParamList,
			ArrayList<CanBusModel> lBusModelList, int mileage) {

		ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
		InputStream in = null;
		Reader r = null;
		HttpURLConnection http = null;
		int serverResponseCode;
		StringBuilder sb = null;
		InputStreamReader inReader;
		SimpleDateFormat datetimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		try {

			JSONArray llJSArr = new JSONArray();

			llJSArr.put(mobileunitId);

			JSONObject llCredVal = new JSONObject();
			llCredVal.put("chassisno", llJSArr);
			llCredVal.put("startDateTime", llStartDateTime);
			llCredVal.put("endDateTime", llEndDateTime);

			URL url = new URL(URL);
			URLConnection con = url.openConnection();
			http = (HttpURLConnection) con;
			http.setRequestMethod("POST"); // POST
			http.setDoOutput(true);
			http.setRequestProperty("X-ibm-client-id", lClientID);
			http.setRequestProperty("X-ibm-client-secret", lClientSecretID);
			http.setRequestProperty("Content-Type", "application/json");
			http.setRequestProperty("user-agent", "Application");//Added on 09AUG2023--EICHER's new api is not working from web but postman..so this is added.
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
			JSONArray jo = new JSONArray(data);
			JSONObject llVehDet = jo.getJSONObject(0);
			String llChassisNo = llVehDet.getString("chassisNo");
			log.info("EICHER Response :- " + llChassisNo);
			if (llVehDet.has("positiondata")) {
				if (llChassisNo.equals(mobileunitId)) {
					JSONArray details = llVehDet.getJSONArray("positiondata");

					for (int i = 0; i < details.length(); i++) {
						JSONObject llData = details.getJSONObject(i);
						boolean llAcc = false;
						String llVehicleLicensePlate = llData.getString("vehicleRegistrationNumber");
						Double llLat = llData.getDouble("lattitude");
						Double llLon = llData.getDouble("longitude");						
						long llTime = llData.getLong("positionDateTime");
						String lTT = String.valueOf(llTime);
						if (lTT.length() > 13 && lTT.length() < 15) {
							String lT1 = lTT.substring(0, 4);
							String lT2 = lTT.substring(4, 6);
							String lT3 = lTT.substring(6, 8);
							String lT4 = lTT.substring(8, 10);
							String lT5 = lTT.substring(10, 12);
							String lT6 = lTT.substring(12, 14);
							lTT = lT1 + "-" + lT2 + "-" + lT3 + " " + lT4 + ":" + lT5 + ":" + lT6;
						} else {
							continue;
						}
						Date llGpsDt = datetimeFormatter.parse(lTT);
						if (llData.has("ignitionStatus")) {
							int lIgnition = llData.getInt("ignitionStatus");
							if (lIgnition == 1) {
								llAcc = true;
							} else if (lIgnition == 0) {
								llAcc = false;
							}
						}
						int llSpeed = llData.getInt("speed");
						if (llSpeed > 0) {
							llAcc = true;
						} else {
							llAcc = false;
						}
						int llDirection = llData.getInt("heading");

						GPSDataModel model = new GPSDataModel();
						model.setLicensePlate(llVehicleLicensePlate);
						model.setLatitude(llLat);
						model.setLongitude(llLon);
						model.setGpsdate(llGpsDt);
						model.setSpeed((short) llSpeed);
						model.setAcc(llAcc);
						model.setDirection((short) llDirection);
						model.setMobileunitid(mobileunitId);
						
						if (llLat == 0 || llLon == 0) {
							model.setGpsstatus(false);
						}

						if (llData.has("odometer")) {
							int lOdometer = llData.getInt("odometer");
							if(lOdometer == 0 && mileage > 0)
							{
								lOdometer = mileage;
							}
							mileage = lOdometer;
							model.setMileage(lOdometer);
						}

						model.setCanBusDataAvailable(lCanBusDataAvailable);
						ArrayList<CanBusModel> lbusModel = new ArrayList<CanBusModel>();
						log.info("EICHER Response :- " + llData);
						if (lCanBusDataAvailable && lCanbusParamList.size() > 0) {
							for (int l = 0; l < lCanbusParamList.size(); l++) {
								CanBusModel lBModel = new CanBusModel();
								CanbusVehicleParams lParams = (CanbusVehicleParams) lCanbusParamList.get(l);
								if (!llData.isNull(lParams.getId().getCanVarId())) {
									Double llValue = llData.getDouble(lParams.getId().getCanVarId());
									lBModel.setCanVarId(lParams.getId().getCanVarId());
									lBModel.setZtVarId(lParams.getId().getZtVarId());
									lBModel.setParamOutputValue(llValue.toString());
									for (int lBDt = 0; lBDt < lBusModelList.size(); lBDt++) {
										CanBusModel lModel = lBusModelList.get(lBDt);
										if (lModel.getZtVarId().equals(lParams.getId().getZtVarId())) {
											lModel.setParamOutputValue(llValue.toString());
											lBusModelList.remove(lBDt);
											lBusModelList.add(lModel);
											break;
										}
									}
									lbusModel.add(lBModel);
								} else {
									lBModel.setCanVarId(lParams.getId().getCanVarId());
									lBModel.setZtVarId(lParams.getId().getZtVarId());
									for (int lBDt = 0; lBDt < lBusModelList.size(); lBDt++) {
										CanBusModel lModel = lBusModelList.get(lBDt);
										if (lModel.getZtVarId().equals(lParams.getId().getZtVarId())) {
											lBModel.setParamOutputValue(lModel.getParamOutputValue());
											break;
										}
									}

									lbusModel.add(lBModel);
								}

							}

						}
						model.setlCanModelList(lbusModel);
						dataList.add(model);

					}
				}
			} else {
				log.info("EICHER dataList Position data not available for mobileunitId : " + mobileunitId + " Time : "
						+ new Date() + " Chassis No: " + mobileunitId);
				log.info("EICHER dataList Position data not available for  " + " Chassis No: " + mobileunitId
						+ " Time : " + new Date());
			}

			log.info("EICHER dataList : " + dataList.size() + " Time : " + new Date() + " Chassis No: " + mobileunitId);

		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
		return dataList;

	}

}
