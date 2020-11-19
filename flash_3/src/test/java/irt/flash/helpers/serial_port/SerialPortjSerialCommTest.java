package irt.flash.helpers.serial_port;

import static org.junit.Assert.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import irt.flash.data.FlashAnswer;
import irt.flash.data.FlashCommand;
import irt.flash.data.UnitAddress;
import irt.flash.helpers.FlashWorker;
import irt.flash.helpers.ReadFlashWorker;
import jssc.SerialPort;

public class SerialPortjSerialCommTest {
	private final Logger logger = LogManager.getLogger();

	private IrtSerialPort serialPort;

	@Before
	public void setup() throws Exception {
		serialPort = new SerialPortjSerialComm("COM3");
		//baudRate: 57600; databits: 8; stopbits: 1; parityEven: 2;
		serialPort.openPort();
		serialPort.setParams(57600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_EVEN);
	}

	@Test
	public void getIdest() throws Exception {

		assertTrue(serialPort.isOpened());

		serialPort.writeBytes(FlashCommand.GET_ID.toBytes());
		byte[] read = serialPort.read(1);

		assertNotNull(read);
		assertTrue(FlashAnswer.ACK.match(read[0]));

		read = serialPort.read(3);

		assertNotNull(read);
		assertArrayEquals(new byte[] {1, 4, 25}, read);

		read = serialPort.read(1);

		assertNotNull(read);
		assertTrue(FlashAnswer.ACK.match(read[0]));
	}

	@Test
	public void getProfile() throws Exception {

		serialPort.writeBytes(FlashCommand.READ_MEMORY.toBytes());

		byte[] read = serialPort.read(1);

		assertNotNull(read);
		assertTrue(FlashAnswer.ACK.match(read[0]));

		final int addr = UnitAddress.HP_BIAS.getAddr();		
		serialPort.writeBytes(FlashWorker.addCheckSum(UnitAddress.intToBytes(addr)).get());

		read = serialPort.read(1);

		assertNotNull(read);
		assertTrue(FlashAnswer.ACK.match(read[0]));

		serialPort.writeBytes(ReadFlashWorker.MAX_BYTES_TO_READ);

		read = serialPort.read(1);

		assertNotNull(read);
		assertTrue(FlashAnswer.ACK.match(read[0]));

		read = serialPort.read(256);
		logger.debug(new String(read));
	}

	@After
	public void exit() throws Exception {
		serialPort.closePort();
	}
}
