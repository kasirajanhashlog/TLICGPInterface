package com.claystone.server.timeout;

import java.util.ArrayList;
import java.util.Date;

public class MobileUnitTimestamp {
	private int companyId;
	private String mobileUnitId;
	private String licensePlate;
	private Date lastProcessedTime;
	private boolean processedFlag;
	private int  lastResponseCode;
	private Date lastResponseTime;
	//Dataprovider field
	private String dataProviderName;
	private String tokenUrl;
	private String apiUrl;
	private String tokenReqParams;
	private String apiReqParams;
	private String tokenReqMethod;
	private String apiReqMethod;
	private String tokenResType;
	private String apiResType;
	private int dataInterval;
	private String searchKey;
	private boolean isTokenEnable;
	private boolean isUnitActive;
	private String transporterName;
	private String vehicleNativeName;
	private Date lastProcessedTimeVamosys;
	private Date lastProcessedTimeEICHER;
	private boolean isCanBusDataAvailable;
	private int  vehicleID;
	private int  mileage;
	public ArrayList<CanBusModel> lCanModelList;
	private Date lastProcessedTimeKPR;
	private int  dataProviderId;
	private String accessToken;
	String aticGrantType;
	String aticUserName;
	String aticPassword;
	String aticClientId;
	String aticClientSecret;
	
	public Date getLastProcessedTime() {
		return lastProcessedTime;
	}
	public void setLastProcessedTime(Date lastProcessedTime) {
		this.lastProcessedTime = lastProcessedTime;
	}
	public String getMobileUnitId() {
		return mobileUnitId;
	}
	public void setMobileUnitId(String mobileUnitId) {
		this.mobileUnitId = mobileUnitId;
	}
	public boolean isProcessedFlag() {
		return processedFlag;
	}
	public void setProcessedFlag(boolean processedFlag) {
		this.processedFlag = processedFlag;
	}
	public int getCompanyId() {
		return companyId;
	}
	public void setCompanyId(int companyId) {
		this.companyId = companyId;
	} 
	public String getLicensePlate() {
		return licensePlate;
	}
	public void setLicensePlate(String licensePlate) {
		this.licensePlate = licensePlate;
	}
	public int getLastResponseCode() {
		return lastResponseCode;
	}
	public void setLastResponseCode(int lastResponseCode) {
		this.lastResponseCode = lastResponseCode;
	}
	public Date getLastResponseTime() {
		return lastResponseTime;
	}
	public void setLastResponseTime(Date lastResponseTime) {
		this.lastResponseTime = lastResponseTime;
	}
	public String getDataProviderName() {
		return dataProviderName;
	}
	public void setDataProviderName(String dataProviderName) {
		this.dataProviderName = dataProviderName;
	}
	public String getTokenUrl() {
		return tokenUrl;
	}
	public void setTokenUrl(String tokenUrl) {
		this.tokenUrl = tokenUrl;
	}
	public String getApiUrl() {
		return apiUrl;
	}
	public void setApiUrl(String apiUrl) {
		this.apiUrl = apiUrl;
	}
	public String getTokenReqParams() {
		return tokenReqParams;
	}
	public void setTokenReqParams(String tokenReqParams) {
		this.tokenReqParams = tokenReqParams;
	}
	public String getApiReqParams() {
		return apiReqParams;
	}
	public void setApiReqParams(String apiReqParams) {
		this.apiReqParams = apiReqParams;
	}
	public String getTokenReqMethod() {
		return tokenReqMethod;
	}
	public void setTokenReqMethod(String tokenReqMethod) {
		this.tokenReqMethod = tokenReqMethod;
	}
	public String getApiReqMethod() {
		return apiReqMethod;
	}
	public void setApiReqMethod(String apiReqMethod) {
		this.apiReqMethod = apiReqMethod;
	}
	public String getTokenResType() {
		return tokenResType;
	}
	public void setTokenResType(String tokenResType) {
		this.tokenResType = tokenResType;
	}
	public String getApiResType() {
		return apiResType;
	}
	public void setApiResType(String apiResType) {
		this.apiResType = apiResType;
	}
	public int getDataInterval() {
		return dataInterval;
	}
	public void setDataInterval(int dataInterval) {
		this.dataInterval = dataInterval;
	}
	public String getSearchKey() {
		return searchKey;
	}
	public void setSearchKey(String searchKey) {
		this.searchKey = searchKey;
	}
	public boolean isTokenEnable() {
		return isTokenEnable;
	}
	public void setTokenEnable(boolean isTokenEnable) {
		this.isTokenEnable = isTokenEnable;
	}
	public boolean isUnitActive() {
		return isUnitActive;
	}
	public void setUnitActive(boolean isUnitActive) {
		this.isUnitActive = isUnitActive;
	}
	public String getTransporterName() {
		return transporterName;
	}
	public void setTransporterName(String transporterName) {
		this.transporterName = transporterName;
	}
	public String getVehicleNativeName() {
		return vehicleNativeName;
	}
	public void setVehicleNativeName(String vehicleNativeName) {
		this.vehicleNativeName = vehicleNativeName;
	}
	
