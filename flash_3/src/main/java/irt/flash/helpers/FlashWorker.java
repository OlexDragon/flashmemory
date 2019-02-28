package irt.flash.helpers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.stream.IntStream;

import javax.xml.bind.DatatypeConverter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import irt.flash.FlashController;
import irt.flash.data.FlashAnswer;
import irt.flash.data.FlashCommand;
import irt.flash.data.UnitAddress;
import javafx.scene.control.Alert.AlertType;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;

public class FlashWorker {

	private static final Logger logger = LogManager.getLogger();

	public static final int KB = 1024;
	public final static int[] ALL_PAGES = new int[] { 	16 * KB, 16 * KB, 16 * KB, 16 * KB, 64 * KB, 128 * KB, 128 * KB, 128 * KB, 128 * KB, 128 * KB, 128 * KB, 128 * KB,
														16 * KB, 16 * KB, 16 * KB, 16 * KB, 64 * KB, 128 * KB, 128 * KB, 128 * KB, 128 * KB, 128 * KB, 128 * KB, 128 * KB};

	/**
	 * 
	 * @param startAddress - first byte to erase
	 * @param length		- number of bytes to erase
	 * @return				- the number of pages to be erased (1 byte) + the page numbers
	 * @throws IOException
	 */
	public static byte[] getPagesToExtendedErase(int startAddress, int length) throws IOException {

		int stopAddress = startAddress + length;
		int sum = UnitAddress.PROGRAM.getAddr();	//Start address

		try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream()){
			outputStream.write(0);	// The bootloader receives one half-word (two bytes) that contain N, the number of pages to be erased
			outputStream.write(0);


			for(int page = 0; page<ALL_PAGES.length && sum<stopAddress; page++) {

				if(sum>=startAddress){
					final byte[] bytes = toBytes((short) page);
					outputStream.write(bytes); // The bootloader receives one half-word (two bytes) that contain N, the number of pages to be erased
												//  each half-word containing a page number (coded on two bytes, MSB first).
				}

				sum += ALL_PAGES[page];
			}

			final byte[] result = outputStream.toByteArray();
			final int pages = result.length/2 - 2;
			final byte[] arrayPages = toBytes((short) pages);
			result[0] = arrayPages[0]; // the number of pages to be erased –1.
			result[1] = arrayPages[1]; 

			return result;
		}
	}

	private static byte[] toBytes(final short pages) {
		return ByteBuffer.allocate(2).putShort(pages).array();
	}

	public static Optional<FlashAnswer> erase(SerialPort serialPort, int startAddress, int length) throws IOException, SerialPortException, SerialPortTimeoutException {

		final byte[] pagesToErase = getPagesToExtendedErase(startAddress, length);
		final short pages = ByteBuffer.allocate(2).put(pagesToErase[0]).put(pagesToErase[1]).getShort(0);
		int numberOfPagies = pages + 1;
		int timaout = numberOfPagies * 3000;

		logger.info("startAddress: 0x{}, length: {}, pages: {}, timeout: {}, pagesToErase: {}", Integer.toHexString(startAddress), length, numberOfPagies, timaout, pagesToErase);


		final Optional<byte[]> addCheckSum = addCheckSum(pagesToErase);

		if(addCheckSum.isPresent() && sendCommand(serialPort, FlashCommand.EXTENDED_ERASE).filter(answer->answer==FlashAnswer.ACK).isPresent())
			return sendBytes(serialPort, addCheckSum.get(), timaout);

		logger.debug("Cannot erase these pages({}). ", ()->DatatypeConverter.printHexBinary(addCheckSum.get()));
		return Optional.empty();
	}

	public static boolean writeBytes(SerialPort serialPort, final byte[] bytes) throws SerialPortException {
		logger.trace("size: {} - {}", bytes.length, bytes);
		serialPort.purgePort(SerialPort.PURGE_RXABORT | SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXABORT | SerialPort.PURGE_TXCLEAR);

		return serialPort.writeBytes(bytes);
	}

	public static Optional<byte[]> getControllerID(SerialPort serialPort) throws SerialPortException, SerialPortTimeoutException {

		if(!sendCommand(serialPort, FlashCommand.GET_ID).filter(answer->answer==FlashAnswer.ACK).isPresent())
			return Optional.empty();

		int numberOfBytes = readByte(serialPort, 1000)&0xFF;	//	number of bytes – 1

		return Optional
				.of(serialPort.readBytes(++numberOfBytes, 100));
	}

	public static byte[] readCommand(SerialPort serialPort, int timeout) throws SerialPortException, SerialPortTimeoutException {
		final byte[] toWrite = FlashCommand.READ_MEMORY.toBytes();
		logger.debug("Command read: {}", toWrite);

		writeBytes(serialPort, toWrite);

		if(!waitForACK(serialPort, timeout).filter(answer->answer==FlashAnswer.ACK).isPresent())
			return null;

		final UnitAddress[] addrs = UnitAddress.values();
		IntStream.range(1, addrs.length).map(i->addrs[i].getAddr()).mapToObj(addr->ByteBuffer.wrap(new byte[4]).putInt(addr).array());

		return null;
	}

	private static int count;
	public static Optional<FlashAnswer> waitForACK(SerialPort serialPort, int timeout) throws SerialPortException, SerialPortTimeoutException {

		final byte readByte = readByte(serialPort, timeout);
		final Optional<FlashAnswer> oFlashAnswer = FlashAnswer.valueOf(readByte);

		logger.debug(oFlashAnswer);

		if(!oFlashAnswer.isPresent()) {

			// Stop the process, too many attempts.
			if(count++>7) {
				count = 0;
				FlashController.showAlert("A lot of wrong answers", AlertType.ERROR);
				return Optional.empty();
			}

			FlashController.showAlert("Aswer is wrong: 0x" + DatatypeConverter.printHexBinary(new byte[] {readByte}), AlertType.ERROR);
			// Trying to get the right answer again.
			return waitForACK(serialPort, timeout);
		}else
			oFlashAnswer.filter(a->a!=FlashAnswer.ACK).ifPresent(a->FlashController.showAlert("Aswer is " + a, AlertType.ERROR));

		count = 0;
		return oFlashAnswer;
	}

	private static byte readByte(SerialPort serialPort, int timeout) throws SerialPortException, SerialPortTimeoutException {
		return logger.traceExit(serialPort.readBytes(1, timeout)[0]);
	}

	public static Optional<byte[]> addCheckSum(byte... original) {
		return Optional
				.ofNullable(original)
				.map(ByteBuffer.allocate(original.length + 1)::put)
				.map(bb->bb.put(getCheckSum(original)))
				.map(ByteBuffer::array);
	}

	public static byte getCheckSum(byte... original) {
		byte xor = 0;

		for (byte b : original)
			xor ^= b;

		return xor;
	}

	public static Optional<FlashAnswer> sendCommand(SerialPort serialPort, FlashCommand flashCommand) throws SerialPortException, SerialPortTimeoutException {
		logger.entry(flashCommand);

		return sendBytes(serialPort, flashCommand.toBytes(), 100);
	}

	public static Optional<FlashAnswer> sendBytes(SerialPort serialPort, byte[] bytes, int timeout) throws SerialPortException, SerialPortTimeoutException {

		if(!writeBytes(serialPort, bytes)) {

			final String message = "Can not send bytes: " + DatatypeConverter.printHexBinary(bytes);
			logger.debug(message);
			FlashController.showAlert(message, AlertType.ERROR);
			return Optional.empty();
		}

		//Wait for ACK
		return waitForACK(serialPort, timeout);
	}
}
