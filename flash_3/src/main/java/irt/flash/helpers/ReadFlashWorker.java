package irt.flash.helpers;

import java.nio.ByteBuffer;
import java.util.Optional;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import irt.flash.FlashController;
import irt.flash.data.FlashAnswer;
import irt.flash.data.FlashCommand;
import irt.flash.data.UnitAddress;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.SingleSelectionModel;
import javafx.util.StringConverter;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;

public class ReadFlashWorker {

	private static final Logger logger = LogManager.getLogger();

	public static final int MAX_VAR_RAM_SIZE = 256;// Bytes
	public static byte[] MAX_BYTES_TO_READ = new byte[] { (byte) (MAX_VAR_RAM_SIZE-1), (byte) ((MAX_VAR_RAM_SIZE-1) ^ 0xFF) };

	private static ByteBuffer byteBuffer; 	public static ByteBuffer getByteBuffer() { return byteBuffer; }

	private static final int MAX_BUFFER_SIZE = 1000000;

	private SerialPort serialPort;
	private DeviceWorker deviceWorker;

	private UploadWorker uploadWorker;

	public ReadFlashWorker(ChoiceBox<UnitAddress> chbRead) {
		final UnitAddress[] addresses = UnitAddress.values();
		final ObservableList<UnitAddress> observableArrayList = FXCollections.observableArrayList(addresses);
		chbRead.setConverter(new StringConverter<UnitAddress>() {
			
			@Override
			public String toString(UnitAddress object) {
				return Optional
						.of(object)
						.filter(ua->ua!=UnitAddress.PROGRAM)
						.map(UnitAddress::toString)
						.orElse("Read ...");
			}
			
			@Override public UnitAddress fromString(String string) { return null; }
		});
		chbRead.setItems(observableArrayList);
		final SingleSelectionModel<UnitAddress> selectionModel = chbRead.getSelectionModel();
		selectionModel.select(0);
		selectionModel.selectedItemProperty().addListener(
				roop->{

					final UnitAddress unitAddress = (UnitAddress) ((ReadOnlyObjectProperty<?>)roop).getValue();

					if(unitAddress==UnitAddress.PROGRAM)
						return;

					logger.info("Read from {}", unitAddress);

					readFromFlash(unitAddress);

					selectionModel.select(0);
				});
	}

	private void readFromFlash(UnitAddress unitAddress) {

			// share the UnitAddresst with uploadWorker
			uploadWorker.setUnitAddress(unitAddress);

			if(serialPort == null || !serialPort.isOpened()) {
				final String message = "Unit is not connected.";
				logger.debug(message);
				FlashController.showAlert(message);
				return;
			}

			byteBuffer = ByteBuffer.allocate(MAX_BUFFER_SIZE);

			try {

				readFromFlash(unitAddress.getAddr());
				deviceWorker.setReadData(byteBuffer);
				byteBuffer = null;

			} catch (SerialPortException e) {
				logger.catching(e);
			} catch (SerialPortTimeoutException e) {
				logger.catching(Level.DEBUG, e);
				FlashController.showAlert("Connection timeout");
			}
	}

	private void readFromFlash(final int addr) throws SerialPortException, SerialPortTimeoutException {
		logger.debug("Read from address: 0x{}", ()->Integer.toHexString(addr));

		if(!FlashWorker.sendCommand(serialPort, FlashCommand.READ_MEMORY).filter(answer->answer==FlashAnswer.ACK).isPresent()) {
			return;
		}

		//Bytes 3 to 7: Send the address + checksum
		FlashWorker.addCheckSum(UnitAddress.intToBytes(addr))				
		.flatMap(this::writeAndWaitForAck)

		//Bytes 8 to 9:Send the number of bytes to be read – 1 (0 < N ≤ 255) + checksum
		.flatMap(a->writeAndWaitForAck(MAX_BYTES_TO_READ))
		.flatMap(size->readBytes())
		.ifPresent(
				b->{

					byteBuffer.put(b);

					// check end of the array for 0xFF ( it means no more data )
					int i;
					final int repeats = 7;
					for(i = 1; i<repeats; i++) {
	
						if(b[b.length - i]!=(byte)0xFF)
							break;
					}

					//if no more data return
					if(i>=repeats)
						return;

					try {

						// Read from memory the next section (next 256 bytes)
						readFromFlash(addr + MAX_VAR_RAM_SIZE);

					} catch (SerialPortException e) {
						logger.catching(e);
					} catch (SerialPortTimeoutException e) {
						logger.catching(Level.DEBUG, e);
						FlashController.showAlert("Connection timeout");
					}
				});
	}

	private Optional<byte[]> readBytes() {
		try {

			return Optional.of(serialPort.readBytes(MAX_VAR_RAM_SIZE, 10000));

		} catch (SerialPortException e) {
			logger.catching(e);
		} catch ( SerialPortTimeoutException e) {
			logger.catching(Level.DEBUG, e);
			FlashController.showAlert("Connection timeout");
		}
		return Optional.empty();
	}

	private Optional<FlashAnswer> writeAndWaitForAck(byte[] bytes) {

		try {

			return FlashWorker.sendBytes(serialPort, bytes, 500);

		} catch (SerialPortException e) {
			logger.catching(e);
		} catch (SerialPortTimeoutException e) {
			logger.catching(Level.DEBUG, e);
			FlashController.showAlert("Connection timeout");
		}
		return Optional.empty();
	}

	public void setSerialPort(SerialPort serialPort) {
		this.serialPort = serialPort;
	}

	public void setDeviceWorker(DeviceWorker deviceWorker) {
		this.deviceWorker = deviceWorker;
	}

	public void setUploadWorker(UploadWorker uploadWorker) {
		this.uploadWorker = uploadWorker;
	}
}