	public Date getLastProcessedTimeEICHER() {
		return lastProcessedTimeEICHER;
	}
	public void setLastProcessedTimeEICHER(Date lastProcessedTimeEICHER) {
		this.lastProcessedTimeEICHER = lastProcessedTimeEICHER;
	}
	public boolean isCanBusDataAvailable() {
		return isCanBusDataAvailable;
	}
	public void setCanBusDataAvailable(boolean isCanBusDataAvailable) {
		this.isCanBusDataAvailable = isCanBusDataAvailable;
	}
	public int getVehicleID() {
		return vehicleID;
	}
	public void setVehicleID(int vehicleID) {
		this.vehicleID = vehicleID;
	}
	public ArrayList<CanBusModel> getlCanModelList() {
		return lCanModelList;
	}
	public void setlCanModelList(ArrayList<CanBusModel> lCanModelList) {
		this.lCanModelList = lCanModelList;
	}
	public int getMileage() {
		return mileage;
	}
	public void setMileage(int mileage) {
		this.mileage = mileage;
	}
	public Date getLastProcessedTimeVamosys() {
		return lastProcessedTimeVamosys;
	}
	public void setLastProcessedTimeVamosys(Date lastProcessedTimeVamosys) {
		this.lastProcessedTimeVamosys = lastProcessedTimeVamosys;
	}
	public Date getLastProcessedTimeKPR() {
		return lastProcessedTimeKPR;
	}
	public void setLastProcessedTimeKPR(Date lastProcessedTimeKPR) {
		this.lastProcessedTimeKPR = lastProcessedTimeKPR;
	}
	public int getDataProviderId() {
		return dataProviderId;
	}
	public void setDataProviderId(int dataProviderId) {
		this.dataProviderId = dataProviderId;
	}
	public String getAccessToken() {
		return accessToken;
	}
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}
	public String getAticGrantType() {
		return aticGrantType;
	}
	public void setAticGrantType(String aticGrantType) {
		this.aticGrantType = aticGrantType;
	}
	public String getAticUserName() {
		return aticUserName;
	}
	public void setAticUserName(String aticUserName) {
		this.aticUserName = aticUserName;
	}
	public String getAticPassword() {
		return aticPassword;
	}
	public void setAticPassword(String aticPassword) {
		this.aticPassword = aticPassword;
	}
	public String getAticClientId() {
		return aticClientId;
	}
	public void setAticClientId(String aticClientId) {
		this.aticClientId = aticClientId;
	}
	public String getAticClientSecret() {
		return aticClientSecret;
	}
	public void setAticClientSecret(String aticClientSecret) {
		this.aticClientSecret = aticClientSecret;
	}
	
}
