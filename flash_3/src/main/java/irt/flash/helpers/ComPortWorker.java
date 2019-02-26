package irt.flash.helpers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tooltip;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

public class ComPortWorker {

	private static final Logger logger = LogManager.getLogger();

	private final static List<SerialPort> ports = new ArrayList<>();

	private ChoiceBox<String> chbPorts;
	private Preferences prefs;
	private Button btnConnect;

	public ComPortWorker(ChoiceBox<String> chbPorts, Button btnConnect) throws IOException {

		this.chbPorts = chbPorts;
		this.btnConnect = btnConnect;

		prefs = Preferences.userNodeForPackage(getClass());

		final String[] serialPorts = SerialPortList.getPortNames();
		final ObservableList<String> observableArrayList = FXCollections.observableArrayList(serialPorts);
		chbPorts.setItems(observableArrayList);

		final SingleSelectionModel<String> selectionModel = chbPorts.getSelectionModel();

		Optional.ofNullable(prefs.get("Selected Serial Port", null)).ifPresent(
				portName->{
					selectionModel.select(portName);
					btnConnect.setDisable(selectionModel.getSelectedIndex()<0);
				});

		selectionModel.selectedItemProperty().addListener(
				e->{
					prefs.put("Selected Serial Port", (String) ((ReadOnlyObjectProperty<?>)e).getValue());
					btnConnect.setDisable(false);
				});
	}

	public Optional<SerialPort> conect() throws SerialPortException{

		if(closeComPort().isPresent()) 
     		return Optional.empty();

		final String portName = chbPorts.getSelectionModel().getSelectedItem();

		final SerialPort serialPort = new SerialPort(portName);
		if(serialPort.openPort()) {
			serialPort.setParams(SerialPort.BAUDRATE_115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_EVEN);
			btnConnect.setText("Disconnect");
			btnConnect.setTooltip(new Tooltip(serialPort.getPortName()));
			chbPorts.setUserData(serialPort);
			ports.add(serialPort);

			return Optional.of(serialPort);
		}

 		return Optional.empty();
	}

	private Optional<SerialPort> closeComPort() {
		final Optional<SerialPort> oComPort = Optional.ofNullable((SerialPort)chbPorts.getUserData());
		oComPort.map(SerialPort.class::cast).ifPresent(
				sp->{
					try {
						sp.closePort();
					} catch (SerialPortException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					chbPorts.setUserData(null);
					btnConnect.setText("Connect");
					btnConnect.setTooltip(null);
				});
		return oComPort;
	}

	public static void disconect() {
		ports.stream().filter(SerialPort::isOpened).forEach(sp->{
			try {
				sp.closePort();
			} catch (SerialPortException e) {
				logger.catching(e);
			}
		});
		ports.clear();
	}

	public Optional<SerialPort> getSerialPort() {
		return Optional.ofNullable((SerialPort)chbPorts.getUserData());
	}
}
