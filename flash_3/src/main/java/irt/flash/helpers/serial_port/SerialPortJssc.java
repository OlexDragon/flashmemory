package irt.flash.helpers.serial_port;

import java.util.concurrent.TimeUnit;

import irt.flash.FlashController;
import jssc.SerialPort;
import jssc.SerialPortException;

public class SerialPortJssc extends SerialPort implements IrtSerialPort {

	public SerialPortJssc(String portName) {
		super(portName);
	}

	@Override
	public boolean writeBytes(byte[] buffer) throws SerialPortException {
		purgePort(SerialPort.PURGE_RXABORT | SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXABORT | SerialPort.PURGE_TXCLEAR);
		return super.writeBytes(buffer);
	}

	@Override
	public byte[] read(int size) throws Exception{
		return readBytes(size, (int) TimeUnit.MINUTES.toMillis(FlashController.MAX_WAIT_TIME_IN_MINUTES));
	}

	@Override
	public byte[] read(int size, int timeout) throws Exception {
		return super.readBytes(size, timeout);
	}
}
