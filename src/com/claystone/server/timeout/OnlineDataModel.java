package com.claystone.server.timeout;

public class OnlineDataModel {
	private double longitude ;  
	private double latitude ;
	private int speed ;  
	private String dttime ; 
	private int ignition ;  
	private String vehicle_name ; 
	private int icon ;
	public double getLongitude() {
		return longitude;
	}
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	public double getLatitude() {
		return latitude;
	}
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	public int getSpeed() {
		return speed;
	}
	public void setSpeed(int speed) {
		this.speed = speed;
	}
	public String getDttime() {
		return dttime;
	}
	public void setDttime(String dttime) {
		this.dttime = dttime;
	}
	public int getIgnition() {
		return ignition;
	}
	public void setIgnition(int ignition) {
		this.ignition = ignition;
	}
	public String getVehicle_name() {
		return vehicle_name;
	}
	public void setVehicle_name(String vehicle_name) {
		this.vehicle_name = vehicle_name;
	}
	public int getIcon() {
		return icon;
	}
	public void setIcon(int icon) {
		this.icon = icon;
	}	
}
