package com.claystone.server.timeout;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import com.claystone.db.Gpsdata;
import com.claystone.server.util.HibernateUtil;

public class InsertGpsData {
	private Logger	log;
	private Connection connection;
	private PreparedStatement pstmt;
	public   InsertGpsData() {
		log = Logger.getLogger(InsertGpsData.class);
		try {
			//10.67.75.12
			Class.forName("org.postgresql.Driver");
			/*
			 * connection = DriverManager.getConnection(
			 * "jdbc:postgresql://10.67.75.12:5432/vTrack", "postgres", "disnlog");
			 */
			connection = DriverManager.getConnection(
					"jdbc:postgresql://10.9.112.3:5432/vTrack", "postgres", "Hashlogdbadm@2021");
//			connection = DriverManager.getConnection(
//					"jdbc:postgresql://34.93.251.117:5432/vTrack-restore1", "postgres", "Hashlogdbadm@2021");
			
			String stm = "INSERT INTO gpsdata ( mobileunitid, gpsdate, gprsdate, gpsstatus,"
					+ " latitude, longitude, speed, direction, acc, in1, in2, in3, in4, out1,"
					+ " out2, eventcode, adc1, adc2, batt, mileage, reserve, processed_flag )"
					+ " VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? );";
			pstmt = connection.prepareStatement(stm);
		}catch(Exception e){
			log.error("Connecting database failed due to - " + e.getMessage());
			e.printStackTrace();
			return;
		}
	}

	public boolean InsertAPIGPSdata(ArrayList<GPSDataModel> gpsdataList) {
		try {
			for(int i=0; i< gpsdataList.size(); i++){
				GPSDataModel model = gpsdataList.get(i); 
				Date gpsdate = model.getGpsdate();

				DateFormat formatstr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
				String gpsdateStr = formatstr.format(gpsdate);

				Session session = HibernateUtil.beginTransaction();
				ArrayList<Gpsdata> gpsdataCheck = HibernateUtil.castList(Gpsdata.class, session.createQuery(" from Gpsdata"
						+ " where id.mobileunitid = '" + model.getMobileunitid() + "'" +
						" and id.gpsdate = '" + gpsdateStr +"'").list());

				HibernateUtil.commit();
				if(gpsdataCheck.size() <= 0){
					InsertIntoGpsData(model.getMobileunitid(), gpsdate, new Date(), model.getLatitude(),
							model.getLongitude(),  model.getSpeed(), model.getMileage(), 0, model.isAcc());
				}else{
					log.info("Gpsdata already inserted for : " + model.getMobileunitid() + "/" + gpsdateStr);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
			return false;
		}
		return true;

	}
	public boolean InsertIntoGpsData(String unitId,Date gpsdate , Date gprsdate, double lat, double lon,
			int speed, int mileage, int direction, boolean acc){
		//String unitId = GetMobileUnitID(imeiCode);
		java.sql.Timestamp tsGpsdate = new java.sql.Timestamp(gpsdate.getTime());
		java.sql.Timestamp tsGprsdate = new java.sql.Timestamp(gprsdate.getTime());
		try{
			int latitude = (int) ((lat) * Double.valueOf(1000000.00)); 
			int longitude = (int) ((lon) * Double.valueOf(1000000.00));
			boolean gpsStatus = true;
			if(lat == 0 && lon == 0){
				gpsStatus = false;
			}
			///boolean acc = false;

			pstmt.setString(1,unitId);
			pstmt.setTimestamp(2, tsGpsdate);
			pstmt.setTimestamp(3, tsGprsdate);
			pstmt.setBoolean(4, gpsStatus);
			pstmt.setInt(5, latitude);
			pstmt.setInt(6, longitude);
			pstmt.setInt(7, speed);
			pstmt.setInt(8, direction);
			pstmt.setBoolean(9, acc);
			pstmt.setBoolean(10, false);
			pstmt.setBoolean(11, false);
			pstmt.setBoolean(12, false);
			pstmt.setBoolean(13, false);
			pstmt.setBoolean(14, false);
			pstmt.setBoolean(15, false);
			pstmt.setInt(16, 0);
			pstmt.setInt(17, 0);
			pstmt.setInt(18, 0);
			pstmt.setInt(19, 0);
			pstmt.setInt(20, mileage);
			pstmt.setString(21,"NO");
			pstmt.setBoolean(22, false);
			pstmt.executeUpdate(); 
			//		connection.commit();
			log.info("Gpsdata inserted for : " + unitId + "/" + tsGpsdate);
		} catch (SQLException e) {
			log.error("mobileunit - " +  unitId
					+ ", gpsdate - " + tsGpsdate + "; inserting into gpsdata failed due to - " 
					+ e.getMessage());
			e.printStackTrace();
			//		connection.rollback();
			return false;
		}
		return true;
	}
}
