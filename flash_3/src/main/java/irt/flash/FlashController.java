package irt.flash;

import java.io.IOException;
import java.util.Optional;

import javax.xml.bind.DatatypeConverter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import irt.flash.data.FlashAnswer;
import irt.flash.data.FlashCommand;
import irt.flash.data.UnitAddress;
import irt.flash.helpers.ComPortWorker;
import irt.flash.helpers.DeviceWorker;
import irt.flash.helpers.FlashWorker;
import irt.flash.helpers.ReadFlashWorker;
import irt.flash.helpers.UploadWorker;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;

public class FlashController {

	private final Logger logger = LogManager.getLogger();
	private final String SERIAL_PORT_IS_BUSY = "Serial port %s is busy.";

    @FXML private Button btnConnect;
	@FXML private ChoiceBox<String> chbPorts;
    @FXML private ChoiceBox<UnitAddress> chbRead;
    @FXML private ChoiceBox<Object> chbUpload;
    @FXML private ChoiceBox<String> chbDeviceGroup;
    @FXML private ChoiceBox<String> chbDeviceType;
    @FXML private TextArea txtArea;

    private static FlashController controller;

    private ComPortWorker comPortWorker;
	private Optional<byte[]> controllerID;
	private ReadFlashWorker readFlashWorker;
	private UploadWorker uploadWorker;

    @FXML void initialize() throws IOException {
    	controller = this;

    	comPortWorker = new ComPortWorker(chbPorts, btnConnect);
    	readFlashWorker = new ReadFlashWorker(chbRead);

    	uploadWorker = new UploadWorker(chbUpload);
    	readFlashWorker.setUploadWorker(uploadWorker);

    	final DeviceWorker deviceWorker = new DeviceWorker(chbDeviceGroup, chbDeviceType, txtArea);
        readFlashWorker.setDeviceWorker(deviceWorker);

    	deviceWorker.setUploadWorker(uploadWorker);
    }

    @FXML void onConnect(){
    	try {

			final Boolean setDisable = comPortWorker.conect()

					.map(
							sp->{
								try {

									readFlashWorker.setSerialPort(sp);
									uploadWorker.setSerialPort(sp);
									final Optional<FlashAnswer> oAnswer = FlashWorker.sendCommand(sp, FlashCommand.CONNECT);

									if(!oAnswer.isPresent())
										return false;

									synchronized (this) { try { wait(100); } catch (InterruptedException e) { } }

									//Maybe I will use this id.
									try {
										controllerID = FlashWorker.getControllerID(sp);
										logger.info("controller ID: {}", controllerID.map(DatatypeConverter::printHexBinary).map(id->"0x" + id).orElse("no ID"));
									}catch(Exception e) { }

									return false;// enable

								} catch (SerialPortException e) {
									logger.catching(e);
								}catch(SerialPortTimeoutException e) {
									logger.catching(Level.DEBUG, e);
									showAlert("Connection timeout");
								}

								return true;// disable
							})
					.orElse(true);// disable

			setDisable(setDisable);

    	} catch (SerialPortException e) {

			if(e.getMessage().contains("Port busy")) {

				final String format = String.format(SERIAL_PORT_IS_BUSY, e.getPortName());
				showAlert(format);
				return;

			}else
				logger.catching(e);
		}
    }

    @FXML void onRead() {
    	final Optional<byte[]> readFromUnit = comPortWorker.getSerialPort().map(
    			t->{
    				try {
    					return FlashWorker.readCommand(t, 1000);
    				} catch (SerialPortException e) {
    					logger.catching(e);
    				} catch (SerialPortTimeoutException e) {
    					logger.catching(Level.DEBUG, e);
					}
    				return null;
    			});

    	if(!readFromUnit.isPresent()) {
			final String message = "Unable to read from device. Try again.";
			logger.debug(message);
			showAlert(message);
		}
    }

	void setDisable(boolean value) {
		chbRead.setDisable(value);
		chbUpload.setDisable(value);
		chbDeviceGroup.setDisable(value);
		chbDeviceType.setDisable(value);
	}

	public static void disable(final boolean value) {
		Optional.ofNullable(controller).ifPresent(fc->Platform.runLater(()->fc.setDisable(value)));
	}

	public static void showAlert(final String message) {
		Platform.runLater(()->{

			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Connection error.");
			alert.setHeaderText(null);
			alert.setContentText(message);
			alert.showAndWait();
		});
	}

}
