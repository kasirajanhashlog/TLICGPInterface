package com.claystone.common.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;

import com.claystone.db.Vehicle;
import com.claystone.db.VehicleMobileUnit;
import com.claystone.server.db.CompanyStationNew;
import com.claystone.server.util.HibernateUtil;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class CommonMethods {
	private static Logger log = Logger.getLogger(CommonMethods.class);

	/* *********************************************
	 *  get license plate from vehicle id
	 * *********************************************/
	public static String getLicensePlate(int companyId, int vehicleId, String mobileUnitId) {
		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
		try {
			List vehicleList = session.createQuery(" from Vehicle " + 
					" where id.vehicleId = " + vehicleId
					+ " and id.companyId = " + companyId).list();

			if (vehicleList.size() > 0) {
				Vehicle vehicle = (Vehicle) vehicleList.get(0);
				return vehicle.getLicensePlate();
			}
		}catch(Exception e) {
			// TODO Auto-generated catch block
			log.error(e.getMessage());
			HibernateUtil.rollback();
			e.printStackTrace();
		}
		// vehcile not found. so, mobile unit id will be considered for license plate
		return mobileUnitId;	
	}


	/* ******************************************************
	 * This method finds the location address (in the format 
	 * like 5 kms from Oragadam) from latitude and longitude
	 * values. ie. the location address is with reference to 
	 * the company's stations. if it can not find that location 
	 * just gets the google address.
	 * ******************************************************/
	public static String getLocationAddress(int latitude, int longitude, int companyId) {
		try{
			String locAddress = null;
			Session session = HibernateUtil.getSessionFactory().getCurrentSession();   
			double lat = latitude/1000000.00;
			double lon = longitude/1000000.00;			

			List gpsList = session.createSQLQuery("select stat_name , id from fn_get_station( " +
					+ lon + "," + lat + "," + companyId + " )").list();

			int stationId = 0;
			if(gpsList.size() > 0){
				Object row[] = (Object[])gpsList.get(0);
				String location = (String)row[0];
				if(location != null){
					stationId = (Integer)row[1];	
					if(stationId == 174 ||		// 174 - ford
							stationId == 579 || // 579 - daimler
							stationId == 1732 || // 1732 - toyota NPC	
							stationId == 2134 || // 2134 - TLI NPC
							stationId == 2160){	// 2160 - ARC NPC
					}else{						
						locAddress = location;
						//  for toyota,TLI and ARC, state name also added after company station name
						if(companyId == 163 || companyId == 189 || companyId == 191){
							List gpsLocationList = session.createSQLQuery("select level1_e,level2_e,level3_e from fn_get_location( " +
									+ lon + "," + lat + " )").list();	
							if(gpsLocationList.size() > 0 ){
								row = (Object[])gpsLocationList.get(0);
								if((String)row[0] != null){
									locAddress = locAddress + " / " + (String)row[0];
								}
							}
						}
					}
				}			
			}
			if(locAddress == null){
				List gpsLocationList = session.createSQLQuery("select level1_e,level2_e,level3_e from fn_get_location( " +
						+ lon + "," + lat + " )").list();		

				if(gpsLocationList.size() > 0 ){
					Object row[] = (Object[])gpsLocationList.get(0);
					if((String)row[2]!= null&& ((String)row[2]).trim().contains("KMs from")){
						locAddress = (String)row[2];
						//  for toyota,TLI and ARC, state name also added after company station name
						if(companyId == 163 || companyId == 189 || companyId == 191){
							if((String)row[0] != null){
								locAddress = locAddress + " / " + (String)row[0];
							}						
						}
					}else{
						//added by mamallan on 21-07-2016 because to avoid the area name for NPC (Toyota, TLI, ARC)
						if(stationId == 1732 || stationId == 2134 || stationId == 2160){
							if((String)row[2] != null){
								locAddress = ((String)row[2]).split("/")[0];								
							}

							if(locAddress == null || locAddress.equals("")){
								locAddress = "-";
							}

							locAddress = locAddress + " / ";
							if((String)row[0] != null){
								locAddress = locAddress + (String)row[0];

							}else{
								locAddress = locAddress + "-";
							}
						}else{ // added by mamallan -End
							if((String)row[2]== null){
								locAddress = "-";
							}else{
								locAddress = (String)row[2];
							}

							locAddress = locAddress + " / ";
							if((String)row[1]!= null){
								locAddress = locAddress + (String)row[1];

							}else{
								locAddress = locAddress + "-";
							}

							locAddress = locAddress + " / ";
							if((String)row[0] != null){
								locAddress = locAddress + (String)row[0];

							}else{
								locAddress = locAddress + "-";
							}
						}
					}
				}
			}
			return locAddress;
		}catch(Exception e){
			log.error(e.getMessage());
			e.printStackTrace();
			HibernateUtil.rollback();
			return null;
		}
	}

	/* ******************************************************
	 * This method checks whether the Gpsdata lies in any of 
	 * the company's stations. if it exists returns station record
	 * ******************************************************/		
	public static CompanyStationNew GetStationIfInStation(String mobileUnitId, int latitude, 
			int longitude, ArrayList<CompanyStationNew> companyStations){
		try{
			/*		// get all the company stations				//   commented out by vijay on 07-04-2015
			Session session = HibernateUtil.getSessionFactory().getCurrentSession();	
			ArrayList<CompanyStationNew> companyStations = new ArrayList<CompanyStationNew>(
						session.createQuery("from CompanyStationNew where" +
								" companyId = " + companyId + " and recordStatus = " + 
								CommonConstants.RECORD_STATUS_ACTIVE).list());	*/
			// in each station, check whether gpsdata lies there 
			for(int i = 0; i < companyStations.size(); ++i){
				CompanyStationNew compStn = companyStations.get(i);	
				String stationName = compStn.getStationName();
				Geometry stnGeometry = compStn.getStationGeom();  // geometry of this station	

				if(stnGeometry != null){
					// station geofence created
					Coordinate vehicleCoordinate = new Coordinate();
					vehicleCoordinate.y = latitude/1000000.0;
					vehicleCoordinate.x = longitude/1000000.0;
					Geometry vehicleGeom = new GeometryFactory().createPoint(vehicleCoordinate);
					if(stnGeometry.contains(vehicleGeom)){ //Station Geofence Matched
						log.info("mobile unit: " + mobileUnitId + " is in station: " + stationName);
						return compStn;
					}
				}else{
					// station geofence not created
					log.info("station geofence not created - station name: " + stationName);
				}
			}
			log.info("mobile unit: " + mobileUnitId + " is outside all the stations of the company");
			return null;
		}catch(Exception e){
			log.error(e.getMessage());
			e.printStackTrace();
			HibernateUtil.rollback();
			return null;
		}
	}

	public static String getVehicleMobileUnitId(int companyId, String licensePlate) {
		String vehicleMUnit="";
		Session session = HibernateUtil.beginTransaction();
		try {
			List vehicleList = session.createQuery(" from Vehicle " + 
					" where licensePlate = '" + licensePlate+"'"
					+ " and id.companyId = " + companyId
					+ " and recordStatus="+ CommonConstants.RECORD_STATUS_ACTIVE).list();

			if (vehicleList.size() > 0) {
				Vehicle vehicle = (Vehicle) vehicleList.get(0);
				List mobileUnitList = session.createQuery(" from VehicleMobileUnit " +
						 " where vehicle_id = " + vehicle.getId().getVehicleId()
						+ " and companyId = "+companyId ).list();
				if(mobileUnitList.size() > 0) {
					VehicleMobileUnit vehicleMobileUnit = (VehicleMobileUnit) mobileUnitList.get(0);
					vehicleMUnit = vehicleMobileUnit.getId().getMobileunitid();
				}
				
			}
			HibernateUtil.commit();
		}catch(HibernateException e) {
			log.error(e.getMessage());
			HibernateUtil.rollback();
			e.printStackTrace();
		}
		// vehcile not found. so, mobile unit id will be considered for license plate
		return vehicleMUnit;	
	}
}
