package irt.flash.data.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.TooManyListenersException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

import irt.flash.data.ToHex;
import irt.flash.data.connection.MicrocontrollerSTM32.Command;
import jtermios.JTermios;
import purejavacomm.CommPortIdentifier;
import purejavacomm.NoSuchPortException;
import purejavacomm.PortInUseException;
import purejavacomm.PureJavaSerialPort;
import purejavacomm.SerialPort;
import purejavacomm.UnsupportedCommOperationException;

public class FlashSerialPort {

	private final Logger logger = (Logger) LogManager.getLogger();

	public static final int MAX_WAIT_TIME = 10000;
    public static final int BAUDRATE = JTermios.B115200;
    public static final int WAIT_TIME = 200;

	private CommPortIdentifier portIdentifier;
	private boolean opened;

	private PureJavaSerialPort serialPort;

	private InputStream inputStream;

	private OutputStream outputStream;

	private byte[] buffer;

	public FlashSerialPort(String portName) throws NoSuchPortException {
		portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
	}

	public String getPortName() {
		return portIdentifier.getName();
	}

	public synchronized boolean waitComPort(int watedBytes){
		logger.debug("ENTRY: watedBytes: {}; WAIT_TIME: {}; buffer.length: {}", ()->watedBytes,  ()->WAIT_TIME, ()->Optional.ofNullable(buffer).map(b->b.length).orElse(null));
		long start = System.currentTimeMillis();

		// control maximum timeout
		final int wait = Optional.of(watedBytes*WAIT_TIME).filter(wt->wt<MAX_WAIT_TIME).orElse(MAX_WAIT_TIME);

		return logger.traceExit(Optional
				.ofNullable(getBuffer())
				.filter(b->b.length>=watedBytes)
				.map(b->true)
				.orElseGet(

						()->{

							int bufferLength = 0;

							do{

								synchronized (this) { try { FlashSerialPort.this.wait(wait); } catch (InterruptedException e) { logger.catching(e); } }

								final Optional<byte[]> oBuffer = Optional.ofNullable(getBuffer());

								bufferLength = oBuffer.map(b->b.length).orElse(0);

							}while(bufferLength<watedBytes && System.currentTimeMillis()-start < wait);

							return Optional.ofNullable(getBuffer()).map(b->b.length>=watedBytes).orElse(bufferLength>=watedBytes);

						}));
	}

	public synchronized boolean openPort() throws PortInUseException, UnsupportedCommOperationException, IOException, TooManyListenersException{
		logger.entry(this);

		if(opened)
			return true;

		serialPort = (PureJavaSerialPort) portIdentifier.open(FlashSerialPort.class.getName(), MAX_WAIT_TIME);
		setBaudrate(BAUDRATE);
		serialPort.enableReceiveThreshold(MAX_WAIT_TIME);
		inputStream = serialPort.getInputStream();
		outputStream = serialPort.getOutputStream();

		serialPort.notifyOnDataAvailable(true);
		serialPort.addEventListener(e->{

			if(opened)
				try {

					readToTheBuffer();
					
					synchronized (FlashSerialPort.this) {
						FlashSerialPort.this.notify();					
					}

				} catch (IOException e1) { logger.catching(e1); }
		});

		return opened = true;
	}

	public synchronized void writeBytes(Command command) throws IOException {
		logger.debug("command: {}", command);
		writeBytes(command.toBytes());
	}

	public synchronized void writeBytes(byte[] butes) throws IOException {
		clear();
		logger.debug("bytes: {}", ToHex.bytesToHex(butes));
		outputStream.write(butes);
	}

