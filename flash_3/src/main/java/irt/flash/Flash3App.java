package irt.flash;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Optional;
import java.util.Properties;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import irt.flash.helpers.ComPortWorker;
import irt.flash.helpers.StageSizeAndPosition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;

public class Flash3App extends Application {

	private final static Logger logger = LogManager.getLogger();

    public static void main(String[] args) throws Exception {
        launch(args);
    }

    private static Stage stage;

	private String version;
	private StageSizeAndPosition size = new StageSizeAndPosition(getClass());
	public static Properties properties;

    @Override
	public void init() throws Exception {
		properties = new Properties();
		properties.load(getClass().getResourceAsStream("/project.properties"));
		version = properties.getProperty("version");
	}

	public void start(Stage stage) throws Exception {

    	Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);

		Flash3App.stage = stage;
		final ObservableList<Image> icons = stage.getIcons();

		for(int i=16; i<=512; i*=2) 
			icons.add(new Image(getClass().getResourceAsStream("/images/flash_" + i + ".png")));

		String fxmlFile = "/fxml/Flash.fxml";
        FXMLLoader loader = new FXMLLoader();
        Parent rootNode = (Parent) loader.load(getClass().getResourceAsStream(fxmlFile));

        Scene scene = new Scene(rootNode);
        scene.getStylesheets().add("/styles/styles.css");

        stage.setTitle("Flash v " + version);
        stage.setScene(scene);
        size.setStageProperties(stage);
        stage.show();

        final FlashController controller = (FlashController)loader.getController();
        controller.setGlobalOnKeyTyped();
    }

	@Override
	public void stop() throws Exception {
		size.saveStageProperties();
		ComPortWorker.disconect();
	}

	public static void setSerialNumber(String serialNumber) {
		Optional.ofNullable(stage).ifPresent(
				st->{
					Platform.runLater(
							()->{
								final String t = st.getTitle().split(" : ")[0];
								st.setTitle(t + " : " + serialNumber); });
		});
	}

	public static void setUnitType(String UnitType) {
		Optional.ofNullable(stage).ifPresent(
				st->{
					Platform.runLater(
							()->{
								final String t = st.getTitle().split(" - ")[0];
								st.setTitle(t + " - " + UnitType); });
		});
	}

	public static void se(String UnitType) {
		Optional.ofNullable(stage).ifPresent(
				st->{
					Platform.runLater(
							()->{
								final String t = st.getTitle().split(" - ")[0];
								st.setTitle(t + " - " + UnitType); });
		});
	}

	private<T> T getException(Class<T> returnClass, Throwable throwable) {

		if(throwable == null)
			return null;

		if(returnClass.isInstance(throwable))
			return returnClass.cast(throwable);

		return getException(returnClass, throwable.getCause());
	}

	public final static String SERIAL_PORT_IS_BUSY	 = "Serial port %s is busy.";
    UncaughtExceptionHandler uncaughtExceptionHandler = (thread, throwable)->{

    	SerialPortException serialPortException = getException(SerialPortException.class, throwable);

    	if(serialPortException != null && serialPortException.getMessage().contains("Port busy")) {

    		logger.catching(Level.DEBUG, throwable);
			final String format = String.format(SERIAL_PORT_IS_BUSY, serialPortException.getPortName());
			FlashController.showAlert("Connection error.", format, AlertType.ERROR);
			return;
    	}

    	final SerialPortTimeoutException serialPortTimeoutException = getException(SerialPortTimeoutException.class, throwable);
    	if(serialPortTimeoutException != null) {

    		logger.catching(Level.DEBUG, throwable);
    		FlashController.showAlert("Connection error.", "Connection timeout", AlertType.ERROR);
    		return;
    	}
    	logger.error(thread);
    	logger.catching(throwable);
    };
}
