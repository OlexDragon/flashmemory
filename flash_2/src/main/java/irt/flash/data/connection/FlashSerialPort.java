package irt.flash.data.connection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

import irt.flash.data.ToHex;
import irt.flash.data.connection.MicrocontrollerSTM32.Command;
import jssc.SerialPort;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

public class FlashSerialPort extends SerialPort {

	private final Logger logger = (Logger) LogManager.getLogger();

	public FlashSerialPort(String portName) {
		super(portName);
	}

	public synchronized boolean wait(int eventValue, int waitTime) throws SerialPortException {
		logger.entry("(int eventValue, int waitTime)", eventValue, waitTime);
		boolean isReady = false;
		long start = System.currentTimeMillis();
		long waitTimeL = waitTime * eventValue;

		long time = System.currentTimeMillis() - start;
		while (isOpened() && !(isReady = getInputBufferBytesCount() >= eventValue) && time < waitTimeL) {

			try { wait(waitTimeL); } catch (InterruptedException e) { logger.catching(e); }
			time = System.currentTimeMillis() - start;
		};
		logger.trace("waiting; time={}, waitTime={}", time, waitTimeL);

		return logger.exit(isReady);
	}

	@Override
	public synchronized boolean openPort() throws SerialPortException {

		boolean isOpened = isOpened();

		if (!isOpened) {
			isOpened = super.openPort();
			if (isOpened){
				setParams(SerialPort.BAUDRATE_115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_ODD);
				addEventListener(new SerialPortEvent());
			}
		}
		return isOpened;
	}

	public synchronized boolean writeBytes(Command command) throws SerialPortException {
		clear();
//		logger.error("{}: {}", command, ToHex.bytesToHex(command.toBytes()));
		return writeBytes(command.toBytes());
	}

	@Override
	public byte[] readBytes() throws SerialPortException {
		byte[] readBytes = super.readBytes();
//		logger.error("{}", ToHex.bytesToHex(readBytes));
		return readBytes;
		
	}

	@Override
	public byte[] readBytes(int byteCount) throws SerialPortException {
		return readBytes(byteCount, 100);
	}

	public synchronized byte[] readBytes(int byteCount, int waitTime) throws SerialPortException {
		logger.entry("(int byteCount, int waitTime)", byteCount, waitTime);

		byte[] readBytes = null;

		if (wait(byteCount, waitTime) && isOpened())
			readBytes = super.readBytes(byteCount);

//		logger.error("{}", ToHex.bytesToHex(readBytes));
		return readBytes;
	}

	public byte[] clear() throws SerialPortException {

		int waitTime = 20;
		byte[] readBytes = null;
		while(wait(1, waitTime)){
			readBytes = super.readBytes(getInputBufferBytesCount());
			logger.error("Clear={}", ToHex.bytesToHex(readBytes));
			if(waitTime!=100)
				waitTime = 100;
		}

		return readBytes;
	}

	@Override
	public synchronized boolean closePort() throws SerialPortException {

		boolean isOpened = isOpened();
		boolean isClosed = false;

		if (isOpened) {
//			try { removeEventListener(); } catch (Exception e) { logger.catching(e); }

			purgePort(PURGE_RXCLEAR | PURGE_TXCLEAR | PURGE_RXABORT | PURGE_TXABORT);

			isClosed = super.closePort();
		}

		return logger.exit(isClosed);
	}

	//*** Class SerialPortEvent *****************************************************
	private class SerialPortEvent implements SerialPortEventListener{

		@Override
		public void serialEvent(jssc.SerialPortEvent serialPortEvent) {

			synchronized (FlashSerialPort.this) {
				FlashSerialPort.this.notify();
			}
		}
		
	}
}
