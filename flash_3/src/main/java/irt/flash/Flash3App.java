package irt.flash;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Optional;
import java.util.Properties;
import java.util.prefs.Preferences;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import irt.flash.helpers.StageSizeAndPosition;
import irt.flash.helpers.serial_port.ComPortWorker;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;

public class Flash3App extends Application {

	private final static Logger logger = LogManager.getLogger();
	private final static Preferences prefs = Preferences.userNodeForPackage(Flash3App.class);
	public final static String IS_CLOSED_PROPERLY = "Flash3 is closed";

    public static void main(String[] args) throws Exception {
        launch(args);
    }

    private static Stage stage;

	private String version;
	private StageSizeAndPosition size = new StageSizeAndPosition();
	public static Properties properties;

    @Override
	public void init() throws Exception {
		properties = new Properties();
		properties.load(getClass().getResourceAsStream("/project.properties"));
		version = properties.getProperty("version");

		Thread.setDefaultUncaughtExceptionHandler((t, e) -> Platform.runLater(() -> showErrorDialog(t, e)));
		Thread.currentThread().setUncaughtExceptionHandler(this::showErrorDialog);
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

		prefs.putBoolean(IS_CLOSED_PROPERLY, true);
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

	public static void setAppTitle(String UnitType) {
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

    private void showErrorDialog(Thread t, Throwable e) {
    	logger.catching(e);

    	StringWriter errorMsg = new StringWriter();
    	e.printStackTrace(new PrintWriter(errorMsg));

    	Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Error.fxml"));

        try {

        	Parent root = loader.load();
            ((ErrorController)loader.getController()).setErrorText(errorMsg.toString());
            dialog.setScene(new Scene(root, 500, 400));
            dialog.show();

        } catch (IOException exc) {
            logger.catching(exc);
        }
	}

	public final static String SERIAL_PORT_IS_BUSY	 = "Serial port %s is busy.";
    UncaughtExceptionHandler uncaughtExceptionHandler = (thread, throwable)->{

    	SerialPortException serialPortException = getException(SerialPortException.class, throwable);

    	if(serialPortException != null && serialPortException.getMessage().contains("Port busy")) {

    		logger.catching(Level.DEBUG, throwable);
			final String message = String.format(SERIAL_PORT_IS_BUSY, serialPortException.getPortName());
			logger.warn(message);
			FlashController.showAlert("Connection error.", message, AlertType.ERROR);
			return;
    	}

    	final SerialPortTimeoutException serialPortTimeoutException = getException(SerialPortTimeoutException.class, throwable);
    	if(serialPortTimeoutException != null) {

    		logger.catching(Level.DEBUG, throwable);
    		FlashController.showAlert("Connection error.", "Connection timeout", AlertType.ERROR);
    		return;
    	}
    	logger.catching(throwable);
    };
}
