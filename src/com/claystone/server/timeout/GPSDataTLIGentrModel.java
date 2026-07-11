package com.claystone.server.timeout;

import java.util.Date;

public class GPSDataTLIGentrModel {

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
	private String temperature;
	private String gpsStatus;
	private String door1;
	private String door2;
	private String door3;
	private String door4;
	private String vehicleType;
	private String status;
	private String batteryPercentage;
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
	public boolean isGpsstatus() {
		return gpsstatus;
	}
	public void setGpsstatus(boolean gpsstatus) {
		this.gpsstatus = gpsstatus;
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
	public String getIgnition() {
		return ignition;
	}
	public void setIgnition(String ignition) {
		this.ignition = ignition;
	}
	public String getLicensePlate() {
		return licensePlate;
	}
	public void setLicensePlate(String licensePlate) {
		this.licensePlate = licensePlate;
	}
	public String getTemperature() {
		return temperature;
	}
	public void setTemperature(String temperature) {
		this.temperature = temperature;
	}
	public String getGpsStatus() {
		return gpsStatus;
	}
	public void setGpsStatus(String gpsStatus) {
		this.gpsStatus = gpsStatus;
	}
	public String getDoor1() {
		return door1;
	}
	public void setDoor1(String door1) {
		this.door1 = door1;
	}
	public String getDoor2() {
		return door2;
	}
	public void setDoor2(String door2) {
		this.door2 = door2;
	}
	public String getDoor3() {
		return door3;
	}
	public void setDoor3(String door3) {
		this.door3 = door3;
	}
	public String getDoor4() {
		return door4;
	}
	public void setDoor4(String door4) {
		this.door4 = door4;
	}
	public String getVehicleType() {
		return vehicleType;
	}
	public void setVehicleType(String vehicleType) {
		this.vehicleType = vehicleType;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getBatteryPercentage() {
		return batteryPercentage;
	}
	public void setBatteryPercentage(String batteryPercentage) {
		this.batteryPercentage = batteryPercentage;
	}
	
}
