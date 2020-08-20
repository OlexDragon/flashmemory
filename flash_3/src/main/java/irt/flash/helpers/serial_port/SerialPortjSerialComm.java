package irt.flash.helpers.serial_port;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import irt.flash.FlashController;
import irt.flash.helpers.ThreadWorker;

public class SerialPortjSerialComm implements IrtSerialPort {

	private final static Logger logger = LogManager.getLogger();

	private final SerialPort serialPort;
	private byte[] buffer;

	public SerialPortjSerialComm(String portName) {
		serialPort = SerialPort.getCommPort(portName);
	}

	@Override
	public String getPortName() {
		return serialPort.getSystemPortName();
	}

	@Override
	public boolean openPort() throws Exception {

		final boolean openPort = serialPort.openPort();

		if(openPort)
			serialPort.addDataListener(
					new SerialPortDataListener() {

						@Override
						public int getListeningEvents() {
							return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
						}

						@Override
						public void serialEvent(SerialPortEvent event) {
							logger.traceEntry("{}", event);

							ThreadWorker.runThread(
									()->{
										if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
											return;

										byte[] b = new byte[serialPort.bytesAvailable()];
										serialPort.readBytes(b, b.length);

										synchronized (serialPort) {
											if(buffer==null)
												buffer = b;

											else {
												final byte[] copyOfBuffer = Arrays.copyOf(buffer, buffer.length + b.length);
												System.arraycopy(b, 0, copyOfBuffer, buffer.length, b.length);
												buffer = copyOfBuffer;
											}
										}

										synchronized(SerialPortjSerialComm.this){
											SerialPortjSerialComm.this.notify();
											logger.debug("notify(); buffer: {}", buffer);
										}
									});

						}});
		return openPort;
	}

	@Override
	public boolean closePort() throws Exception {
		serialPort.removeDataListener();
		return serialPort.closePort();
	}

	@Override
	public boolean setParams(int baudRate, int databits, int stopbits, int parityEven) throws Exception {
		return serialPort.setComPortParameters(baudRate, databits, stopbits, parityEven);
	}

	@Override
	public boolean isOpened() {
		return serialPort.isOpen();
	}

	@Override
	public boolean writeBytes(byte[] bytes) throws Exception {
		logger.traceEntry("{}", bytes);
		buffer = null; //clear buffer
		final int writeBytes = serialPort.writeBytes(bytes, bytes.length);
		return writeBytes>0;
	}

	@Override
	public byte[] read(int size) throws Exception {
		logger.traceEntry("size: {};  buffer: {}", size, buffer);

		if(size<=0)
			size = Integer.MAX_VALUE;

		final long start = System.currentTimeMillis();
		long elapsed = 0;
		final int waitTime = (int) TimeUnit.MINUTES.toMillis(FlashController.MAX_WAIT_TIME_IN_MINUTES);

			synchronized (this) {
		while((buffer==null || buffer.length<size) && (elapsed=(System.currentTimeMillis()-start))<waitTime) {

				wait(waitTime - elapsed);
			}
		}

//		logger.error("waitTime: {} milles; elapsed time = {} milles; buffer: {};", waitTime, elapsed, buffer);

		if(buffer==null || buffer.length<size) {
			logger.warn("No answer or it is to short. buffer: {}", buffer);
			return null;
		}

		byte[] result;

		synchronized (serialPort) {
			result = Arrays.copyOf(buffer, size);
			buffer = Arrays.copyOfRange(buffer, size, buffer.length);
		}


		return result;
	}

}
