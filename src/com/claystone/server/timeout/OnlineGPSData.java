package com.claystone.server.timeout;

public class OnlineGPSData {
	private double longitude;
    private double latitude;
    private int speed;
    private long date;
    private String ignitionStatus;
    private double odoDistance;
    private String shortName;

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
	public long getDate() {
		return date;
	}
	public void setDate(long date) {
		this.date = date;
	}
	public String getIgnitionStatus() {
		return ignitionStatus;
	}
	public void setIgnitionStatus(String ignitionStatus) {
		this.ignitionStatus = ignitionStatus;
	}
	public double getOdoDistance() {
		return odoDistance;
	}
	public void setOdoDistance(double odoDistance) {
		this.odoDistance = odoDistance;
	}
	public String getShortName() {
		return shortName;
	}
	public void setShortName(String shortName) {
		this.shortName = shortName;
	}
}
