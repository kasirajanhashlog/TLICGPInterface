package com.claystone.db;

public class DeviceTypeModel implements java.io.Serializable {
	
	private static final long serialVersionUID = 1L;
	private int hardwareId;
	private String hardwareName;
	public int getHardwareId() {
		return hardwareId;
	}
	public void setHardwareId(int hardwareId) {
		this.hardwareId = hardwareId;
	}
	public String getHardwareName() {
		return hardwareName;
	}
	public void setHardwareName(String hardwareName) {
		this.hardwareName = hardwareName;
	}
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	
}
