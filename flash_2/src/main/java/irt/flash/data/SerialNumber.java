package irt.flash.data;

import java.sql.Timestamp;

public class SerialNumber {

	private long id;
	private String serialNumber;
	private Timestamp date;

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getSerialNumber() {
		return serialNumber;
	}
	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}
	public Timestamp getDate() {
		return date;
	}
	public void setDate(Timestamp date) {
		this.date = date;
	}
}
