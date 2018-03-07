package irt.flash.data.connection;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import java.util.TooManyListenersException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.prefs.Preferences;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

import irt.flash.data.MyThreadFactory;
import irt.flash.data.connection.MicrocontrollerSTM32.Answer;
import irt.flash.presentation.panel.ConnectionPanel;
import purejavacomm.NoSuchPortException;
import purejavacomm.PortInUseException;
import purejavacomm.UnsupportedCommOperationException;

public class FlashConnector implements Observer{

	private static final Logger logger = (Logger) LogManager.getLogger();

	protected static final Preferences prefs = Preferences.userRoot().node("IRT Technologies inc.");

	protected final static ExecutorService service = Executors.newSingleThreadScheduledExecutor(new MyThreadFactory());

	private static FlashSerialPort serialPort;
	private static FlashConnector connector = new FlashConnector();
	private static ConnectionStatus connectionStatus = ConnectionStatus.NOT_CONNECTED;

	private static FutureTask<ConnectionStatus> futureTask;

	private FlashConnector(){
		logger.info("* Start *");
	}

	public static FlashConnector getConnector(){
		return connector;
	}

	public static FutureTask<ConnectionStatus> connect() throws InterruptedException, NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException, TooManyListenersException {
		logger.entry();

		String serialPortStr = prefs.get(ConnectionPanel.SERIAL_PORT, ConnectionPanel.SELECT_SERIAL_PORT);
		if(!serialPortStr.equals(ConnectionPanel.SELECT_SERIAL_PORT)){
			disconnect();
			serialPort = new FlashSerialPort(serialPortStr);
			if(serialPort.openPort()){
				connectionStatus = ConnectionStatus.NOT_CONNECTED;
				MicrocontrollerSTM32.getInstance(serialPort).addObserver(connector);
				MicrocontrollerSTM32.connect();
			}
		}

		logger.trace("EXIT Serial port {} is opend: {}", serialPort, serialPort.isOpened());

		return futureTask =

				new FutureTask<>(new Callable<ConnectionStatus>() {

					@Override
					public ConnectionStatus call() throws Exception {
						return connectionStatus;
					}
				});
	}

	public static void disconnect() {
		if(serialPort!=null)
			serialPort.closePort();
		connectionStatus = ConnectionStatus.NOT_CONNECTED;
		serialPort = null;
	}

	public static ConnectionStatus getConnectionStatus() {
		return connectionStatus;
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
		logger.trace("ENTRY {}", bytes);
		
		Optional
		.ofNullable(bytes)
		.filter(bs->bs.length==1)
		.map(bs->bs[0]==Answer.ACK.getAnswer())
		.ifPresent(ack->{
			connectionStatus = ack ? ConnectionStatus.CONNECTED : ConnectionStatus.NOT_CONNECTED;
			Optional.ofNullable(futureTask).ifPresent(service::execute);
			logger.trace("connectionStatus: {};", connectionStatus);
		});

	}

	public static void write(String serialPortStr, byte[] bytes) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException, TooManyListenersException{
		if(serialPort!=null && serialPort.isOpened())
			disconnect();

		serialPort = new FlashSerialPort(serialPortStr);

		if(serialPort.openPort()){
			serialPort.writeBytes(bytes);
			disconnect();
		}
	}

	public enum ConnectionStatus{
		NOT_CONNECTED,
		CONNECTED,
		CONNECTING;
	}
}
