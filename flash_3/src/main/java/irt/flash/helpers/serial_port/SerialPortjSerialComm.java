package irt.flash.helpers.serial_port;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

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

							if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
								return;

							ThreadWorker.runThread(
									()->{

										final int bytesAvailable = serialPort.bytesAvailable();
										if(bytesAvailable==0)
											return;

										final byte[] tmp = new byte[bytesAvailable];
										serialPort.readBytes(tmp, tmp.length);

										// Remove zeros
										final int[] noZeros = IntStream.range(0, tmp.length).filter(index->tmp[index]!=0).map(index->tmp[index]&0xFF).toArray();
										if(noZeros.length!=tmp.length)
											logger.warn("Serial port receive zeros: \n received\n  {}\n Filtered\n  {}", tmp, noZeros);

										if(noZeros.length==0)
											return;

										final byte[] b = new byte[noZeros.length];
										IntStream.range(0, b.length).forEach(index->b[index]=(byte) noZeros[index]);

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
											logger.debug("notify();\n buffer: {},\n new bytes: {};\n", buffer, b);
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
		synchronized (serialPort) {
			buffer = null; //clear buffer
		}
		final int writeBytes = serialPort.writeBytes(bytes, bytes.length);
		return writeBytes>0;
	}

	@Override
	public byte[] read(int size) throws Exception {
		logger.traceEntry("read {} bites from buffer: {}", size, buffer);

		if(size<=0)
			size = Integer.MAX_VALUE;

		final long start = System.currentTimeMillis();
		long elapsed = 0;
		final long min = TimeUnit.SECONDS.toMillis(FlashController.MIN_WAIT_TIME_IN_SECONDS);
		final long max = TimeUnit.MINUTES.toMillis(FlashController.MAX_WAIT_TIME_IN_MINUTES);
		final int waitTime = Optional.of(size*min).filter(time->time<max).orElse(max).intValue();

		while((buffer==null || buffer.length<size) && (elapsed=(System.currentTimeMillis()-start))<waitTime) {

			final long timeout = waitTime - elapsed;
//		logger.error("timeout: {}; size to read: {}; buffer.length: {}", timeout, size, Optional.ofNullable(buffer).map(b->b.length).orElse(null));
			if(timeout<=0)
				break;

			synchronized (this) {
				wait(timeout);
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

		logger.debug("to read: {}; result: {}; buffer: {}", size, result, buffer);

		return result;
	}
}
