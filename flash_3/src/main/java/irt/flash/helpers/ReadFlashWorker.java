package irt.flash.helpers;

import static irt.flash.exception.ExceptionWrapper.catchConsumerException;
import static irt.flash.exception.ExceptionWrapper.catchFunctionException;

import java.nio.ByteBuffer;
import java.util.Optional;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import irt.flash.Flash3App;
import irt.flash.FlashController;
import irt.flash.data.FlashAnswer;
import irt.flash.data.FlashCommand;
import irt.flash.data.UnitAddress;
import irt.flash.exception.WrapperException;
import irt.flash.helpers.serial_port.IrtSerialPort;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.SingleSelectionModel;
import javafx.util.StringConverter;
import jssc.SerialPortTimeoutException;

public class ReadFlashWorker {

	private static final Logger logger = LogManager.getLogger();

	public static final int MAX_VAR_RAM_SIZE = 256;// Bytes
	public static byte[] MAX_BYTES_TO_READ = new byte[] { (byte) (MAX_VAR_RAM_SIZE-1), (byte) ((MAX_VAR_RAM_SIZE-1) ^ 0xFF) };

	private static ByteBuffer byteBuffer; 	public static ByteBuffer getByteBuffer() { return byteBuffer; }

	private static final int MAX_BUFFER_SIZE = 1000000;

	private IrtSerialPort serialPort;
	private DeviceWorker deviceWorker;

	private UploadWorker uploadWorker;

	private UnitAddress unitAddress;

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

					UnitAddress ua = (UnitAddress) ((ReadOnlyObjectProperty<?>)roop).getValue();

					if(ua==UnitAddress.PROGRAM)
						return;

					unitAddress = ua;

					logger.info("Read from Flash ({})", unitAddress);

					try {

						readFromFlash(unitAddress);

					} catch (SerialPortTimeoutException e) {

						logger.catching(Level.DEBUG, e);
						FlashController.showAlert("Connection error.", "Connection timeout", AlertType.ERROR);

					}catch (WrapperException e) {

			    		if(e.getCause() instanceof SerialPortTimeoutException) {

			    			logger.catching(Level.DEBUG, e);
			    			FlashController.showAlert("Connection error.", "Connection timeout", AlertType.ERROR);

			    		}else
			    			logger.catching(e);

			    	}catch (Exception e2) {
						logger.catching(e2);
					}

					selectionModel.select(0);
				});
	}

	public boolean readFromFlash() {

		if(unitAddress == null)
			return false;

		ThreadWorker.runThread(()->Optional.ofNullable(unitAddress).ifPresent(catchConsumerException(this::readFromFlash)));
		return true;
	}

	private void readFromFlash(UnitAddress unitAddress) throws Exception {
		logger.traceEntry("UnitAddress - {}", unitAddress);

		byteBuffer = null;

				// share the UnitAddresst with uploadWorker
			uploadWorker.setUnitAddress(unitAddress);
			Flash3App.setAppTitle(unitAddress.name());

			if(serialPort == null || !serialPort.isOpened()) {
				final String message = "Unit is not connected.";
				logger.debug(message);
				FlashController.showAlert("Connection error.", message, AlertType.ERROR);
				return;
			}

			byteBuffer = ByteBuffer.allocate(MAX_BUFFER_SIZE);

			if(readFromFlash(unitAddress.getAddr()))
					deviceWorker.setReadData(byteBuffer);
	}

	private boolean readFromFlash(final int addr) throws Exception{
		logger.debug("Read from address: 0x{}", ()->Integer.toHexString(addr));

		Optional<byte[]> oResult = FlashWorker.sendCommand(serialPort, FlashCommand.READ_MEMORY)

				.filter(answer->answer==FlashAnswer.ACK)

				//Bytes 3 to 7: Send the address + checksum
				.flatMap(a->FlashWorker.addCheckSum(UnitAddress.intToBytes(addr)))
				.flatMap(catchFunctionException(this::writeAndWaitForAck))
				.filter(answer->answer==FlashAnswer.ACK)

				//Bytes 8 to 9:Send the number of bytes to be read – 1 (0 < N ≤ 255) + checksum
				.flatMap(catchFunctionException(a->writeAndWaitForAck(MAX_BYTES_TO_READ)))
				.filter(answer->answer==FlashAnswer.ACK)
				.flatMap(
						catchFunctionException(
								answer->readBytes()));

						
		oResult.ifPresent(
				catchConsumerException(
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

							// Read from memory the next section (next 256 bytes)
							readFromFlash(addr + MAX_VAR_RAM_SIZE);
						}));

		return oResult.isPresent();
	}

	private Optional<byte[]> readBytes() throws Exception {

		return Optional.of(serialPort.read(MAX_VAR_RAM_SIZE));
	}

	private Optional<FlashAnswer> writeAndWaitForAck(byte[] bytes) throws Exception {

			return FlashWorker.sendBytes(serialPort, bytes);
	}

	public void setSerialPort(IrtSerialPort serialPort) {
		this.serialPort = serialPort;
	}

	public void setDeviceWorker(DeviceWorker deviceWorker) {
		this.deviceWorker = deviceWorker;
	}

	public void setUploadWorker(UploadWorker uploadWorker) {
		this.uploadWorker = uploadWorker;
	}
}
