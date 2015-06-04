package irt.flash.data.connection;

import java.util.Arrays;

import irt.flash.data.connection.MicrocontrollerSTM32.Command;
import jssc.SerialPort;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class FlashSerialPort extends SerialPort {

	private final Logger logger = (Logger) LogManager.getLogger();

	public FlashSerialPort(String portName) {
		super(portName);
		logger.info("* Start *");
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

		logger.entry("is Opened", isOpened);

		if (!isOpened) {
			isOpened = super.openPort();
			if (isOpened){
				setParams(SerialPort.BAUDRATE_115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_ODD);
				addEventListener(new SerialPortEvent());
			}
		}
		return logger.exit(isOpened);
	}

	public synchronized boolean writeBytes(Command command) throws SerialPortException {
		logger.entry(command);
		clear();
		return logger.exit(writeBytes(command.toBytes()));
	}

	@Override
	public byte[] readBytes() throws SerialPortException {
		byte[] readBytes = super.readBytes();
		Level level = logger.getLevel();
		if(level==Level.ALL || level==Level.TRACE)
			logger.trace(Arrays.toString(readBytes));
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
		Level level = logger.getLevel();

		if(level==Level.ALL || level==Level.TRACE)
			logger.trace("read: {}", Arrays.toString(readBytes));

		return logger.exit(readBytes);
	}

	public byte[] clear() throws SerialPortException {
		logger.entry();
		int waitTime = 20;
		byte[] readBytes = null;
		while(wait(1, waitTime)){
			readBytes = super.readBytes(getInputBufferBytesCount());
			logger.trace("Clear={}", readBytes);
			if(waitTime!=100)
				waitTime = 100;
		}
		logger.exit();
		return readBytes;
	}

	@Override
	public synchronized boolean closePort() throws SerialPortException {

		boolean isOpened = isOpened();
		logger.entry("isOpened", isOpened);
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
