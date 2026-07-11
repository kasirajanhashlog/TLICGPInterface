/*
 * B7ambooDatabase.java
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.claystone.server.timeout;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.StringTokenizer;

import org.apache.commons.lang.CharUtils;
import org.apache.log4j.Logger;

/**
 *
 * @author Administrator
 */


public class BambooData implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public String mobileUnitID = "";
	public Calendar gpsDate,  gprsDate;
	public char gpsStatus = 'N';
	public int latitude,  longitude,  speed,  direction,  adc1 = 0,  adc2 = 0,  batt = 0,  serverPort,  eventCode = 0, mileage=0;
	public boolean acc,  in1,  in2,  in3,  in4,  out1,  out2;
	public String locating;
	public String reservedStr = "NO";
	public GPSDataModel receivedMessageStr = null;
	private Logger log = Logger.getLogger(BambooData.class);
	public String alertMessageStr = "";
	private ByteBuffer unitBuffer;
	int COMMAND_LENGTH = 27;
	private String imeiCode;
	public ArrayList<CanBusModel> lCanModelList;	
	public Boolean isCanBusDataAvailable = false;

	private BambooData(GPSDataModel gpsDataModel)
	{
		receivedMessageStr = gpsDataModel;
	}

	public static BambooData getInstance(GPSDataModel gpsDataModel) {
		BambooData lBambooData = new BambooData(gpsDataModel);
		return lBambooData.ParseLocationInfo();
	}
	
	@SuppressWarnings("static-access")
	public BambooData ParseLocationInfo()
	{

		try
		{
			ArrayList<BambooData> lbambooDataList = new ArrayList<BambooData>();

			GPSDataModel lGPSDataModel = receivedMessageStr;

			this.mobileUnitID += lGPSDataModel.getMobileunitid();
			gpsDate = Calendar.getInstance();
			gpsDate.setTime(lGPSDataModel.getGpsdate());
		
			gprsDate = Calendar.getInstance();
			if(lGPSDataModel.getGprsdate() != null)
				gprsDate.setTime(lGPSDataModel.getGprsdate());
			int latitude = 0; 
			int longitude = 0;
			if(lGPSDataModel.getLatitude() != 0 && lGPSDataModel.getLongitude() != 0){
				latitude = (int) ((lGPSDataModel.getLatitude()) * Double.valueOf(1000000.00)); 
				longitude = (int) ((lGPSDataModel.getLongitude()) * Double.valueOf(1000000.00));
			}
			this.latitude = latitude;
			this.longitude = longitude;
			this.speed = lGPSDataModel.getSpeed();
			this.direction = lGPSDataModel.getDirection();
			this.acc = lGPSDataModel.isAcc();
			this.mileage = lGPSDataModel.getMileage();
			if(lGPSDataModel.isCanBusDataAvailable())
			{
				this.isCanBusDataAvailable = true;
				this.lCanModelList =lGPSDataModel.lCanModelList;
			}
			lbambooDataList.add(this);
			if(lbambooDataList.size() > 0)
				return lbambooDataList.get(0);
			else
				return null;
		}
		catch(Exception e)
		{
			log.error(e.getMessage());
			log.error("Error processing Location Header",e);
			return null;
		}
	}

}
