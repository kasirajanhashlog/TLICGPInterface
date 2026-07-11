/*
 * BambooDB.java
 *

 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.claystone.server.timeout;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import org.apache.log4j.Logger;

import com.claystone.common.utils.CommonConstants;

/**
 * 
 * @author Administrator
 */
public class BambooDB implements Runnable {

	public boolean isLocationData = true;
	public int eventCode;
	public static int insert_count=0;
	private static final String insertBambooGPSData = "INSERT INTO gpsdata ( mobileunitid, gpsdate, gprsdate, gpsstatus,"
			+ " latitude, longitude, speed, direction, acc, in1, in2, in3, in4, out1,"
			+ " out2, eventcode, adc1, adc2, batt, mileage, reserve, processed_flag )"
			+ " VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? );";
	private static final String insertBambooCanbusGPSData = "INSERT INTO canbus_gpsdata (mobileunitid,gpsdate,gprsdate,output_1,output_2,output_3,"
					+ "output_4,output_5,output_6,output_7,output_8,output_9,output_10,output_11,output_12,output_13,output_14,output_15,"
					+ "output_16,output_17,output_18,output_19,output_20,output_21,output_22,output_23,output_24,output_25,output_26,output_27,"
					+ "output_28,output_29,output_30,output_31,output_32,output_33,output_34,output_35,output_36,output_37,output_38,output_39,"
					+ "output_40,output_41,output_42,output_43,output_44,output_45,output_46,output_47,output_48,output_49,output_50,output_51,"
					+ "output_52,output_53,output_54,output_55,output_56,output_57,output_58,output_59,output_60,output_61,output_62,output_63,"
					+ "reserve,processed_flag) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,"
					+ "?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
	
	private Connection connBAMBOO; // connGIS, connGPRS, connGoogleData,
	private Timestamp gpsDate, gprsDate;
	private BambooData bamboodata;
	private Logger log = Logger.getLogger(BambooDB.class);

	public BambooDB(BambooData bamboodata) {
		this.bamboodata = bamboodata;
	}

