package com.claystone.server.timeout;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import com.claystone.common.utils.CommonConstants;
import com.claystone.common.utils.CommonMethods;
import com.claystone.db.GpsApiPolling;
import com.claystone.db.GpsApiPollingId;
import com.claystone.server.util.HibernateUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;


public class JsonMethodAPI {
	private static Logger log;
	public JsonMethodAPI() {
		log = Logger.getLogger(XmlMethodAPI.class);	

	}

	public ArrayList<GPSDataModel> GetDataFromAPI(String URL, String unitID, HashMap<String, String> columnList,
			String timeformat) {
		ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
		try {
			log.info(unitID + /* " Response result : " + result + */"; URL : "+ URL + "; Time : " + new Date());
			String result = getJSONAPIResult(URL);
			for (Map.Entry<String, String> entry : columnList.entrySet()) {
				result = result.replace(entry.getValue(), entry.getKey());
			}
			/*
			 * if(URL.indexOf("http://globalgps.in:8089/api/v1/LiveDataForAlok1") > -1) {
			 * log.info("globalgps : "+ result); }
			 */
			if (result.startsWith("[") && result.endsWith("]")) {
				Gson gson = GsonBuilderWithoutTimezone(timeformat);
				GPSDataModel[] dataArray = gson.fromJson(result, GPSDataModel[].class);
				for (GPSDataModel dataModel : dataArray) {
					// Added on 2020-OCT-15 Ignition remains false/OFF even when speed is > 0 so
					// checking speed for all devices
					if (dataModel.getSpeed() > 0) {
						dataModel.setAcc(true);
					} else {
						dataModel.setAcc(false);
					}
					// Commented on 2020-OCT-15 Ignition remains false/OFF even when speed is > 0 so
					// checking speed for all devices
					/*
					 * if(dataModel.getIgnition().equalsIgnoreCase("ON")) { dataModel.setAcc(true);
					 * }
					 */

					dataModel.setMobileunitid(unitID);
					dataList.add(dataModel);
				}
			}
			log.info(unitID + " dataList : " + dataList.size() + " Time : " + new Date());
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
		return dataList;
	}

	public ArrayList<GPSDataModel> GetDataFromAPIAxes(String URL, String unitID, HashMap<String, String> columnList,
			String timeformat) {
		ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
		try {

			String result = getJSONAPIResult(URL);
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(result);
			JSONObject jo = (JSONObject) obj;
			List val = (List) jo.get("result");
			result = String.valueOf(val); // res.toString();
			for (Map.Entry<String, String> entry : columnList.entrySet()) {
				result = result.replace(entry.getValue(), entry.getKey());
			}
			log.info(unitID + " Response result : " + result + " Time : " + new Date());
			if (result.startsWith("[") && result.endsWith("]") && !result.contains("error")) {
				Gson gson = GsonBuilderWithoutTimezone(timeformat);
				GPSDataModel[] dataArray = gson.fromJson(result, GPSDataModel[].class);
				for (GPSDataModel dataModel : dataArray) {

					// Added on 2020-OCT-15 Ignition remains false/OFF even when speed is > 0 so
					// checking speed for all devices
					if (dataModel.getSpeed() > 0) {
						dataModel.setAcc(true);
					} else {
						dataModel.setAcc(false);
					}
					// Commented on 2020-OCT-15 Ignition remains false/OFF even when speed is > 0 so
					// checking speed for all devices
					/*
					 * if(dataModel.getIgnition().equalsIgnoreCase("1")) { dataModel.setAcc(true); }
					 */

					dataModel.setMobileunitid(unitID);
					dataList.add(dataModel);
				}
			}

			log.info(unitID + " dataList : " + dataList.size() + " Time : " + new Date());
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
		return dataList;
	}

	public ArrayList<GPSDataModel> GetDataFromAPIRealTrack(String URL, String unitID,
			HashMap<String, String> columnList, String timeformat) {
		ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
		try {

			String result = getJSONAPIResult(URL);
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(result);
			JSONObject jo = (JSONObject) obj;
			List val = (List) jo.get("detail_data");
			result = String.valueOf(val);

			for (Map.Entry<String, String> entry : columnList.entrySet()) {
				result = result.replace(entry.getValue(), entry.getKey());
			}
			// log.info(unitID+ " Response result : " + result + " Time : " + new Date());
			Gson gson = GsonBuilderWithoutTimezone(timeformat);
			GPSDataModel[] dataArray = gson.fromJson(result, GPSDataModel[].class);
			for (GPSDataModel dataModel : dataArray) {

				// Added on 2020-OCT-15 Ignition remains false/OFF even when speed is > 0 so
				// checking speed for all devices
				if (dataModel.getSpeed() > 0) {
					dataModel.setAcc(true);
				} else {
					dataModel.setAcc(false);
				}

				// Commented on 2020-OCT-15 Ignition remains false/OFF even when speed is > 0 so
				// checking speed for all devices
				/*
				 * if(!dataModel.getIgnition().equalsIgnoreCase("off")) {
				 * dataModel.setAcc(true); }else { dataModel.setAcc(false); }
				 */

				dataModel.setMobileunitid(unitID);
				dataList.add(dataModel);
			}
			log.info(unitID + " dataList : " + dataList.size() + " Time : " + new Date());
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
		return dataList;
	}

	public ArrayList<GPSDataModel> GetDataFromTataAPI(String URL, String companyID, HashMap<String, String> columnList,
			String timeformat, String dataProviderName) {
		ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
		try {
			String result;
			if (dataProviderName != null && dataProviderName.equalsIgnoreCase("GOODSMOVER")) {
				result = getJSONAPIResult(URL);
			} else {
				result = getJSONPostAPIResult(URL);
			}
			if(result != null) {
				for (Map.Entry<String, String> entry : columnList.entrySet()) {
					result = result.replace(entry.getValue(), entry.getKey());
				}
			}
			
			log.info(companyID + " Response result : " + result + " Time : " + new Date());
			if (result != null && result.startsWith("[") && result.endsWith("]")) {
				Gson gson = GsonBuilderWithoutTimezone(timeformat);
				GPSDataModel[] dataArray = gson.fromJson(result, GPSDataModel[].class);
				for (GPSDataModel dataModel : dataArray) {

					if (dataProviderName != null && dataProviderName.equalsIgnoreCase("GOODSMOVER")) {
						if (dataModel.getSpeed() > 0) {
							dataModel.setAcc(true);
						} else {
							dataModel.setAcc(false);
						}
					} else {
						// Commented on 2020-OCT-15 Ignition remains false/OFF even when speed is > 0 so
						// checking speed for all devices
						/*
						 * if(!dataModel.getIgnition().equalsIgnoreCase("Ignition Off")) {
						 * dataModel.setAcc(true); }else { dataModel.setAcc(false); }
						 */
						// Added on 2020-OCT-15 Ignition remains false/OFF even when speed is > 0 so
						// checking speed for all devices
						if (dataModel.getSpeed() > 0) {
							dataModel.setAcc(true);
						} else {
							dataModel.setAcc(false);
						}
						dataModel
								.setGpsdate(new Date(dataModel.getGpsdate().getTime() + (((5 * 60) + 30) * 60 * 1000)));
					}
					dataList.add(dataModel);
				}
			}
			log.info(companyID + " dataList : " + dataList.size() + " Time : " + new Date());
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
		return dataList;
	}

	public ArrayList<GPSDataModel> GetDataFromGTROPYAPI(String URL, String companyID,
			HashMap<String, String> columnList, String timeformat, String dataProviderName) {
		ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
		try {
			// Added on 2020OCT08
			// String lUrl =
			// "http://ctyf.co.in/api/customizedconsignorobject?key=L2FwaS9jb25zaWdub3JpbmZvP2lkPTI/dG9rZW49NjU5Q0FFNDAwRg==&client=Toyota"
			// ;
			String result = getJSONAPIResult(URL);
			// String result = getJSONAPIResult(lUrl);
			if (result != null && !result.equals("Too Frequent calling,Service should be called after given interval limit")) {
				JSONParser parser = new JSONParser();
				Object obj = parser.parse(result);
				JSONObject jo = (JSONObject) obj;
				List val = (List) jo.get("result");
				result = String.valueOf(val);

				for (Map.Entry<String, String> entry : columnList.entrySet()) {
					result = result.replace('"' + entry.getValue() + '"', '"' + entry.getKey() + '"');
					// result.replaceAll(regex, replacement)
				}
				log.info(companyID + " Response result : " + result + " Time : " + new Date());
				if (result.startsWith("[") && result.endsWith("]")) {
					Gson gson = GsonBuilderWithoutTimezone(timeformat);
					GPSDataModel[] dataArray = gson.fromJson(result, GPSDataModel[].class);
					for (GPSDataModel dataModel : dataArray) {

						if (dataModel.getSpeed() > 0) {
							dataModel.setAcc(true);
						} else {
							dataModel.setAcc(false);
						}

						dataList.add(dataModel);
					}
				}
			} else {
				log.info(companyID + " dataList : " + result + " Time : " + new Date());
			}
			log.info(companyID + " dataList : " + dataList.size() + " Time : " + new Date());
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
		return dataList;
	}

	public ArrayList<GPSDataModel> GetDataFromVamosysAPI(String URL, String companyID, HashMap<String, String> columnList,
			String timeformat, String dataProviderName) {
		ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
		InputStream in = null;
		Reader r = null;
		try {
			GetMethod request = new GetMethod(URL);
			HttpClient httpclient = new HttpClient();
			HttpConnectionParams params = (HttpConnectionParams) httpclient.getHttpConnectionManager().getParams();
			params.setConnectionTimeout(20000);
			params.setSoTimeout(15000);

			// request.setRequestHeader("api", "2cf7a4c532661e1a8396ec5072a9ca19");
			int result = httpclient.executeMethod(request);

			// Display status code
			log.info("Response status code: " + result);
			// log.info("Response : " + request.getResponseBodyAsString());
			Gson gson = new Gson();
			if (result == 200) {
				in = new BufferedInputStream(request.getResponseBodyAsStream());
				r = new InputStreamReader(in, "UTF-8");
				BufferedReader streamReader = new BufferedReader(r);
				StringBuilder responseStrBuilder = new StringBuilder();
				String inputStr;
				while ((inputStr = streamReader.readLine()) != null) {
					responseStrBuilder.append(inputStr);
				}
				String data = "{\"onlineGPSDatas\":" + responseStrBuilder.toString() + "}";
				StatusModel responseObj = gson.fromJson(data, StatusModel.class);
				if (responseObj.getOnlineGPSDatas() != null) {
					ArrayList<OnlineGPSData> data1 = responseObj.getOnlineGPSDatas();
					for (int i = 0; i < data1.size(); i++) {
						OnlineGPSData dataModel = data1.get(i);
						long date = dataModel.getDate();
						// int lat = (int)(dataModel.getLatitude() * 1000000.0);
						// int lon = (int)(dataModel.getLongitude() * 1000000.0);
						int speed = dataModel.getSpeed();
						String ingStr = dataModel.getIgnitionStatus();
						boolean acc = false;
						if (ingStr.equals("ON") || speed > 0) {
							acc = true;
						}
						int direction = 0;
						int mileage = (int) dataModel.getOdoDistance();
						Date gpsdate = new Date(date);

						GPSDataModel model = new GPSDataModel();
						model.setLicensePlate(dataModel.getShortName());
						model.setLatitude(dataModel.getLatitude());
						model.setLongitude(dataModel.getLongitude());
						model.setAcc(acc);
						model.setSpeed((short) speed);
						model.setGpsdate(gpsdate);
						dataList.add(model);
					}
				}
			} else {
				log.info(companyID + " dataList : " + result + " Time : " + new Date());
			}
			log.info(companyID + " dataList : " + dataList.size() + " Time : " + new Date());
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
		return dataList;
	}

	public ArrayList<GPSDataModel> GetDataFromFleetAPI(String URL, String companyID, HashMap<String, String> columnList,
			String timeformat, String tokenRes) {
		ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
		try {
			String result = getJSONPostAPIFleet(URL, tokenRes);
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(result);
			JSONObject jo = (JSONObject) obj;
			List val = (List) jo.get("result");
			result = String.valueOf(val);
			for (Map.Entry<String, String> entry : columnList.entrySet()) {
				result = result.replace(entry.getValue(), entry.getKey());
			}
			log.info(companyID + " Response result : " + result + " Time : " + new Date());
			if (result.startsWith("[") && result.endsWith("]")) {
				Gson gson = GsonBuilderWithoutTimezone(timeformat);
				GPSDataModelDup[] dataArray = gson.fromJson(result, GPSDataModelDup[].class);
				ArrayList<GPSDataModel> gpsArray = new ArrayList<GPSDataModel>();
				for (GPSDataModelDup dataModel : dataArray) {
					GPSDataModel model = new GPSDataModel();
					// Commented on 2020-OCT-15 Ignition remains false/OFF even when speed is > 0 so
					// checking speed for all devices
					/*
					 * if(dataModel.getIgnition().equalsIgnoreCase("On")) { model.setAcc(true);
					 * }else { model.setAcc(false); }
					 */
					// Added on 2020-OCT-15 Ignition remains false/OFF even when speed is > 0 so
					// checking speed for all devices
					if (Integer.parseInt(dataModel.getSpeed()) > 0) {
						model.setAcc(true);
					} else {
						model.setAcc(false);
					}
					String[] values = dataModel.getLatlng().split(",");
					model.setLatitude(Double.parseDouble(values[0]));
					model.setLongitude(Double.parseDouble(values[1]));
					model.setGpsdate(dataModel.getGpsdate());
					if (dataModel.getSpeed() != null) {
						// String[] arrOfStr = dataModel.getSpeed().split(" km/h");
						String myStr = dataModel.getSpeed().replaceAll("km/h", "");
						myStr = myStr.trim();
						double speed = Double.parseDouble(myStr);
						// Short myShort = Short.valueOf(speed);
						model.setSpeed((short) speed);
					}
					// model.setDirection(Short.parseShort(dataModel.getDirection()));
					model.setLicensePlate(dataModel.getLicensePlate());

					// dataModel.setMobileunitid(companyID);
					dataList.add(model);
				}
			}
			log.info(companyID + " dataList : " + dataList.size() + " Time : " + new Date());
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

	public static String getJSONAPIResult(String urlString) {
		HttpURLConnection connection = null;
		int serverResponseCode;
		StringBuilder sb = null;
		InputStream in;
		InputStreamReader inReader;
		try {
			URL url = new URL(urlString);
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "*/*");
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
			case HttpsURLConnection.HTTP_BAD_REQUEST:
			case HttpsURLConnection.HTTP_UNAUTHORIZED:
			case HttpsURLConnection.HTTP_FORBIDDEN:
			case HttpsURLConnection.HTTP_NOT_FOUND:
			case HttpsURLConnection.HTTP_BAD_METHOD:
			case HttpsURLConnection.HTTP_INTERNAL_ERROR:
			case HttpsURLConnection.HTTP_GATEWAY_TIMEOUT:
			case HttpsURLConnection.HTTP_UNAVAILABLE:
				sb = new StringBuilder();
				try {
					in = connection.getInputStream();
					inReader = new InputStreamReader(in);
				}catch (Exception e) {
					log.error("Error in news parsing" + e.toString());
					in = connection.getErrorStream();
					inReader = new InputStreamReader(in);
				}
				BufferedReader reader = new BufferedReader(inReader);
				String line;
				while ((line = reader.readLine()) != null) {
					sb.append(line);
				}
			}
		} catch (Exception e) {
			log.error("Error in new parsing : " + urlString + " / "+e.toString());
			e.printStackTrace();
			return null;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
			in = null;
			inReader = null;
			connection = null;
		}
		if (sb == null)
			return null;
		else
			return sb.toString();
	}

	private String getJSONPostAPIResult(String uRL) {
		String response = null;
		try {
			PostMethod post = new PostMethod(uRL);
			post.setRequestHeader("Content-Type", "text/xml; charset=utf-8");
			HttpClient httpclient = new HttpClient();
			try {
				int result = httpclient.executeMethod(post);
				// Display status code
				log.info("Response status code: " + result);
				// Display response
				log.info("Response body: ");
				log.info(post.getResponseBodyAsString());

				response = post.getResponseBodyAsString();
			} catch (Exception e) {
				e.printStackTrace();
				log.error(e.getMessage());
			} finally {
				// Release current connection to the connection pool once you are done
				post.releaseConnection();
				((SimpleHttpConnectionManager) httpclient.getHttpConnectionManager()).shutdown();
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error(e.getMessage());
		}
		return response;
	}

	private String getJSONPostAPIFleet(String uRL, String tokenRes) {
		String response = null;
		try {
			PostMethod post = new PostMethod(uRL);
			post.setRequestBody(tokenRes);
			post.setRequestHeader("Content-Type", "application/json; charset=utf-8");
			HttpClient httpclient = new HttpClient();
			try {
				int result = httpclient.executeMethod(post);
				// Display status code
				log.info("Response status code: " + result);
				// Display response
				log.info("Response body: ");
				log.info(post.getResponseBodyAsString());

				response = post.getResponseBodyAsString();
			} catch (Exception e) {
				log.error(e.getMessage());
				e.printStackTrace();
			} finally {
				// Release current connection to the connection pool once you are done
				post.releaseConnection();
				((SimpleHttpConnectionManager) httpclient.getHttpConnectionManager()).shutdown();
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e.getMessage());
			e.printStackTrace();
		}
		return response;
	}
	public void InsertGPSAPIPolling(MobileUnitTimestamp model, ArrayList<GPSDataModel> dataList) {
		try {
			if (model != null) {
				ArrayList<GpsApiPolling> gpsdataCheck = null;
				String mobileunit = CommonConstants.API_POLL_ALL;
				Session session = HibernateUtil.beginTransaction();
				if (model.getMobileUnitId() != null && !model.getMobileUnitId().trim().equals("")) {
					mobileunit = model.getMobileUnitId();
					gpsdataCheck = HibernateUtil.castList(GpsApiPolling.class,
							session.createQuery(" from GpsApiPolling" + " where id.mobileunitid = '"
									+ model.getMobileUnitId() + "'" + " and id.companyId = " + model.getCompanyId()
									+ " and id.dataProviderName = '" + model.getDataProviderName() + "'").list());
				} else {
					gpsdataCheck = HibernateUtil
							.castList(GpsApiPolling.class, session
									.createQuery(" from GpsApiPolling" + " where id.companyId = " + model.getCompanyId()
											+ " and id.dataProviderName = '" + model.getDataProviderName() + "'")
									.list());
				}

				GpsApiPolling apiPolling = null;
				if (gpsdataCheck != null && gpsdataCheck.size() > 0) {
					apiPolling = gpsdataCheck.get(0);
					apiPolling.setDataInterval(model.getDataInterval());
				} else {
					apiPolling = new GpsApiPolling();
					GpsApiPollingId apiPollingId = new GpsApiPollingId();
					apiPollingId.setCompanyId(model.getCompanyId());
					apiPollingId.setDataProviderName(model.getDataProviderName());
					apiPollingId.setMobileunitid(mobileunit);
					if (mobileunit.equals(CommonConstants.API_POLL_ALL))
						apiPollingId.setSearchKey(mobileunit);
					else
						apiPollingId.setSearchKey(CommonConstants.API_POLL_UNIT);
					apiPolling.setId(apiPollingId);
					apiPolling.setApiReqMethod(model.getApiReqMethod());
					apiPolling.setApiReqParams(model.getApiReqParams());
					apiPolling.setApiResType(model.getApiResType());
					apiPolling.setApiUrl(model.getApiUrl());
					apiPolling.setDataInterval(model.getDataInterval());
				}
				if (apiPolling != null) {
					apiPolling.setLicensePlate(model.getLicensePlate());
					apiPolling.setTransporterName(model.getTransporterName());
					apiPolling.setIsUnitActive(model.isUnitActive());
					apiPolling.setLastDataCount(dataList.size());
					apiPolling.setLastModifiedTime(new Date());
					apiPolling.setLastProcessedTime(model.getLastProcessedTime());
					apiPolling.setLastResponseCode(model.getLastResponseCode());
					if (model.getLastResponseTime() != null)
						apiPolling.setLastResponseTime(model.getLastResponseTime());
					else {
						if (gpsdataCheck == null || gpsdataCheck.size() <= 0) {
							DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							Date gpsdateStr = formatstr.parse("2000-01-01 00:00:00");
							apiPolling.setLastResponseTime(gpsdateStr);
						}
					}
					if (dataList.size() > 0) {
						if(dataList.get(dataList.size() - 1).getGpsdate() != null)
						{
							apiPolling.setLastGpsdateTime(dataList.get(dataList.size() - 1).getGpsdate());
						}else {
							for(int l = dataList.size();l > 0 ; l-- )
							{
								if(dataList.get(l).getGpsdate() != null)
								{
									log.info("API Polling Last Gpsdate is null hence check for others data if gpsdate is available : " +mobileunit); //Added on 2022-Mar-30
									apiPolling.setLastGpsdateTime(dataList.get(l).getGpsdate());
									break;									
								}
							}
						}
					} else {
						if (gpsdataCheck != null && gpsdataCheck.size() > 0) {
							if (apiPolling.getLastGpsdateTime() == null) {
								DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
								Date gpsdateStr = formatstr.parse("2000-01-01 00:00:00");
								apiPolling.setLastGpsdateTime(gpsdateStr);
							}
						} else {
							DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							Date gpsdateStr = formatstr.parse("2000-01-01 00:00:00");
							apiPolling.setLastGpsdateTime(gpsdateStr);
						}
					}

					if (gpsdataCheck != null && gpsdataCheck.size() > 0)
						session.update(apiPolling);
					else
						session.save(apiPolling);
				}
				HibernateUtil.commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
			HibernateUtil.rollback();
		} finally {
			HibernateUtil.close();
		}

	}
	public void InsertGPSAPIPolling_FastTag(MobileUnitTimestamp model, ArrayList<GPSDataModel> dataList) {
		try {
			if (model != null) {
				ArrayList<GpsApiPolling> gpsdataCheck = null;
//				String mobileunit = CommonConstants.API_POLL_ALL;
				String mobileunit = "";
				Session session = HibernateUtil.beginTransaction();
				if (model.getMobileUnitId() != null && !model.getMobileUnitId().trim().equals("")) {
					mobileunit = model.getMobileUnitId();
					gpsdataCheck = HibernateUtil.castList(GpsApiPolling.class,
							session.createQuery(" from GpsApiPolling" + " where id.mobileunitid = '"
									+ model.getMobileUnitId() + "'" + " and id.companyId = " + model.getCompanyId()
									+ " and id.dataProviderName = '" + model.getDataProviderName() + "'").list());
				} 
//					else {
//					gpsdataCheck = HibernateUtil
//							.castList(GpsApiPolling.class, session
//									.createQuery(" from GpsApiPolling" + " where id.companyId = " + model.getCompanyId()
//											+ " and id.dataProviderName = '" + model.getDataProviderName() + "'")
//									.list());
//				}
				Date lMT = new Date();
				GpsApiPolling apiPolling = null;
				if (gpsdataCheck != null && gpsdataCheck.size() > 0) {
					apiPolling = gpsdataCheck.get(0);
					apiPolling.setDataInterval(model.getDataInterval());
				} else {
					apiPolling = new GpsApiPolling();
					GpsApiPollingId apiPollingId = new GpsApiPollingId();
					apiPollingId.setCompanyId(model.getCompanyId());
					apiPollingId.setDataProviderName(model.getDataProviderName());
					apiPollingId.setMobileunitid(mobileunit);
//					if (mobileunit.equals(CommonConstants.API_POLL_ALL))
//						apiPollingId.setSearchKey(mobileunit);
//					else
						apiPollingId.setSearchKey(CommonConstants.API_POLL_UNIT);
					apiPolling.setId(apiPollingId);
					apiPolling.setApiReqMethod(model.getApiReqMethod());
					apiPolling.setApiReqParams(model.getApiReqParams());
					apiPolling.setApiResType(model.getApiResType());
					apiPolling.setApiUrl(model.getApiUrl());
					apiPolling.setDataInterval(model.getDataInterval());
				}
				if (apiPolling != null) {
					apiPolling.setLicensePlate(model.getLicensePlate());
					apiPolling.setTransporterName(model.getTransporterName());
					apiPolling.setIsUnitActive(model.isUnitActive());
					apiPolling.setLastDataCount(dataList.size());
//					apiPolling.setLastModifiedTime(new Date());
//					apiPolling.setLastProcessedTime(model.getLastProcessedTime());
					apiPolling.setLastModifiedTime(lMT);
					apiPolling.setLastProcessedTime(lMT);
					apiPolling.setLastResponseCode(model.getLastResponseCode());
					apiPolling.setLastResponseTime(lMT);
//					if (model.getLastResponseTime() != null)
//						apiPolling.setLastResponseTime(model.getLastResponseTime());
//					else {
//						if (gpsdataCheck == null || gpsdataCheck.size() <= 0) {
//							DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//							Date gpsdateStr = formatstr.parse("2000-01-01 00:00:00");
//							apiPolling.setLastResponseTime(gpsdateStr);
//						}
//					}
					if (dataList.size() > 0) {
						if(dataList.get(dataList.size() - 1).getGpsdate() != null)
						{
							apiPolling.setLastGpsdateTime(dataList.get(dataList.size() - 1).getGpsdate());
						}else {
							for(int l = dataList.size();l > 0 ; l-- )
							{
								if(dataList.get(l).getGpsdate() != null)
								{
									log.info("API Polling Last Gpsdate is null hence check for others data if gpsdate is available : " +mobileunit); //Added on 2022-Mar-30
									apiPolling.setLastGpsdateTime(dataList.get(l).getGpsdate());
									break;									
								}
							}
						}
					} else {
						if (gpsdataCheck != null && gpsdataCheck.size() > 0) {
							if (apiPolling.getLastGpsdateTime() == null) {
								DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
								Date gpsdateStr = formatstr.parse("2000-01-01 00:00:00");
								apiPolling.setLastGpsdateTime(gpsdateStr);
							}
						} else {
							DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							Date gpsdateStr = formatstr.parse("2000-01-01 00:00:00");
							apiPolling.setLastGpsdateTime(gpsdateStr);
						}
					}

					if (gpsdataCheck != null && gpsdataCheck.size() > 0)
						session.update(apiPolling);
					else
						session.save(apiPolling);
				}
				HibernateUtil.commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
			HibernateUtil.rollback();
		} finally {
			HibernateUtil.close();
		}

	}

	public void InsertGPSAPIPollingCompany(MobileUnitTimestamp model, GPSDataModel dataList, String mobileUnitId,
			String transCode) {
		try {
			if (model != null) {
				ArrayList<GpsApiPolling> gpsdataCheck = null;
				String mobileunit = CommonConstants.API_POLL_ALL;
				Session session = HibernateUtil.beginTransaction();
				if (mobileUnitId != null && !mobileUnitId.trim().equals("")) {
					mobileunit = mobileUnitId;
					gpsdataCheck = HibernateUtil.castList(GpsApiPolling.class,
							session.createQuery(" from GpsApiPolling" + " where id.mobileunitid = '" + mobileUnitId
									+ "'" + " and id.companyId = " + model.getCompanyId()
									+ " and id.dataProviderName = '" + model.getDataProviderName() + "'").list());
				} else {
					gpsdataCheck = HibernateUtil
							.castList(GpsApiPolling.class, session
									.createQuery(" from GpsApiPolling" + " where id.companyId = " + model.getCompanyId()
											+ " and id.dataProviderName = '" + model.getDataProviderName() + "'")
									.list());
				}

				GpsApiPolling apiPolling = null;
				if (gpsdataCheck != null && gpsdataCheck.size() > 0) {
					apiPolling = gpsdataCheck.get(0);
					apiPolling.setDataInterval(model.getDataInterval());
				} else {
					apiPolling = new GpsApiPolling();
					GpsApiPollingId apiPollingId = new GpsApiPollingId();
					apiPollingId.setCompanyId(model.getCompanyId());
					apiPollingId.setDataProviderName(model.getDataProviderName());
					apiPollingId.setMobileunitid(mobileunit);
					if (mobileunit.equals(CommonConstants.API_POLL_ALL))
						apiPollingId.setSearchKey(mobileunit);
					else
						apiPollingId.setSearchKey(CommonConstants.API_POLL_UNIT);
					apiPolling.setId(apiPollingId);
					apiPolling.setApiReqMethod(model.getApiReqMethod());
					apiPolling.setApiReqParams(model.getApiReqParams());
					apiPolling.setApiResType(model.getApiResType());
					apiPolling.setApiUrl(model.getApiUrl());
					apiPolling.setDataInterval(model.getDataInterval());
				}
				if (apiPolling != null) {
					apiPolling.setLicensePlate(dataList.getLicensePlate());
					apiPolling.setTransporterName(transCode);
					apiPolling.setIsUnitActive(model.isUnitActive());
					apiPolling.setLastDataCount(1);
					apiPolling.setLastModifiedTime(new Date());
					apiPolling.setLastProcessedTime(model.getLastProcessedTime());
					apiPolling.setLastResponseCode(model.getLastResponseCode());
					if (model.getLastResponseTime() != null)
						apiPolling.setLastResponseTime(model.getLastResponseTime());
					else {
						if (gpsdataCheck == null || gpsdataCheck.size() <= 0) {
							DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							Date gpsdateStr = formatstr.parse("2000-01-01 00:00:00");
							apiPolling.setLastResponseTime(gpsdateStr);
						}
					}
					if (dataList.getGpsdate() != null) {
						apiPolling.setLastGpsdateTime(dataList.getGpsdate());
					} else {
						if (gpsdataCheck != null && gpsdataCheck.size() > 0) {
							if (apiPolling.getLastGpsdateTime() == null) {
								DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
								Date gpsdateStr = formatstr.parse("2000-01-01 00:00:00");
								apiPolling.setLastGpsdateTime(gpsdateStr);
							}
						} else {
							DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							Date gpsdateStr = formatstr.parse("2000-01-01 00:00:00");
							apiPolling.setLastGpsdateTime(gpsdateStr);
						}
					}

					if (gpsdataCheck != null && gpsdataCheck.size() > 0)
						session.update(apiPolling);
					else
						session.save(apiPolling);
				}
				HibernateUtil.commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
			HibernateUtil.rollback();
		} finally {
			HibernateUtil.close();
		}

	}

	class XModelJsonDeserializer implements JsonDeserializer<GPSDataModel> {

		private final Set<String> TRUE_STRINGS = new HashSet<>(Arrays.asList("true", "1", "yes"));

		@Override
		public GPSDataModel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			GPSDataModel response = new GPSDataModel();
			JsonObject jsonResponse = (JsonObject) json;
			JsonElement dataElement = jsonResponse.get("ignition");
			if (dataElement.isJsonNull()) {
				response.setAcc(false);
			} else if (dataElement.isJsonPrimitive()) {
				JsonPrimitive jsonPrimitive = dataElement.getAsJsonPrimitive();
				if (jsonPrimitive.isBoolean()) {
					response.setAcc(jsonPrimitive.getAsBoolean());
				} else if (jsonPrimitive.isNumber()) {
					response.setAcc(jsonPrimitive.getAsNumber().intValue() == 1);
				} else if (jsonPrimitive.isString()) {
					response.setAcc(TRUE_STRINGS.contains(jsonPrimitive.getAsString()));
				}
				log.info("Json data is primitive: " + dataElement.getAsString());
			} else if (dataElement.isJsonObject() || dataElement.isJsonArray()) {
				response.setAcc(true); // ?!?!
			}
			return response;
		}
		

	}	
	//15MAR2024
	public ArrayList<GPSDataModel> GetDataFromAPIGENTR(String URL, String unitID, HashMap<String, String> columnList,
			String timeformat) {
		ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
		try {
			log.info(unitID + /* " Response result : " + result + */"; URL : "+ URL + "; Time : " + new Date());
			String result = getJSONAPIResult(URL);
			if(result != null) {
				JSONParser parser = new JSONParser();
				JSONObject responsejson = (JSONObject) parser.parse(result);
				if(responsejson.containsKey("root")) {
					JSONObject rootobj = (JSONObject) responsejson.get("root");
					if(rootobj.containsKey("VehicleData")) {
						JSONArray vehArray = (JSONArray) rootobj.get("VehicleData");
						if(vehArray.size() > 0) {
							for(int i=0; i < vehArray.size(); i++) {
								JSONObject obj = (JSONObject) vehArray.get(i);
								GPSDataModel dataModel =new GPSDataModel();
								dataModel.setMobileunitid(unitID);
								String lDt= (String) obj.get("GPSActualTime");
								
								if(!lDt.equals("")) {
									String day = lDt.substring(0, 2);
									String month = lDt.substring(3, 5);
									String year = lDt.substring(6,10);
									
									
									String hour = lDt.substring(11,13);
									String minute = lDt.substring(14, 16);
									String sec = lDt.substring(17, 19);
									String tStamp = year+"-"+month+"-"+day+" "+hour+":"+minute+":"+sec;
									Date gpsDate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(tStamp);  
									dataModel.setGpsdate(gpsDate);
								}
								
								String lat= (String) obj.get("Latitude");
								dataModel.setLatitude(Double.parseDouble(lat));
								String lnt= (String) obj.get("Longitude");
								dataModel.setLongitude(Double.parseDouble(lnt));
								
								String speed = (String)obj.get("Speed");
								dataModel.setSpeed(Short.valueOf(speed));		
//								dataModel.setDirection((short) 0); // no values from api
							
//								dataModel.setEventcode(null);// no values from api
//								long battery_percentage = (long) obj.get("battery_percentage");
//								String battery_percentage = (String) obj.get("battery_percentage");
//								dataModel.setBatt(Short.valueOf(battery_percentage));
								long battery_percentage = (long) obj.get("battery_percentage");
								dataModel.setBatt((short)battery_percentage);
								
								String milage = (String)obj.get("Odometer");
								dataModel.setMileage(Integer.valueOf(milage));	
								

								dataModel.setLicensePlate((String) obj.get("Vehicle_No"));
								dataModel.setCanBusDataAvailable(false);
								dataModel.setlCanModelList(null);
								if (dataModel.getSpeed() > 0) {
									dataModel.setAcc(true);
								} else {
									dataModel.setAcc(false);
								}
								
								
								dataList.add(dataModel);
							}
						}
					}
					if(responsejson.containsKey("error")) {
						dataList = new ArrayList<GPSDataModel>();
					}
					
				}
				
			}
			
				
			
			log.info("result: "+result); 
			log.info("result: "+result.contains("root"));
			

			log.info(unitID + " dataList : " + dataList.size() + " Time : " + new Date());
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
		return dataList;
	}

	//26AUG2024--TLI-GENTR changed the api without informing us...Here is the changes api
	//15MAR2024
	public ArrayList<GPSDataModel> GetDataFromAPIGENTRNew(String URL, String unitID, HashMap<String, String> columnList,
			String timeformat) {
		ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
		try {
			log.info(unitID + /* " Response result : " + result + */"; URL : "+ URL + "; Time : " + new Date());
			String result = getJSONAPIResult(URL);
			if(result != null) {
				JSONParser parser = new JSONParser();
				JSONObject responsejson = (JSONObject) parser.parse(result);
				if(responsejson.containsKey("status")) {
					Long statusLng = (Long) responsejson.get("status");

					if(statusLng == 1) {
						JSONObject rootobj = (JSONObject) responsejson.get("data");
						GPSDataModel dataModel =new GPSDataModel();
						dataModel.setMobileunitid(unitID);
						String lDt= (String) rootobj.get("gpsDateTime");
						
						//10SEP2024---TimeStamp format has changed
//						if(!lDt.equals("")) {
//							Date gpsDate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(lDt);  
//							dataModel.setGpsdate(gpsDate);
//						}
						if(!lDt.equals("")) {
							
//							String day = lDt.substring(0, 2);
//							String month = lDt.substring(3, 5);
//							String year = lDt.substring(6,10);
							
							String year = lDt.substring(0,4);
							String month = lDt.substring(4, 6);
							String day = lDt.substring(6, 8);
							
							
							
							String hour = lDt.substring(8,10);
							String minute = lDt.substring(10, 12);
							String sec = lDt.substring(12, 14);
							String tStamp = year+"-"+month+"-"+day+" "+hour+":"+minute+":"+sec;
							Date gpsDate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(tStamp);  
							dataModel.setGpsdate(gpsDate);
						}
						
						String lat= (String) rootobj.get("latitude");
						dataModel.setLatitude(Double.parseDouble(lat));
						String lnt= (String) rootobj.get("longitude");
						dataModel.setLongitude(Double.parseDouble(lnt));
						
						Long speed = (Long)rootobj.get("speed");
						dataModel.setSpeed(speed.shortValue());		
						dataModel.setLicensePlate((String) rootobj.get("PlateNo"));
						dataModel.setCanBusDataAvailable(false);
						dataModel.setlCanModelList(null);
						if (dataModel.getSpeed() > 0) {
							dataModel.setAcc(true);
						} else {
							dataModel.setAcc(false);
						}
						dataList.add(dataModel);
					}else if(statusLng != 0) {
							dataList = new ArrayList<GPSDataModel>();

					}
				}
			}
			
				
			
			log.info("result: "+result); 
			log.info("result: "+result.contains("root"));
			

			log.info(unitID + " dataList : " + dataList.size() + " Time : " + new Date());
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
		return dataList;
	}

	//15NOV2024::XPINDIA api
	public ArrayList<GPSDataModel> GetDataFromAPIZXPIndia(String URL, String unitID, HashMap<String, String> columnList,
			String timeformat,MobileUnitTimestamp mobileUnitTimestamp) {
		ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
		try {
			log.info(unitID + /* " Response result : " + result + */"; URL : "+ URL + "; Time : " + new Date());
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
				http.setRequestMethod("GET"); // POST
				http.setDoOutput(true);
				http.setRequestProperty("Content-Type", "application/json");
				http.setRequestProperty(mobileUnitTimestamp.getTokenUrl(), mobileUnitTimestamp.getTokenReqParams());
				
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

				String result = sb.toString();
				log.error("XP India Res : " + result);
				JSONParser parser = new JSONParser();
				Object obj = parser.parse(result);
				JSONObject jo = (JSONObject) obj;
				if(jo.containsKey("Data")) {
					JSONArray vehArray = (JSONArray) jo.get("Data");
					if(vehArray.size() > 0) {
						for(int i=0; i < vehArray.size(); i++) {
							JSONObject vehObj = (JSONObject) vehArray.get(i);
							GPSDataModel dataModel =new GPSDataModel();
							dataModel.setMobileunitid(unitID);
							
							String lat = (String) vehObj.get("Latitude");
//							dataModel.setLatitude(Double.parseDouble(lat));
							String lnt = (String) vehObj.get("Longitude");
//							dataModel.setLongitude(Double.parseDouble(lnt));
							
//							int latitude = (int) ((lat) * Double.valueOf(1000000.00));
//							int longitude = (int) ((lnt) * Double.valueOf(1000000.00));
							
							double latdbl = Double.parseDouble(lat);
							double lntdbl = Double.parseDouble(lnt);
							 
							int latitude = (int) ((latdbl) * Double.valueOf(1000000.00));
							int longitude = (int) ((lntdbl) * Double.valueOf(1000000.00));
							
						
							dataModel.setLatitude(Double.parseDouble(lat));
							dataModel.setLongitude(Double.parseDouble(lnt));							
							
							String lDt= (String) vehObj.get("LocationUpdateDateTime");
							LocalDateTime dateTime = LocalDateTime.parse(lDt);
							String formattedDate = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
							
							Date gpsDate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(formattedDate);  
							dataModel.setGpsdate(gpsDate);
							
							dataList.add(dataModel);
						}
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage());
				e.printStackTrace();
			}
			return dataList;

		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
		return dataList;
	}
}