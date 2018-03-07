package irt.flash.data.connection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.TooManyListenersException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.junit.After;
import org.junit.Test;

import purejavacomm.NoSuchPortException;
import purejavacomm.PortInUseException;
import purejavacomm.UnsupportedCommOperationException;

public class FlashSerialPortTest {

	private final Logger logger = (Logger) LogManager.getLogger();

	private FlashSerialPort serialPort;
	private String portName = "COM13";

	@org.junit.Before
	public void init() throws NoSuchPortException{
		serialPort = new FlashSerialPort(portName);
		logger.entry(serialPort);
	}

	@Test
	public void testOpenPort() throws PortInUseException, UnsupportedCommOperationException, IOException, TooManyListenersException {
		boolean opened = serialPort.isOpened();
		logger.trace("Serial Port is Opened={}", opened);

		if(!opened)
			assertTrue(serialPort.openPort());

		logger.exit();
	}

	@Test
	public void testClosePort() throws PortInUseException, UnsupportedCommOperationException, IOException, TooManyListenersException {

		serialPort.openPort();

		boolean opened = serialPort.isOpened();
		logger.trace("Serial Port is Opened={}", opened);

		boolean closed = serialPort.closePort();

		if(opened){
			assertTrue(closed);
		}else{
			assertFalse(closed);
		}
		logger.exit(closed);
	}

	@After
	public void closePort() {
		logger.entry();
		serialPort.closePort();
		logger.exit();
	}
}
