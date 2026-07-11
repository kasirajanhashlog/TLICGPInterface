package com.claystone.server.timeout;

import java.util.ArrayList;
import java.util.Date;

public class GPSDataModel {
	private String mobileunitid;
	private Date gpsdate;
	private Date gprsdate;
	private boolean gpsstatus;
	private double latitude;
	private double longitude;
	private short speed;
	private short direction;
	private boolean acc; 
	private Short eventcode; 
	private Short batt; 
	private int mileage;
	private String ignition;
	private String licensePlate;
	private boolean isCanBusDataAvailable;
	public ArrayList<CanBusModel> lCanModelList;
	public boolean isGpsstatus() {
		return gpsstatus;
	}
	public void setGpsstatus(boolean gpsstatus) {
		this.gpsstatus = gpsstatus;
	}

	public short getSpeed() {
		return speed;
	}
	public void setSpeed(short speed) {
		this.speed = speed;
	}
	public short getDirection() {
		return direction;
	}
	public void setDirection(short direction) {
		this.direction = direction;
	}
	public boolean isAcc() {
		return acc;
	}
	public void setAcc(boolean acc) {
		this.acc = acc;
	}
	public Short getEventcode() {
		return eventcode;
	}
	public void setEventcode(Short eventcode) {
		this.eventcode = eventcode;
	}
	public Short getBatt() {
		return batt;
	}
	public void setBatt(Short batt) {
		this.batt = batt;
	}
	public int getMileage() {
		return mileage;
	}
	public void setMileage(int mileage) {
		this.mileage = mileage;
	}
	public String getMobileunitid() {
		return mobileunitid;
	}
	public void setMobileunitid(String mobileunitid) {
		this.mobileunitid = mobileunitid;
	}
	public Date getGpsdate() {
		return gpsdate;
	}
	public void setGpsdate(Date gpsdate) {
		this.gpsdate = gpsdate;
	}
	public Date getGprsdate() {
		return gprsdate;
	}
	public void setGprsdate(Date gprsdate) {
		this.gprsdate = gprsdate;
	}
	public String getIgnition() {
		return ignition;
	}
	public void setIgnition(String Ignition) {
		ignition = Ignition;
	}
	public double getLatitude() {
		return latitude;
	}
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	public double getLongitude() {
		return longitude;
	}
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	public String getLicensePlate() {
		return licensePlate;
	}
	public void setLicensePlate(String licensePlate) {
		this.licensePlate = licensePlate;
	}	
	public boolean isCanBusDataAvailable() {
		return isCanBusDataAvailable;
	}
	public void setCanBusDataAvailable(boolean isCanBusDataAvailable) {
		this.isCanBusDataAvailable = isCanBusDataAvailable;
	}
	public ArrayList<CanBusModel> getlCanModelList() {
		return lCanModelList;
	}
	public void setlCanModelList(ArrayList<CanBusModel> lCanModelList) {
		this.lCanModelList = lCanModelList;
	}
	
	
}
