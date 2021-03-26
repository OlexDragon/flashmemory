package irt.flash.helpers.serial_port;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import irt.flash.Flash3App;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tooltip;
import jssc.SerialPort;
import jssc.SerialPortList;
import lombok.Getter;
import lombok.Setter;

public class ComPortWorker {

	private static final Logger logger = LogManager.getLogger();

	private final static List<IrtSerialPort> ports = new ArrayList<>();
	private final static Preferences prefs = Preferences.userNodeForPackage(Flash3App.class);

	private ChoiceBox<String> chbPorts;
	private Button btnConnect;

	@Getter @Setter
	private static Class<?> serialPortClass = SerialPortjSerialComm.class;
//	private static Class<? extends IrtSerialPort> serialPortClass = SerialPortJssc.class;

	public ComPortWorker(ChoiceBox<String> chbPorts, Button btnConnect) throws IOException {

		this.chbPorts = chbPorts;
		this.btnConnect = btnConnect;

		final String[] serialPorts = SerialPortList.getPortNames();
		final ObservableList<String> observableArrayList = FXCollections.observableArrayList(serialPorts);
		chbPorts.setItems(observableArrayList);

		final SingleSelectionModel<String> selectionModel = chbPorts.getSelectionModel();

		Optional.ofNullable(prefs.get("Selected Serial Port", null)).ifPresent(
				portName->{
					selectionModel.select(portName);
					final int selectedIndex = selectionModel.getSelectedIndex();
					btnConnect.setDisable(selectedIndex<0);
				});

		selectionModel.selectedItemProperty().addListener(
				e->{
					prefs.put("Selected Serial Port", (String) ((ReadOnlyObjectProperty<?>)e).getValue());
					btnConnect.setDisable(false);
				});
	}

	public Optional<IrtSerialPort> conect(int baudRate) throws Exception{

		// Close the serial port if open
		if(closeComPort().isPresent()) 
     		return Optional.empty();

		// Create and open a new serial port
		final String portName = chbPorts.getSelectionModel().getSelectedItem();

		final Constructor<?> constructor = serialPortClass.getConstructor(String.class);
		final IrtSerialPort serialPort = (IrtSerialPort) constructor.newInstance(portName);
		if(serialPort.openPort()) {
			serialPort.setParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_EVEN);

			Platform.runLater(
					()->{
						btnConnect.setText("Disconnect");
						btnConnect.setTooltip(new Tooltip(serialPort.getPortName()));
						chbPorts.setUserData(serialPort);
					});
			ports.add(serialPort);

			return Optional.of(serialPort);
		}
		logger.error("1");

 		return Optional.empty();
	}

	private Optional<IrtSerialPort> closeComPort() {

		final Optional<IrtSerialPort> oComPort = Optional.ofNullable((IrtSerialPort)chbPorts.getUserData());
		oComPort.ifPresent(
				sp->{
					try {
						sp.closePort();
					} catch (Exception e) {
						logger.catching(e);
					}
					Platform.runLater(
							()->{
								chbPorts.setUserData(null);
								btnConnect.setText("Connect");
								btnConnect.setTooltip(null);
							});
				});
		return oComPort;
	}

	public static void disconect() {
		ports.stream().filter(IrtSerialPort::isOpened).forEach(sp->{
			try {
				sp.closePort();
			} catch (Exception e) {
				logger.catching(e);
			}
		});
		ports.clear();
	}

	public Optional<SerialPort> getSerialPort() {
		return Optional.ofNullable((SerialPort)chbPorts.getUserData());
	}
}
