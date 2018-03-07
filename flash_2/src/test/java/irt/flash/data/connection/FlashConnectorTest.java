package irt.flash.data.connection;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.TooManyListenersException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.prefs.Preferences;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import irt.flash.data.connection.FlashConnector.ConnectionStatus;
import irt.flash.presentation.panel.ConnectionPanel;
import purejavacomm.NoSuchPortException;
import purejavacomm.PortInUseException;
import purejavacomm.UnsupportedCommOperationException;

public class FlashConnectorTest {

	private final static Logger logger = LogManager.getLogger();

	@Before
	public void before(){

		Preferences.userRoot().node("IRT Technologies inc.").put(ConnectionPanel.SERIAL_PORT, "COM14");
	}

	@Test
	public void test() throws InterruptedException, NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException, TooManyListenersException{

		logger.info("***** start *****");
		final FutureTask<ConnectionStatus> connect = FlashConnector.connect();

		logger.info("***** connecting *****");

		try {

			assertEquals(ConnectionStatus.CONNECTED, connect.get(FlashSerialPort.MAX_WAIT_TIME, TimeUnit.MILLISECONDS));

		} catch (ExecutionException | TimeoutException e) {
			logger.catching(e);
		}

		logger.info("***** connected *****");

		assertEquals(ConnectionStatus.CONNECTED, FlashConnector.getConnectionStatus());
		logger.info("***** Test END *****");
	}

	@After
	public void after(){
		FlashConnector.disconnect();
	}
}
