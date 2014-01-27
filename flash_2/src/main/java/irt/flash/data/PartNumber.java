package irt.flash.data;

import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;

public class PartNumber {

	public static final ProfileProperties PROFILE_PROPERTIE = ProfileProperties.DEVICE_PART_NUMBER;

	private long id;
	private String partNumber;
	private String description;
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getPartNumber() {
		return partNumber;
	}
	public void setPartNumber(String partNumber) {
		this.partNumber = partNumber;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
}
