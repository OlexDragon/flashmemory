package irt.flash.data;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class SerialNumberTableRow {

	private long id;
	private long serialNumberId;
	private long tableNameId;
	private BigDecimal key;
	private BigDecimal value;
	private boolean status;
	private Timestamp date;
	private Timestamp statusChangeDate;

	public long getId() {
		return id;
	}
	public long getSerialNumberId() {
		return serialNumberId;
	}
	public long getTableNameId() {
		return tableNameId;
	}
	public BigDecimal getKey() {
		return key;
	}
	public BigDecimal getValue() {
		return value;
	}
	public boolean isStatus() {
		return status;
	}
	public Timestamp getDate() {
		return date;
	}
	public Timestamp getStatusChangeDate() {
		return statusChangeDate;
	}
	public SerialNumberTableRow setId(long id) {
		this.id = id;
		return this;
	}
	public SerialNumberTableRow setSerialNumberId(long serialNumberId) {
		this.serialNumberId = serialNumberId;
		return this;
	}
	public SerialNumberTableRow setTableNameId(long tableNameId) {
		this.tableNameId = tableNameId;
		return this;
	}
	public SerialNumberTableRow setKey(BigDecimal key) {
		this.key = key;
		return this;
	}
	public SerialNumberTableRow setValue(BigDecimal value) {
		this.value = value;
		return this;
	}
	public SerialNumberTableRow setStatus(boolean status) {
		this.status = status;
		return this;
	}
	public SerialNumberTableRow setDate(Timestamp date) {
		this.date = date;
		return this;
	}
	public SerialNumberTableRow setStatusChangeDate(Timestamp statusChangeDate) {
		this.statusChangeDate = statusChangeDate;
		return this;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		result = prime * result + (int) (id ^ (id >>> 32));
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + (int) (serialNumberId ^ (serialNumberId >>> 32));
		result = prime * result + (status ? 1231 : 1237);
		result = prime * result + ((statusChangeDate == null) ? 0 : statusChangeDate.hashCode());
		result = prime * result + (int) (tableNameId ^ (tableNameId >>> 32));
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SerialNumberTableRow other = (SerialNumberTableRow) obj;
		if (date == null) {
			if (other.date != null)
				return false;
		} else if (!date.equals(other.date))
			return false;
		if (id != other.id)
			return false;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (serialNumberId != other.serialNumberId)
			return false;
		if (status != other.status)
			return false;
		if (statusChangeDate == null) {
			if (other.statusChangeDate != null)
				return false;
		} else if (!statusChangeDate.equals(other.statusChangeDate))
			return false;
		if (tableNameId != other.tableNameId)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "SerialNumberTableRow [id=" + id + ", serialNumberId=" + serialNumberId + ", tableNameId=" + tableNameId + ", key=" + key + ", value=" + value
				+ ", status=" + status + ", date=" + date + ", statusChangeDate=" + statusChangeDate + "]";
	}
}
