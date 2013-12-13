package irt.flash.data.connection;

import static org.junit.Assert.*;
import jssc.SerialPortException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.junit.After;
import org.junit.Test;

public class FlashSerialPortTest {

	private final Logger logger = (Logger) LogManager.getLogger();

	private FlashSerialPort serialPort;
	private String portName = "COM1";

	@org.junit.Before
	public void init(){
		serialPort = new FlashSerialPort(portName);
		logger.entry(serialPort);
	}

	@Test
	public void testOpenPort() throws SerialPortException {
		boolean opened = serialPort.isOpened();
		logger.entry("Serial Port is Opened={}", opened);

		if(!opened)
			assertTrue(serialPort.openPort());

		logger.exit();
	}

	@Test
	public void testClosePort() throws SerialPortException {
		boolean opened = serialPort.isOpened();
		logger.entry("Serial Port is Opened={}", opened);

		boolean closed = serialPort.closePort();

		if(opened){
			assertTrue(closed);
		}else{
			assertFalse(closed);
		}
		logger.exit(closed);
	}

	@After
	public void closePort() throws SerialPortException{
		logger.entry();
		serialPort.closePort();
		logger.exit();
	}
}
