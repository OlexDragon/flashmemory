package irt.flash.helpers.serial_port;

public interface IrtSerialPort {

	String getPortName();
	boolean openPort() throws Exception;
	boolean closePort() throws Exception;
	boolean setParams(int baudRate, int databits8, int stopbits1, int parityEven) throws Exception;
	boolean isOpened();
	boolean writeBytes(byte[] bytes) throws Exception;
	byte[] read(int maxVarRamSize) throws Exception;
}
