package com.claystone.server.timeout;

import java.util.Date;

public class TataMotorSnapShotModel {
	private String mobileunitid;
	private String messageType;
	private Date eventDateTime;
	private double gpsLatitude;
	private double gpsLongitude;
	private double gpsAltitude;
	private int gpsCourseInDegrees;
	private double accelX;
	private double accelY;
	private double accelZ;
	private boolean ignitionOn;
	private boolean crankOn;
	private int speed;
	private long odometer;
	private int fuel;
	private double gyroX;
	private double gyroY;
	private double gyroZ;
	private String registrationNumber;
	private String imei;
	public String getMobileunitid() {
		return mobileunitid;
	}
	public void setMobileunitid(String mobileunitid) {
		this.mobileunitid = mobileunitid;
	}
	public String getMessageType() {
		return messageType;
	}
	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}
	public Date getEventDateTime() {
		return eventDateTime;
	}
	public void setEventDateTime(Date eventDateTime) {
		this.eventDateTime = eventDateTime;
	}
	public double getGpsLatitude() {
		return gpsLatitude;
	}
	public void setGpsLatitude(double gpsLatitude) {
		this.gpsLatitude = gpsLatitude;
	}
	public double getGpsLongitude() {
		return gpsLongitude;
	}
	public void setGpsLongitude(double gpsLongitude) {
		this.gpsLongitude = gpsLongitude;
	}
	public double getGpsAltitude() {
		return gpsAltitude;
	}
	public void setGpsAltitude(double gpsAltitude) {
		this.gpsAltitude = gpsAltitude;
	}
	public int getGpsCourseInDegrees() {
		return gpsCourseInDegrees;
	}
	public void setGpsCourseInDegrees(int gpsCourseInDegrees) {
		this.gpsCourseInDegrees = gpsCourseInDegrees;
	}
	public double getAccelX() {
		return accelX;
	}
	public void setAccelX(double accelX) {
		this.accelX = accelX;
	}
	public double getAccelY() {
		return accelY;
	}
	public void setAccelY(double accelY) {
		this.accelY = accelY;
	}
	public double getAccelZ() {
		return accelZ;
	}
	public void setAccelZ(double accelZ) {
		this.accelZ = accelZ;
	}
	public boolean isIgnitionOn() {
		return ignitionOn;
	}
	public void setIgnitionOn(boolean ignitionOn) {
		this.ignitionOn = ignitionOn;
	}
	public boolean isCrankOn() {
		return crankOn;
	}
	public void setCrankOn(boolean crankOn) {
		this.crankOn = crankOn;
	}
	public int getSpeed() {
		return speed;
	}
	public void setSpeed(int speed) {
		this.speed = speed;
	}
	public long getOdometer() {
		return odometer;
	}
	public void setOdometer(long odometer) {
		this.odometer = odometer;
	}
	public int getFuel() {
		return fuel;
	}
	public void setFuel(int fuel) {
		this.fuel = fuel;
	}
	public double getGyroX() {
		return gyroX;
	}
	public void setGyroX(double gyroX) {
		this.gyroX = gyroX;
	}
	public double getGyroY() {
		return gyroY;
	}
	public void setGyroY(double gyroY) {
		this.gyroY = gyroY;
	}
	public double getGyroZ() {
		return gyroZ;
	}
	public void setGyroZ(double gyroZ) {
		this.gyroZ = gyroZ;
	}
	public String getRegistrationNumber() {
		return registrationNumber;
	}
	public void setRegistrationNumber(String registrationNumber) {
		this.registrationNumber = registrationNumber;
	}
	public String getImei() {
		return imei;
	}
	public void setImei(String imei) {
		this.imei = imei;
	}

}
