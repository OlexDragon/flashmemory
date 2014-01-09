package irt.flash.data.connection;

import irt.flash.data.connection.MicrocontrollerSTM32.Answer;
import irt.flash.presentation.panel.ConnectionPanel;

import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;

import jssc.SerialPortException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class FlashConnector implements Observer{

	private static final Logger logger = (Logger) LogManager.getLogger();

	protected static final Preferences prefs = Preferences.userRoot().node("IRT Technologies inc.");

	private static FlashSerialPort serialPort;
	private static FlashConnector connector = new FlashConnector();
	private static boolean isConnected;

	private FlashConnector(){ }

	public static FlashConnector getConnector(){
		return connector;
	}

	public static void connect() throws SerialPortException, InterruptedException {
		logger.entry();

		String serialPortStr = prefs.get(ConnectionPanel.SERIAL_PORT, ConnectionPanel.SELECT_SERIAL_PORT);
		if(!serialPortStr.equals(ConnectionPanel.SELECT_SERIAL_PORT)){
			disconnect();
			serialPort = new FlashSerialPort(serialPortStr);
			if(serialPort.openPort()){
				MicrocontrollerSTM32.getInstance(serialPort).addObserver(getConnector());
				MicrocontrollerSTM32.connect();
			}
		}
		logger.exit();
	}

	public static void disconnect() throws SerialPortException {
		if(serialPort!=null && serialPort.isOpened())
			serialPort.closePort();
		isConnected = false;
		serialPort = null;
	}

	public static boolean isConnected() {
		return isConnected;
	}

	@Override
	public void update(Observable o, Object obj) {
		logger.entry(o, obj);

		if (obj == null){
			if (o instanceof MicrocontrollerSTM32) {
				MicrocontrollerSTM32 stm32 = (MicrocontrollerSTM32) o;
				switch (stm32.getCommand()) {
				case CONNECT:
					setConnected(stm32.getReadBytes());
					break;
				case ERASE:
					break;
				case EXTENDED_ERASE:
					break;
				case GET:
					break;
				case READ_MEMORY:
					break;
				case WRITE_MEMORY:
					break;
				case USER_COMMAND:
					break;
				default:
					break;
				}
			}
		}
	}

	private void setConnected(byte[] bytes) {
		
		if(bytes!=null && bytes.length==1)
			isConnected = bytes[0]==Answer.ACK.getAnswer();
		else
			isConnected = false;
	}

	public static void write(String serialPortStr, byte[] bytes) throws SerialPortException {
		serialPort = new FlashSerialPort(serialPortStr);
		if(serialPort.openPort()){
			serialPort.writeBytes(bytes);
			disconnect();
		}
	}
}
