package irt.flash.data.connection;

import static org.junit.Assert.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.TooManyListenersException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.prefs.Preferences;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import irt.flash.data.connection.FlashConnector.ConnectionStatus;
import irt.flash.data.connection.MicrocontrollerSTM32.Address;
import irt.flash.data.connection.MicrocontrollerSTM32.Answer;
import irt.flash.presentation.panel.ConnectionPanel;
import purejavacomm.NoSuchPortException;
import purejavacomm.PortInUseException;
import purejavacomm.UnsupportedCommOperationException;

public class MicrocontrollerSTM32Test {

	private final static Logger logger = LogManager.getLogger();

	private MicrocontrollerSTM32 stm32;
	private String unitType;

	public MicrocontrollerSTM32Test() throws InterruptedException, NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException, TooManyListenersException {
		stm32 = MicrocontrollerSTM32.getInstance();
		Preferences.userRoot().node("IRT Technologies inc.").put(ConnectionPanel.SERIAL_PORT, "COM13");
		unitType = Address.BIAS.getName();

		logger.info("Connecting");
		try {

			final ConnectionStatus connectionStatus = FlashConnector.connect().get(3000, TimeUnit.MILLISECONDS);
			assertEquals(ConnectionStatus.CONNECTED, connectionStatus);

			logger.info("Connected");

		} catch (ExecutionException | TimeoutException e) { logger.catching(e); }
	}

	@Before
	public void init() throws InterruptedException, NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException, TooManyListenersException{
	}

	@Test
	public void test() {
		BigDecimal actual = new BigDecimal(2149).divide(new BigDecimal(10000), 2, RoundingMode.HALF_EVEN);
		System.out.println(actual);
		assertEquals(new BigDecimal("0.21"), actual);
	}

	@Test
	public void test2() {
		stm32.notifyObservers();
	}

	@Test
	public void testConnection() throws InterruptedException, ExecutionException, TimeoutException {

		final byte[] read = MicrocontrollerSTM32.read(unitType).get(FlashSerialPort.MAX_WAIT_TIME, TimeUnit.MILLISECONDS);

		logger.info("bytes {}", read);
		assertNotNull(read);
	}

	@Test
	public void testEraseFlash() throws InterruptedException, ExecutionException, TimeoutException{

		Answer answer = MicrocontrollerSTM32.erase(unitType).get(5, TimeUnit.SECONDS);
		assertEquals(Answer.ACK, answer);
	}

	@Test
	public void testEraseProgramFlash() throws InterruptedException, ExecutionException, TimeoutException{

		Answer answer = MicrocontrollerSTM32.erase(Address.PROGRAM.name()).get(5, TimeUnit.SECONDS);
		assertEquals(Answer.ACK, answer);
	}

	@After
	public void after(){
		FlashConnector.disconnect();
	}
}