	public byte[] readBytes(int size){

		final Optional<byte[]> ofNullable = Optional.ofNullable(getBuffer());

		logger.trace( "ENTRY size: {}; buffer.length: {}; buffer: {}", ()->size, ()->ofNullable.map(b->b.length).orElse(null), ()->getBuffer());

		final byte[] result = ofNullable
				.map(bf->bf.length)
				.filter(bLength->bLength >= size)
				.map(bLength->{
					try {
						return getAPartFromTheBuffer(size);
					} catch (IOException e1) {
						logger.catching(e1);
					}
					return null;
				})
				.orElseGet(()->{
					try {

						if(waitComPort(size))
							return getAPartFromTheBuffer(size);
						else if(getBuffer()==null)
							return null;
						else
							return getAPartFromTheBuffer(getBuffer().length);

					} catch (Exception e) {
						logger.catching(e);
					}

					return null;
				});

		logger.trace("EXIT result: {}", ()->ToHex.bytesToHex(result));

		return  result;

	}

	private synchronized byte[] getAPartFromTheBuffer(final int size) throws IOException {
		logger.trace("ENTRY size: {}; buffer: {}", ()->size, ()->ToHex.bytesToHex(buffer));

		byte[] result;

		if(buffer==null || buffer.length<size)
			return null;

//		synchronized (MyComPort.this) {
			
			if(buffer.length==size){
				 result = buffer;
				 buffer = null;

			}else{
				result = Arrays.copyOf(buffer, size);
				buffer = Arrays.copyOfRange(buffer, size, buffer.length);
			}
//		}

		return result;
	}

	private synchronized void readToTheBuffer() throws IOException {
		logger.traceEntry();

			final byte[] bytes = new byte[inputStream.available()];

			if(bytes.length==0)
				return;

			inputStream.read(bytes);
			logger.debug("read bytes: {}; bytes: {}\n buffer: {}", ()->bytes.length, ()->ToHex.bytesToHex(bytes), ()->ToHex.bytesToHex(buffer));

			setBuffer(Optional
					.ofNullable(buffer)
					.map(bf->{

						final int oldLength = bf.length;
						bf = Arrays.copyOf(bf, oldLength + bytes.length);
						System.arraycopy(bytes, 0, bf, oldLength, bytes.length);

						return bf;
					})
					.orElse(bytes));
	}

	public synchronized byte[] getBuffer() {
		return buffer;
	}

	public synchronized void setBuffer(byte[] buffer) {
		this.buffer = buffer;
	}

	public void setBaudrate(int baudrate) {
		logger.entry(baudrate);
		try {
			serialPort.setSerialPortParams(baudrate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_ODD);
		} catch (UnsupportedCommOperationException e) {
			logger.catching(e);
		}
	}

	public void clear(){
		logger.traceEntry(()->getBuffer());

//		 do{

			 setBuffer(null);

//			 try { synchronized (this) {
//				 wait(20);
//			 }
//			 } catch (InterruptedException e) {}
//
//		 }while (readToTheBuffer || Optional.ofNullable(getBuffer()).filter(b->b.length>0).map(b->true).orElse(false));

//		logger.traceExit();
	}

	public synchronized boolean closePort() {
		logger.trace(toString());

		if(!opened)
			return true;

		opened = false;
		buffer = null;

		try {
			if(inputStream!=null)
				inputStream.close();
		} catch (IOException e) {
			logger.catching(e);
		}

		try {
			if(outputStream!=null)
				outputStream.close();
		} catch (IOException e) {
			logger.catching(e);
		}

		if(serialPort!=null){
			serialPort.removeEventListener();
			serialPort.disableReceiveTimeout();
			serialPort.close();
		}

		return true;
	}

	public boolean isOpened() {
		return opened;
	}

	public static List<String> getPortNames(){

		List<String> portsList = new ArrayList<>();

		final Enumeration<CommPortIdentifier> portIdentifiers = CommPortIdentifier.getPortIdentifiers();

		while(portIdentifiers.hasMoreElements())
			portsList.add(portIdentifiers.nextElement().getName());

		Collections.sort(portsList, (a, b)->Integer.parseInt(a.replaceAll("\\D", "")) - Integer.parseInt(b.replaceAll("\\D", "")));

		return portsList;
	}

	@Override
	public String toString() {
		return Optional.ofNullable(portIdentifier).map(CommPortIdentifier::getName).orElse(null);
	}
}
