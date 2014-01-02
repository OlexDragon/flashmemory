package irt.flash.data;

public class DeviceType {

	private int deviceType;
	private String description;

	public DeviceType(int deviceType, String description) {
		this.deviceType = deviceType;
		this.description = description;
	}
	public int getDeviceType() {
		return deviceType;
	}
	public String getDescription() {
		return description;
	}
	@Override
	public String toString() {
		return deviceType + " - " + description ;
	}
	@Override
	public int hashCode() {
		return deviceType>0 ? deviceType : super.hashCode();
	}
	@Override
	public boolean equals(Object obj) {
		return obj!=null ? obj.hashCode()==hashCode() : false;
	}
}
