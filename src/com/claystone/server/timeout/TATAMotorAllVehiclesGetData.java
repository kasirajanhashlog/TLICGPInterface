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
import java.net.URLEncoder;
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

public class TATAMotorAllVehiclesGetData {
	private static Logger log;
	private Properties properties = new Properties();
	private int vehicleLimit = 1;
	private String vehicleLimitStr = "1";
//	private String lSAMRXVehicleURL = "";

	public TATAMotorAllVehiclesGetData() {
		log = Logger.getLogger(TATAMotorAllVehiclesGetData.class);
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

//	public ArrayList<GPSDataModel> GetDataFromTATAMotors(String URL, String companyID, HashMap<String, String> columnList,
//			String timeformat,String tokenURL,String clientId,String clientSecret,int dataProviderId, String dataProviderName,
//			MobileUnitTimestamp companyTimestampHash) {
	public ArrayList<GPSDataModel> GetDataFromTATAMotors(String URL, String companyID, HashMap<String, String> columnList,
			String timeformat,String tokenURL,String clientId,String accessToken,int dataProviderId, String dataProviderName,
			MobileUnitTimestamp companyTimestampHash) {

		ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
		InputStream in = null;
		Reader r = null;
		HttpURLConnection http = null;
		HttpURLConnection http2 = null;
		int serverResponseCode;
		StringBuilder sb = null;
		InputStreamReader inReader;
//		String llToken = clientSecret;
		String llToken = accessToken;
		String clientID = "";
		String grantType = "";
		String clientSecret="";
		
		if(clientId!=null && !clientId.equals("")){
			try {
				if (clientId != null && !clientId.isEmpty()) {
					JSONObject clientParam = new JSONObject(clientId);
					if(clientParam.has("client_id")&& clientParam.getString("client_id") != null) {
						clientID = clientParam.getString("client_id");
					}
					if(clientParam.has("client_secret")&& clientParam.getString("client_secret") != null) {
						clientSecret = clientParam.getString("client_secret");
					}
					if(clientParam.has("grant_type")&& clientParam.getString("grant_type") != null) {
						grantType = clientParam.getString("grant_type");
					}
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				log.error(e.getMessage());
				e.printStackTrace();
			}
	}
		try {

			JSONArray llJSArr = new JSONArray();

			Session session = HibernateUtil.beginTransaction();
			List llVehList = session.createSQLQuery(" select distinct vm.mobileunitid,dp.data_provider_name "
					+ " from vehicle v," + " vehicle_mobile_unit vm ," + " mobile_unit m," + " data_provider dp"
					+ " where" + " v.company_id = " + companyID + " and v.record_status = "
					+ CommonConstants.RECORD_STATUS_ACTIVE + " and vm.vehicle_id = v.vehicle_id"
					+ " and vm.is_active = true " + " and vm.last_effective_date >= 'now()' "
					+ " and m.mobileunitid = vm.mobileunitid " + " and dp.data_provider_id = m.data_provider_id"
//					+ " and dp.data_provider_name = 'TATAMOTOR-1' " + " order by vm.mobileunitid ").list();
					+ " and dp.data_provider_name = '"+dataProviderName+"'" + " order by vm.mobileunitid ").list();

			HibernateUtil.commit();
			
			
			if(llVehList.size()!= 0 && vehicleLimit > llVehList.size()) {
				if (llVehList.size() > 0) {
					for (int llInd = 0; llInd < llVehList.size(); llInd++) {
						Object[] obj = (Object[]) llVehList.get(llInd);
						if ((String) obj[0] != null) {
							llJSArr.put((String) obj[0]);
						}
					}
				}

				JSONObject llCredVal = new JSONObject();
				llCredVal.put("chassisNumbers", llJSArr);
				URL url = new URL(URL);
				URLConnection con = url.openConnection();
				http = (HttpURLConnection) con;
				http.setRequestMethod("POST"); // GET
				http.setDoOutput(true);
				http.setRequestProperty("Authorization", "Bearer " + llToken);
				http.setRequestProperty("Content-Type", "application/json");
				OutputStream osw = http.getOutputStream();
				osw.write(llCredVal.toString().getBytes());
				osw.flush();
				osw.close();
				http.connect();
				serverResponseCode = http.getResponseCode();
				if(serverResponseCode == CommonConstants.UNAUTHORISED_ACCESS) {
					if (http != null) {
						http.disconnect();
						http = null;
					}
					URL url2 = new URL(URL);
					URLConnection con2 = url2.openConnection();
					http2 = (HttpURLConnection) con2;
					http2.setRequestMethod("POST"); // GET
					http2.setDoOutput(true);
					http2.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

					//Get the token from table and check with token in the memory(hashmap).If both are same then regenerate the token by calling
					//another api
					boolean isvalidToken = false;
//					isvalidToken = CheckToken(clientSecret,dataProviderId,dataProviderName);
					isvalidToken = CheckToken(accessToken,dataProviderId,dataProviderName);
//					if(isvalidToken == false) {
						String newToken = "";
						newToken = GetTataMotorToken(tokenURL, clientID, clientSecret, grantType);
//						if(newToken!=null || !newToken.equals("") && !clientSecret.equals(newToken) ) {
						if(newToken!=null || !newToken.equals("") && !accessToken.equals(newToken) ) {
							http2.setRequestProperty("Authorization", "Bearer " + newToken);
							http2.setRequestProperty("Content-Type", "application/json");
							OutputStream osw2 = http2.getOutputStream();
							osw2.write(llCredVal.toString().getBytes());
							osw2.flush();
							osw2.close();
							http2.connect();
						
							serverResponseCode = http2.getResponseCode();
								//Now update the newToken in hashmap and as well as in table
								if (companyTimestampHash.getDataProviderId() == (dataProviderId)) {
									companyTimestampHash.setApiReqParams(newToken);
									UpdateToken(newToken,dataProviderId,dataProviderName);
							}
						}
//					}
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


				String result = sb.toString();
				JSONArray jsonArray = new JSONArray(result);
				if(jsonArray.length() > 0) {
					for(int iCount=0;iCount < jsonArray.length();iCount++) {
						JSONObject llVehDetail = jsonArray.getJSONObject(iCount);
						String mobileUnitId = llVehDetail.getString("vehicleId");
						JSONObject snapShotModel = llVehDetail.getJSONObject("snapshot");
						
						String llLicensePlate = snapShotModel.getString("registrationNumber");
						Double llLat = snapShotModel.getDouble("gpsLatitude");
						Double llLon = snapShotModel.getDouble("gpsLongitude");
						String llEventDateTime = snapShotModel.getString("eventDateTime");
						SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
						DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						Date llGpSt = null;
						try {
//							llGpSt = formatter.parse(llEventDateTime);
							SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss");
							// input is in UTC
							sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
							Date utcTimeStamp = sdf.parse(llEventDateTime);
							
							// another formatter for output
							SimpleDateFormat outputFormat= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							outputFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
							outputFormat.format(utcTimeStamp);
							log.info("outputFormat:- "+outputFormat.format(utcTimeStamp));
							llGpSt=formatstr.parse(outputFormat.format(utcTimeStamp));  

						} catch (ParseException e) {
							// TODO Auto-generated catch block
							log.error(e.getMessage());
							e.printStackTrace();
						}
						int llSpeed = snapShotModel.getInt("speed");
						Boolean llIgnitionStatus = snapShotModel.getBoolean("ignitionOn");
						long llOdometer = snapShotModel.getLong("odometer");
						boolean llAcc = false;
						if (llSpeed > 0) {
							llAcc = true;
						}
						GPSDataModel model = new GPSDataModel();
						model.setLicensePlate(llLicensePlate);
						model.setLatitude(llLat);
						model.setLongitude(llLon);
						model.setGpsdate(llGpSt);
						model.setSpeed((short) llSpeed);
						model.setAcc(llAcc);
						model.setMobileunitid(mobileUnitId);
						model.setMileage((int)llOdometer);
						dataList.add(model);
					}
				}
			}else {
				log.info("TATA_MOTOR_VEHICLE_LIMIT :- "+vehicleLimit+" ; VehicleCount :- "+llVehList.size());
				if (llVehList.size() > 0) {
					int processedCount = 0;
					for(int jCount=0;jCount < llVehList.size();jCount++ ) {
//						int processedCount = 0;
//						log.info("UnitNo :- "+jCount);
						Object[] obj = (Object[]) llVehList.get(jCount);
						if ((String) obj[0] != null) {
							llJSArr.put((String) obj[0]);
						}
						if(jCount == ((vehicleLimit+processedCount)-1)||((llVehList.size()-1)==jCount)) {
//							log.info("Go for Processing Started Here");

							JSONObject llCredVal = new JSONObject();
							llCredVal.put("chassisNumbers", llJSArr);
							URL url = new URL(URL);
							URLConnection con = url.openConnection();
							http = (HttpURLConnection) con;
							http.setRequestMethod("POST"); // GET
							http.setDoOutput(true);
							http.setRequestProperty("Authorization", "Bearer " + llToken);
							http.setRequestProperty("Content-Type", "application/json");
							OutputStream osw = http.getOutputStream();
							osw.write(llCredVal.toString().getBytes());
							osw.flush();
							osw.close();
							http.connect();
							serverResponseCode = http.getResponseCode();
							if(serverResponseCode == CommonConstants.UNAUTHORISED_ACCESS) {
								if (http != null) {
									http.disconnect();
									http = null;
								}
								URL url2 = new URL(URL);
								URLConnection con2 = url2.openConnection();
								http2 = (HttpURLConnection) con2;
								http2.setRequestMethod("POST"); // GET
								http2.setDoOutput(true);
								http2.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

								//Get the token from table and check with token in the memory(hashmap).If both are same then regenerate the token by calling
								//another api
								boolean isvalidToken = false;
//								isvalidToken = CheckToken(clientSecret,dataProviderId,dataProviderName);
								isvalidToken = CheckToken(accessToken,dataProviderId,dataProviderName);
//								if(isvalidToken == false) {
									String newToken = "";
									newToken = GetTataMotorToken(tokenURL, clientID, clientSecret, grantType);
//									if(newToken!=null || !newToken.equals("") && !clientSecret.equals(newToken) ) {
									if(newToken!=null || !newToken.equals("") && !accessToken.equals(newToken) ) {
										http2.setRequestProperty("Authorization", "Bearer " + newToken);
										http2.setRequestProperty("Content-Type", "application/json");
										OutputStream osw2 = http2.getOutputStream();
										osw2.write(llCredVal.toString().getBytes());
										osw2.flush();
										osw2.close();
										http2.connect();
									
										serverResponseCode = http2.getResponseCode();
											//Now update the newToken in hashmap and as well as in table
											if (companyTimestampHash.getDataProviderId() == (dataProviderId)) {
												companyTimestampHash.setApiReqParams(newToken);
												UpdateToken(newToken,dataProviderId,dataProviderName);
										}
									}
//								}
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


							String result = sb.toString();
							JSONArray jsonArray = new JSONArray(result);
							if(jsonArray.length() > 0) {
								for(int iCount=0;iCount < jsonArray.length();iCount++) {
									JSONObject llVehDetail = jsonArray.getJSONObject(iCount);
									String mobileUnitId = llVehDetail.getString("vehicleId");
									JSONObject snapShotModel = llVehDetail.getJSONObject("snapshot");
									
									String llLicensePlate = snapShotModel.getString("registrationNumber");
									Double llLat = snapShotModel.getDouble("gpsLatitude");
									Double llLon = snapShotModel.getDouble("gpsLongitude");
									String llEventDateTime = snapShotModel.getString("eventDateTime");
									SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
									DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
									Date llGpSt = null;
									try {
//										llGpSt = formatter.parse(llEventDateTime);
										SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss");
										// input is in UTC
										sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
										Date utcTimeStamp = sdf.parse(llEventDateTime);
										
										// another formatter for output
										SimpleDateFormat outputFormat= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
										outputFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
										outputFormat.format(utcTimeStamp);
										log.info("outputFormat:- "+outputFormat.format(utcTimeStamp));
										llGpSt=formatstr.parse(outputFormat.format(utcTimeStamp));  

									} catch (ParseException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
										log.error(e.getMessage());
									}
									int llSpeed = snapShotModel.getInt("speed");
									Boolean llIgnitionStatus = snapShotModel.getBoolean("ignitionOn");
									long llOdometer = snapShotModel.getLong("odometer");
									boolean llAcc = false;
									if (llSpeed > 0) {
										llAcc = true;
									}
									GPSDataModel model = new GPSDataModel();
									model.setLicensePlate(llLicensePlate);
									model.setLatitude(llLat);
									model.setLongitude(llLon);
									model.setGpsdate(llGpSt);
									model.setSpeed((short) llSpeed);
									model.setAcc(llAcc);
									model.setMobileunitid(mobileUnitId);
									model.setMileage((int)llOdometer);
									dataList.add(model);
								}
							}
							log.info("Go for Processing Ended Here");
							processedCount = jCount;
							llJSArr = new JSONArray();
						}
					}
					
				}

			}

			log.info(companyID + " dataList : " + dataList.size() + " Time : " + new Date());

		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
		return dataList;
	}
	
	public ArrayList<GPSDataModel> GetDataFromTATAMotors_GET_METHORD(String URL, String companyID,
			HashMap<String, String> columnList, String timeformat, String tokenURL, String clientId, String accessToken,
			int dataProviderId, String dataProviderName, MobileUnitTimestamp companyTimestampHash) {

		ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
		InputStream in = null;
		Reader r = null;
		HttpURLConnection http = null;
		HttpURLConnection http2 = null;
		int serverResponseCode;
		StringBuilder sb = null;
		InputStreamReader inReader;
//		String llToken = clientSecret;
		String llToken = accessToken;
		String clientID = "";
		String grantType = "";
		String clientSecret = "";

		if (clientId != null && !clientId.equals("")) {
			try {
				if (clientId != null && !clientId.isEmpty()) {
					JSONObject clientParam = new JSONObject(clientId);
					if (clientParam.has("client_id") && clientParam.getString("client_id") != null) {
						clientID = clientParam.getString("client_id");
					}
					if (clientParam.has("client_secret") && clientParam.getString("client_secret") != null) {
						clientSecret = clientParam.getString("client_secret");
					}
					if (clientParam.has("grant_type") && clientParam.getString("grant_type") != null) {
						grantType = clientParam.getString("grant_type");
					}
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				log.error(e.getMessage());
				e.printStackTrace();
			}
		}
		try {		
			
			URL url = new URL(URL);
			URLConnection con = url.openConnection();
			http = (HttpURLConnection) con;
			http.setRequestMethod("GET");
			http.setDoOutput(true);
			http.setRequestProperty("Authorization", "Bearer " + llToken);
			http.setRequestProperty("Content-Type", "application/json");
			http.connect();
			serverResponseCode = http.getResponseCode();
			log.error("Tata Motor Token 1 : " + serverResponseCode);
			if (serverResponseCode == CommonConstants.UNAUTHORISED_ACCESS) {
				if (http != null) {
					http.disconnect();
					http = null;
				}
				URL url2 = new URL(URL);
				URLConnection con2 = url2.openConnection();
				http2 = (HttpURLConnection) con2;
				http2.setRequestMethod("GET");
				http2.setDoOutput(true);
				http2.setRequestProperty("Content-Type", "application/json");

				boolean isvalidToken = false;
				isvalidToken = CheckToken(accessToken, dataProviderId, dataProviderName);
				String newToken = "";
				newToken = GetTataMotorToken(tokenURL, clientID, clientSecret, grantType);
				if (newToken != null || !newToken.equals("") && !accessToken.equals(newToken)) {
					http2.setRequestProperty("Authorization", "Bearer " + newToken);
					http2.setRequestProperty("Content-Type", "application/json");
					http2.connect();
					serverResponseCode = http2.getResponseCode();
					log.error("Tata Motor Token 2 : " + serverResponseCode);
					if (companyTimestampHash.getDataProviderId() == (dataProviderId)) {
						companyTimestampHash.setApiReqParams(newToken);
						UpdateToken(newToken, dataProviderId, dataProviderName);
						log.error("Tata Motor Token Updated : " + newToken);
					}
				}
			}
			if (http != null) {
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
					} catch (Exception e) {
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
			if (http2 != null) {
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
					} catch (Exception e) {
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

			String result = sb.toString();
			log.error("Tata Motor Res : " + result);
			JSONObject res = new JSONObject(result);
			if (res != null && res.has("vehicles")) {
				JSONArray jsonArray = res.getJSONArray("vehicles");
				if (jsonArray.length() > 0) {
					for (int iCount = 0; iCount < jsonArray.length(); iCount++) {
						JSONObject snapShotModel = jsonArray.getJSONObject(iCount);

						String llLicensePlate = snapShotModel.getString("registrationNumber");
						Double llLat = snapShotModel.getDouble("gpsLatitude");
						Double llLon = snapShotModel.getDouble("gpsLongitude");
						String llEventDateTime = snapShotModel.getString("eventDateTime");
						DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						Date llGpSt = null;
						try {
//								llGpSt = formatter.parse(llEventDateTime);
							SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
							// input is in UTC
							sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
							Date utcTimeStamp = sdf.parse(llEventDateTime);

							// another formatter for output
							SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							outputFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
							outputFormat.format(utcTimeStamp);
							log.info("outputFormat:- " + outputFormat.format(utcTimeStamp));
							llGpSt = formatstr.parse(outputFormat.format(utcTimeStamp));

						} catch (ParseException e) {
							// TODO Auto-generated catch block
							log.error(e.getMessage());
							e.printStackTrace();
						}
						int llSpeed = snapShotModel.getInt("speed");
						Boolean llIgnitionStatus = snapShotModel.getBoolean("ignitionOn");
						long llOdometer = snapShotModel.getLong("odometer");
						boolean llAcc = false;
						if (llSpeed > 0) {
							llAcc = true;
						}
						GPSDataModel model = new GPSDataModel();
						model.setLicensePlate(llLicensePlate);
						model.setLatitude(llLat);
						model.setLongitude(llLon);
						model.setGpsdate(llGpSt);
						model.setSpeed((short) llSpeed);
						model.setAcc(llAcc);
						model.setMileage((int) llOdometer);
						dataList.add(model);
					}
				}
			}
			log.info(companyID + " dataList : " + dataList.size() + " Time : " + new Date());

		} catch (Exception e) {
			e.printStackTrace();
			log.error("Tata Motor Error : "+ e.getMessage());
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
