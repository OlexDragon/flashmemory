package irt.flash.data;

public class ValueDescription {
	private String value;
	private String description;

	public ValueDescription(String value, String description) {
		this.value = value;
		this.description = description;
	}
	public String getValue() {
		return value;
	}
	public String getDescription() {
		return description;
	}
	@Override
	public String toString() {
		return value + " - " + description;
	}
	@Override
	public int hashCode() {
		return value==null || value.isEmpty() ? super.hashCode() : value.hashCode();
	}
	@Override
	public boolean equals(Object obj) {
		return obj!=null ? obj.hashCode()==hashCode() : false;
	}
}
