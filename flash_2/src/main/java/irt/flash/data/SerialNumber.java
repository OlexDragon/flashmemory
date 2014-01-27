package irt.flash.data;

import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;

import java.sql.Timestamp;

public class SerialNumber {

	public static final ProfileProperties PROFILE_PROPERTIE = ProfileProperties.SERIAL_NUMBER;

	private long id;
	private String serialNumber;
	private Timestamp date;
	private String profile;
	private Timestamp profileChangeDate;
	private int softwareId;

	public long getId() {
		return id;
	}
	public SerialNumber setId(long id) {
		this.id = id;
		return this;
	}
	public String getSerialNumber() {
		return serialNumber;
	}
	public SerialNumber setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
		return this;
	}
	public Timestamp getDate() {
		return date;
	}
	public void setDate(Timestamp date) {
		this.date = date;
	}
	public String getProfile() {
		return profile;
	}
	public Timestamp getProfileChangeDate() {
		return profileChangeDate;
	}
	public int getSoftwareId() {
		return softwareId;
	}
	public void setProfile(String profile) {
		this.profile = profile;
	}
	public void setProfileChangeDate(Timestamp profileChangeDate) {
		this.profileChangeDate = profileChangeDate;
	}
	public void setSoftwareId(int softwareId) {
		this.softwareId = softwareId;
	}
	@Override
	public int hashCode() {
		return id>0 ? ((Long)id).hashCode() : super.hashCode();
	}
	/**
	 * this.id==id && this.profileChangeDate-10*1000 < profileChangeDate;
	 */
	@Override
	public boolean equals(Object obj) {
		return obj != null ? obj.hashCode()==hashCode() : false;
	}
	@Override
	public String toString() {
		return "SerialNumber [id=" + id + ", serialNumber=" + serialNumber + ", date=" + date + ", profile=" + profile + ", profileChangeDate=" + profileChangeDate
				+ ", softwareId=" + softwareId + "]";
	}
}
