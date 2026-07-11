package com.claystone.server.timeout;

public class PaizoGpsDataModel {

	private String RegNo;
	private String Time;
	private double Lat;
	private double Lng;
	private short Speed;
	private long Odometer;
	private String Ignition;
	
	public String getRegNo() {
		return RegNo;
	}
	public void setRegNo(String regNo) {
		RegNo = regNo;
	}
	public String getTime() {
		return Time;
	}
	public void setTime(String time) {
		Time = time;
	}
	public double getLat() {
		return Lat;
	}
	public void setLat(double lat) {
		Lat = lat;
	}
	public double getLng() {
		return Lng;
	}
	public void setLng(double lng) {
		Lng = lng;
	}
	public short getSpeed() {
		return Speed;
	}
	public void setSpeed(short speed) {
		Speed = speed;
	}
	public long getOdometer() {
		return Odometer;
	}
	public void setOdometer(long odometer) {
		Odometer = odometer;
	}
	public String getIgnition() {
		return Ignition;
	}
	public void setIgnition(String ignition) {
		Ignition = ignition;
	}

	
	
}