	private boolean storeBamboo(Connection connBAMBOO) {

		log.info("Bamboo process start: "
				+ bamboodata.mobileUnitID + " - Thread Name : " +  Thread.currentThread().getName() );

		if (bamboodata.mobileUnitID.equals("")) {
			log.info("Invalid UnitID");			
			return true;
		}

		PreparedStatement pstmt = null;
		try {			
			connBAMBOO.setAutoCommit(false);


			java.sql.Timestamp tsGpsdate = new java.sql.Timestamp(bamboodata.gpsDate.getTime().getTime());
			java.sql.Timestamp tsGprsdate = new java.sql.Timestamp(bamboodata.gprsDate.getTime().getTime());

			boolean gpsStatus = true;
			if(bamboodata.latitude == 0 && bamboodata.longitude == 0){
				gpsStatus = false;
			}
			int eventCode = 0;
			if(bamboodata.eventCode == CommonConstants.EVENT_CODE_SIM_GPSDATA_ULIP) {
				eventCode = CommonConstants.EVENT_CODE_SIM_GPSDATA_ULIP;
			}
			///boolean acc = false;
			pstmt = connBAMBOO.prepareStatement(insertBambooGPSData);
			pstmt.setString(1,bamboodata.mobileUnitID);
			pstmt.setTimestamp(2, tsGpsdate);
			pstmt.setTimestamp(3, tsGprsdate);
			pstmt.setBoolean(4, gpsStatus);
			pstmt.setInt(5, bamboodata.latitude);
			pstmt.setInt(6, bamboodata.longitude);
			pstmt.setInt(7, bamboodata.speed);
			pstmt.setInt(8, bamboodata.direction);
			pstmt.setBoolean(9, bamboodata.acc);
			pstmt.setBoolean(10, false);
			pstmt.setBoolean(11, false);
			pstmt.setBoolean(12, false);
			pstmt.setBoolean(13, false);
			pstmt.setBoolean(14, false);
			pstmt.setBoolean(15, false);
//			pstmt.setInt(16, 0);
			pstmt.setInt(16, eventCode);
			pstmt.setInt(17, 0);
			pstmt.setInt(18, 0);
			pstmt.setInt(19, 0);
			pstmt.setInt(20, bamboodata.mileage);
			pstmt.setString(21,"NO");
			pstmt.setBoolean(22, false);
			pstmt.executeUpdate(); 
			//		connection.commit();
			log.info("Gpsdata inserted for : " + bamboodata.mobileUnitID + "/" + tsGpsdate);
			//}
			connBAMBOO.commit();
			try {
				if(pstmt != null)
				{
					pstmt.close();
				}
			} catch (SQLException ex) {
				log.error("SQL Exception",ex);
			}
			
			if(bamboodata.isCanBusDataAvailable)
			{
				pstmt = null;
				String str = "";
				String strOutput1 = "";
				String strOutput2 = "";
				String strOutput3 = "";
				String strOutput4 = "";
				String strOutput5 = "";
				String strOutput6 = "";
				String strOutput7 = "";
				String strOutput8 = "";
				String strOutput9 = "";
				String strOutput10 = "";
				String strOutput11 = "";
				String strOutput12 = "";
				String strOutput13 = "";
				String strOutput14 = "";
				String strOutput15 = "";
				String strOutput16 = "";
				String strOutput17 = "";
				String strOutput18 = "";
				String strOutput19 = "";
				String strOutput20 = "";
				String strOutput21 = "";
				String strOutput22 = "";
				String strOutput23 = "";
				String strOutput24 = "";
				String strOutput25 = "";
				String strOutput26 = "";
				String strOutput27 = "";
				String strOutput28 = "";
				String strOutput29 = "";
				String strOutput30 = "";
				String strOutput31 = "";
				String strOutput32 = "";
				String strOutput33 = "";
				String strOutput34 = "";
				String strOutput35 = "";
				
				ArrayList<CanBusModel> lCanbusModel = bamboodata.lCanModelList;
				for (int i = 0; i < lCanbusModel.size(); i++) {
					CanBusModel lParam = lCanbusModel.get(i);

					if (lParam.getZtVarId().equals("output_1")) {
						strOutput1 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_2")) {
						strOutput2 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_3")) {
						strOutput3 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_4")) {
						strOutput4 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_5")) {
						strOutput5 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_6")) {
						strOutput6 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_7")) {
						strOutput7 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_8")) {
						strOutput8 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_9")) {
						strOutput9 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_10")) {
						strOutput10 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_11")) {
						strOutput11 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_12")) {
						strOutput12 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_13")) {
						strOutput13 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_14")) {
						strOutput14 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_15")) {
						strOutput15 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_16")) {
						strOutput16 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_17")) {
						strOutput17 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_18")) {
						strOutput18 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_19")) {
						strOutput19 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_20")) {
						strOutput20 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_21")) {
						strOutput21 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_22")) {
						strOutput22 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_23")) {
						strOutput23 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_24")) {
						strOutput24 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_25")) {
						strOutput25 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_26")) {
						strOutput26 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_27")) {
						strOutput27 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_28")) {
						strOutput28 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_29")) {
						strOutput29 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_30")) {
						strOutput30 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_31")) {
						strOutput31 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_32")) {
						strOutput32 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_33")) {
						strOutput33 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_34")) {
						strOutput34 = lParam.getParamOutputValue();
					}
					if (lParam.getZtVarId().equals("output_35")) {
						strOutput35 = lParam.getParamOutputValue();
					}
				}
				try {			
					connBAMBOO.setAutoCommit(false);
					pstmt = connBAMBOO.prepareStatement(insertBambooCanbusGPSData);					
					pstmt.setString(1, bamboodata.mobileUnitID);
					pstmt.setTimestamp(2, tsGpsdate);
					pstmt.setTimestamp(3, tsGprsdate);
					pstmt.setString(4, strOutput1);
					pstmt.setString(5, strOutput2);
					pstmt.setString(6, strOutput3);
					pstmt.setString(7, strOutput4);
					pstmt.setString(8, strOutput5);
					pstmt.setString(9, strOutput6);
					pstmt.setString(10, strOutput7);
					pstmt.setString(11, strOutput8);
					pstmt.setString(12, strOutput9);
					pstmt.setString(13, strOutput10);
					pstmt.setString(14, strOutput11);
					pstmt.setString(15, strOutput12);
					pstmt.setString(16, strOutput13);
					pstmt.setString(17, strOutput14);
					pstmt.setString(18, strOutput15);
					pstmt.setString(19, strOutput16);
					pstmt.setString(20, strOutput17);
					pstmt.setString(21, strOutput18);
					pstmt.setString(22, strOutput19);
					pstmt.setString(23, strOutput20);
					pstmt.setString(24, strOutput21);
					pstmt.setString(25, strOutput22);
					pstmt.setString(26, strOutput23);
					pstmt.setString(27, strOutput24);
					pstmt.setString(28, strOutput25);
					pstmt.setString(29, strOutput26);
					pstmt.setString(30, strOutput27);
					pstmt.setString(31, strOutput28);
					pstmt.setString(32, strOutput29);
					pstmt.setString(33, strOutput30);
					pstmt.setString(34, strOutput31);
					pstmt.setString(35, strOutput32);
					pstmt.setString(36, strOutput33);
					pstmt.setString(37, strOutput34);
					pstmt.setString(38, strOutput35);
					pstmt.setString(39, str);
					pstmt.setString(40, str);
					pstmt.setString(41, str);
					pstmt.setString(42, str);
					pstmt.setString(43, str);
					pstmt.setString(44, str);
					pstmt.setString(45, str);
					pstmt.setString(46, str);
					pstmt.setString(47, str);
					pstmt.setString(48, str);
					pstmt.setString(49, str);
					pstmt.setString(50, str);
					pstmt.setString(51, str);
					pstmt.setString(52, str);
					pstmt.setString(53, str);
					pstmt.setString(54, str);
					pstmt.setString(55, str);
					pstmt.setString(56, str);
					pstmt.setString(57, str);
					pstmt.setString(58, str);
					pstmt.setString(59, str);
					pstmt.setString(60, str);
					pstmt.setString(61, str);
					pstmt.setString(62, str);
					pstmt.setString(63, str);
					pstmt.setString(64, str);
					pstmt.setString(65, str);
					pstmt.setString(66, str);
					pstmt.setString(67, "NO");
					pstmt.setBoolean(68, false); 
					pstmt.executeUpdate(); 
					//		connection.commit();
					log.info("Can Gpsdata inserted for : " + bamboodata.mobileUnitID + "/" + tsGpsdate);
					//}
					connBAMBOO.commit();
					try {
						if(pstmt != null)
						{
							pstmt.close();
						}
					} catch (SQLException ex) {
						log.error("mobile unit - " + bamboodata.mobileUnitID
								+ "; Error when setting gpsdatas into prepareStatement: " + ex.getMessage());
						ex.printStackTrace();
					}
				} catch (SQLException ex) {
					log.error("SQL Exception",ex);
				}
			}
			

		} catch (Exception E) {

			log.error(bamboodata.mobileUnitID + " : DB Exception ",E);
			return false;
		}
		return true;
	}


