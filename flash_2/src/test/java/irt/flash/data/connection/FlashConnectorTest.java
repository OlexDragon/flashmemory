package irt.flash.data.connection;

import static org.junit.Assert.*;
import jssc.SerialPortException;

import org.junit.Test;

public class FlashConnectorTest {

	@Test
	public void test() throws SerialPortException, InterruptedException {

		FlashConnector.connect();

		synchronized (this) {
			wait(10000);
		}
		assertTrue(FlashConnector.isConnected());
		FlashConnector.disconnect();
	}

}
