package irt.flash.data;

import java.sql.Timestamp;

public class SerialNumberProfileVariable {

	private long id;
	private long serialNumberId;//owner
	private long variableId;
	private String variableValue;
	private boolean status;//true = Active
	private Timestamp date;
	private Timestamp statusChangeDate;

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public long getSerialNumberId() {
		return serialNumberId;
	}
	public void setSerialNumberId(long serialNumberId) {
		this.serialNumberId = serialNumberId;
	}
	public long getVariableId() {
		return variableId;
	}
	public void setVariableId(long variableId) {
		this.variableId = variableId;
	}
	public String getVariableValue() {
		return variableValue;
	}
	public void setVariableValue(String variableValue) {
		this.variableValue = variableValue;
	}
	public boolean isStatus() {
		return status;
	}
	public void setStatus(boolean status) {
		this.status = status;
	}
	public Timestamp getDate() {
		return date;
	}
	public void setDate(Timestamp date) {
		this.date = date;
	}
	@Override
	public String toString() {
		return "SerialNumberProfileVariable [id=" + id + ", serialNumberId=" + serialNumberId + ", variableId=" + variableId + ", variableValue=" + variableValue
				+ ", status=" + status + ", date=" + date + "]";
	}
	public Timestamp getStatusChangeDate() {
		return statusChangeDate;
	}
	public void setStatusChangeDate(Timestamp statusChangeDate) {
		this.statusChangeDate = statusChangeDate;
	}
}