	private java.sql.Timestamp getSQLTimestamp(Calendar dateTime) {
		java.sql.Timestamp tStamp = new java.sql.Timestamp(dateTime
				.getTimeInMillis());          
		return tStamp;

	}

	public void run() {
		boolean dataInserted = false;
		log.info("Inside DB Pool - UnitID :  " + bamboodata.mobileUnitID + " - " + Thread.currentThread().getName());
		log.info(" Longitude : " +  bamboodata.longitude + " Latitude : " +  bamboodata.latitude);

		if (bamboodata.mobileUnitID.equals("")
				|| bamboodata.mobileUnitID.equals("0")) {
		} else {
			try {	
				if(isLocationData == true)
				{
					gpsDate = getSQLTimestamp(bamboodata.gpsDate);
				}
				gprsDate = getSQLTimestamp(bamboodata.gprsDate);
				connBAMBOO = DatabasePoolManager.getConnection();
				log.info(bamboodata.mobileUnitID + " : Before data insert - Timestamps : " + gpsDate + " - " + gprsDate);

				dataInserted = storeBamboo(connBAMBOO);
			} catch (Exception ex1) {
				log.error("",ex1);

			}
		}
		try {
			if(connBAMBOO != null)
			{
				connBAMBOO.close();
			}
		} catch (SQLException ex) {
			log.error(ex.getMessage());

		}
	}

}
