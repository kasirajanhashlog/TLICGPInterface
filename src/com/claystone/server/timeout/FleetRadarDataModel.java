package com.claystone.server.timeout;

public class FleetRadarDataModel {
	private String gpsDateTime;
	private String plateNo;
	private double latitude;
	private double longitude;
	private String location;
	private String ignition;
	private short speed;
	private int distance;
	private int distance24Hour;
	private String transporterName;
	public String getGpsDateTime() {
		return gpsDateTime;
	}
	public void setGpsDateTime(String gpsDateTime) {
		this.gpsDateTime = gpsDateTime;
	}
	public String getPlateNo() {
		return plateNo;
	}
	public void setPlateNo(String plateNo) {
		this.plateNo = plateNo;
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
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public String getIgnition() {
		return ignition;
	}
	public void setIgnition(String ignition) {
		this.ignition = ignition;
	}
	public short getSpeed() {
		return speed;
	}
	public void setSpeed(short speed) {
		this.speed = speed;
	}
	public int getDistance() {
		return distance;
	}
	public void setDistance(int distance) {
		this.distance = distance;
	}
	public int getDistance24Hour() {
		return distance24Hour;
	}
	public void setDistance24Hour(int distance24Hour) {
		this.distance24Hour = distance24Hour;
	}
	public String getTransporterName() {
		return transporterName;
	}
	public void setTransporterName(String transporterName) {
		this.transporterName = transporterName;
	}
	
}
