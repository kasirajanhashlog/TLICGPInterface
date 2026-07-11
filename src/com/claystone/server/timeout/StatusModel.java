package com.claystone.server.timeout;

import java.util.ArrayList;

public class StatusModel {
	private int status;
	private String message;
	private ArrayList<OnlineDataModel> result;
	private ArrayList<OnlineGPSData> onlineGPSDatas;
	public int getStatus() {
		return status;
	}
	public String getMessage() {
		return message;
	}
	public ArrayList<OnlineDataModel> getResult() {
		return result;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public void setResult(ArrayList<OnlineDataModel> result) {
		this.result = result;
	}
	public ArrayList<OnlineGPSData> getOnlineGPSDatas() {
		return onlineGPSDatas;
	}
	public void setOnlineGPSDatas(ArrayList<OnlineGPSData> onlineGPSDatas) {
		this.onlineGPSDatas = onlineGPSDatas;
	}
}
