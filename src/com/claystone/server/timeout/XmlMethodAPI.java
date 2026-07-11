package com.claystone.server.timeout;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class XmlMethodAPI {
	private Logger	log;
	public XmlMethodAPI() {
		log = Logger.getLogger(XmlMethodAPI.class); 
	}
	
	public ArrayList<GPSDataModel> GetDataFromAPI(String URL, String reqParam , String unitID){
		ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
		try {
			PostMethod post = new PostMethod(URL);
			post.setRequestBody(reqParam);
			post.setRequestHeader("Content-Type", "text/xml; charset=utf-8");
			HttpClient httpclient = new HttpClient();
			//httpclient.setConnectionTimeout(1000);
			httpclient.setTimeout(60000);
			try {
				int result = httpclient.executeMethod(post);
				// Display status code
				log.info(unitID+ " Response status code: " + result + " Time : " + new Date()); 
				if(result == 200) {
					//log.info(post.getResponseBodyAsString());

					InputStream is = new ByteArrayInputStream(post.getResponseBody());
					SOAPMessage request = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL).createMessage(null, is);

					SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");

					//SOAPMessage soapMessage = getSoapMessageFromString(post.getResponseBodyAsString());
					SOAPBody soapBody = request.getSOAPBody(); 
					String response = soapBody.getTextContent();
					NodeList nodeList = soapBody.getChildNodes().item(0).getChildNodes().item(0).getChildNodes();

					if(nodeList.getLength() > 0) {
						Node node = nodeList.item(0);
						NodeList nodeChildList = nodeList.item(1).getChildNodes();
						Node  nodeChild = nodeChildList.item(0);
						log.info("Thread : "+ Thread.currentThread().getId() + " - "+ unitID + " Response : " + nodeChild + " Time : " + new Date());
						if(nodeChild != null) {
							NodeList list = nodeChild.getChildNodes();
							for(int i=0;i<list.getLength();i++){
								GPSDataModel aticDataModel = new GPSDataModel();
								Node  nodeTable = list.item(i);

								/*
								 * OnDate Direction Latitude Longitude Device_Id Speed VehNo GPSNo LocalityCity
								 * State DistanceInKM HaltDuration Event_Id DI1 DI2 DI3 DI4 DI5 DI6 DI7
								 */
								for(int i1=0;i1<nodeTable.getChildNodes().getLength();i1++){
									Element statusElement = (Element) nodeTable.getChildNodes().item(i1);
									switch (statusElement.getNodeName()) {
									case "VehNo":
										//aticDataModel.setVehNo(statusElement.getTextContent()) ;
										break;          // 
									case "Device_Id":
										aticDataModel.setMobileunitid(statusElement.getTextContent()) ;
										break;  
									case "Direction":
										aticDataModel.setDirection (Short.parseShort(statusElement.getTextContent())) ;
										break; 
									case "AntennaStatus":
										//aticDataModel.setAntennaStatus (Short.parseShort(statusElement.getTextContent())) ;
										break; 
									case "OnDate":
										aticDataModel.setGpsdate (formatter.parse(statusElement.getTextContent())) ;
										break; 
									case "Latitude":
										double lat = Double.parseDouble(statusElement.getTextContent()); 
										aticDataModel.setLatitude (lat) ;
										break; 
									case "Longitude":
										double lon = Double.parseDouble(statusElement.getTextContent()); 

										aticDataModel.setLongitude (lon) ;
										break; 
									case "Speed":
										aticDataModel.setSpeed(Short.parseShort(statusElement.getTextContent())) ;
										if(aticDataModel.getSpeed() > 0){
											aticDataModel.setAcc(true);
										} 
										break; 	
									case "DistanceInKM":
										aticDataModel.setMileage (Integer.parseInt(statusElement.getTextContent())) ;
										break;  		 
									default:
										break;
									}
								}
								dataList.add(aticDataModel);
							}
						} 
					} 
					log.info("Thread : "+ Thread.currentThread().getId() + " - "+unitID+ " Data Size : " + dataList.size() + " Time : " + new Date()); 
				}else {
					dataList = null;
					log.info("Thread : "+ Thread.currentThread().getId() + " - "+ unitID + " Response status code: " + result + " Time : " + new Date()); 
				} 
			} catch (SocketTimeoutException ste) {
				dataList = null;
				ste.printStackTrace();
				log.error("Thread : "+ Thread.currentThread().getId() + " - "+ unitID + " SocketTimeoutException : " + ste + "; Time : " + new Date()); 
			} catch (Exception e) {
				e.printStackTrace();
				log.error("Thread : "+ Thread.currentThread().getId() + " - "+ unitID + " Exception : " + e + " Time : " + new Date()); 
			}finally{
		         post.releaseConnection();
		         ((SimpleHttpConnectionManager)httpclient.getHttpConnectionManager()).shutdown();
		    }/*finally {
				// Release current connection to the connection pool once you are done
				post.releaseConnection();
			}*/
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e.getMessage());
			log.error("Thread : "+ Thread.currentThread().getId() + " - "+ unitID + " Exception : " + e + "; Time : " + new Date()); 
			e.printStackTrace();
		}
		return dataList;

	}
	public ArrayList<GPSDataModel> GetDataFromAPI(String URL, String unitID, 
			HashMap<String, String> columnList, String timeformat, String reqParameter){
		ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
		try {
			String result = getXMLAPIResult(URL, reqParameter);
			if(result != null) {
				for (Map.Entry<String,String> entry : columnList.entrySet())  {
					result = result.replace(entry.getValue(), entry.getKey());
				}
				log.info(unitID+ " Response result : " + result + " Time : " + new Date()); 
				if(result.startsWith("[") && result.endsWith("]")) {
					Gson gson = GsonBuilderWithoutTimezone(timeformat);
					GPSDataModel[] dataArray = gson.fromJson(result, GPSDataModel[].class);
					for(GPSDataModel dataModel : dataArray) {

						if(dataModel.getIgnition().equalsIgnoreCase("ON")) {
							dataModel.setAcc(true);
						}

						dataModel.setMobileunitid(unitID);
						dataList.add(dataModel);
					}
				}
			}
			log.info(unitID+ " dataList : " + dataList.size() + " Time : " + new Date()); 
		} catch (Exception e) {log.error(e.getMessage());
			e.printStackTrace();
			log.error(e.getMessage());
		} 
		return dataList;
	}
	private String getXMLAPIResult(String uRL, String reqParameter) {
		String response = null;
		try {
			PostMethod post = new PostMethod(uRL);
			post.setRequestBody(reqParameter);
			post.setRequestHeader("Content-Type", "text/xml; charset=utf-8");
			HttpClient httpclient = new HttpClient();
			httpclient.setTimeout(60000);
			try {
				int result = httpclient.executeMethod(post);
				// Display status code
				//log.info("Response status code: " + result);
				// Display response
				//log.info("Response body: ");
				//log.info(post.getResponseBodyAsString());

				InputStream is = new ByteArrayInputStream(post.getResponseBody());
				SOAPMessage request = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL).createMessage(null, is);


				//SOAPMessage soapMessage = getSoapMessageFromString(post.getResponseBodyAsString());
				SOAPBody soapBody = request.getSOAPBody(); 
				response = soapBody.getTextContent();
			} catch (SocketTimeoutException ste) {
				response = null;
				log.error(ste.getMessage());
				ste.printStackTrace();
				log.error("Thread : "+ Thread.currentThread().getId() + " -  SocketTimeoutException : " + ste + "; Time : " + new Date()); 
			} catch (Exception e) {
				e.printStackTrace();
				log.error(e.getMessage());
			} finally {
				// Release current connection to the connection pool once you are done
				post.releaseConnection();
				 ((SimpleHttpConnectionManager)httpclient.getHttpConnectionManager()).shutdown();
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e.getMessage());
			e.printStackTrace();
		}
		return response;
	}
	public String GetDataFromTokenAPI(String URL, String reqParam , String unitID){
		ArrayList<GPSDataModel> dataList = new ArrayList<GPSDataModel>();
		String tokenRes = null;
		try {
			PostMethod post = new PostMethod(URL);
			post.setRequestBody(reqParam);
			post.setRequestHeader("Content-Type", "application/json; charset=utf-8");
			HttpClient httpclient = new HttpClient();
			try {
				int result = httpclient.executeMethod(post);
				// Display status code
				log.info(unitID+ " Response status code: " + result + " Time : " + new Date()); 
				if(result == 200) {
					//log.info(post.getResponseBodyAsString());
					DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
					InputSource src = new InputSource();
					src.setCharacterStream(new StringReader(post.getResponseBodyAsString()));

					Document doc = builder.parse(src);
					if(doc.getElementsByTagName("tokenString") != null) {
						 tokenRes = doc.getElementsByTagName("tokenString").item(0).getTextContent();
					}
					log.info(unitID+ " token : " + tokenRes + " Time : " + new Date()); 
				}else {
					tokenRes = null;
				} 
			} catch (Exception e) {
				e.printStackTrace();
				log.error(e.getMessage());
			} finally {
				// Release current connection to the connection pool once you are done
				post.releaseConnection();
				 ((SimpleHttpConnectionManager)httpclient.getHttpConnectionManager()).shutdown();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error(e.getMessage());
		}
		return tokenRes;

	}
	public static Gson GsonBuilderWithoutTimezone(String timeformat) {
		GsonBuilder gsonBuilder = new GsonBuilder(); 
		//gsonBuilder.registerTypeAdapter(GPSDataModel.class, new XModelJsonDeserializer());
		gsonBuilder.setDateFormat(timeformat);
		Gson gson = gsonBuilder
				.create();
		return gson;
	}
}
