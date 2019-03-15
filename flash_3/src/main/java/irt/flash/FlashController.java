package irt.flash;

import static irt.flash.exception.ExceptionWrapper.catchFunctionException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import javax.xml.bind.DatatypeConverter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import irt.flash.data.FlashAnswer;
import irt.flash.data.FlashCommand;
import irt.flash.data.UnitAddress;
import irt.flash.exception.WrapperException;
import irt.flash.helpers.ComPortWorker;
import irt.flash.helpers.DeviceWorker;
import irt.flash.helpers.FlashWorker;
import irt.flash.helpers.ProfileWorker;
import irt.flash.helpers.ReadFlashWorker;
import irt.flash.helpers.ThreadWorker;
import irt.flash.helpers.UploadWorker;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;

public class FlashController {

	private final static String PREFS_KEY_BAUDRATE = "BAUDRATE";
	private final static Logger logger = LogManager.getLogger();
	private final String SERIAL_PORT_IS_BUSY = "Serial port %s is busy.";

    @FXML private Button btnConnect;
	@FXML private ChoiceBox<String> chbPorts;
    @FXML private ChoiceBox<UnitAddress> chbRead;
    @FXML private ChoiceBox<Object> chbUpload;
    @FXML private ChoiceBox<ThreadWorker> chbEdit;
    @FXML private TextArea txtArea;

    private static Preferences prefs = Preferences.userNodeForPackage(FlashController.class);
	private static int baudRate = SerialPort.BAUDRATE_115200;
    private static Node node;
    private static FlashController controller;

    private ComPortWorker	 comPortWorker;
	private Optional<byte[]> controllerID;
	private ReadFlashWorker	 readFlashWorker;
	private UploadWorker	 uploadWorker;

    @FXML void initialize() throws IOException {
    	controller = this;
    	node = btnConnect;
    	baudRate = prefs.getInt(PREFS_KEY_BAUDRATE, SerialPort.BAUDRATE_115200);

    	final SingleSelectionModel<UnitAddress> selectionModel = chbRead.getSelectionModel();
		selectionModel.selectedIndexProperty().addListener(

				// Clear chbEdit on read from flash memory
				e->
				Optional.of(selectionModel.getSelectedIndex())
				.filter(si->si>0)
				.ifPresent(
						si->{
							final ObservableList<ThreadWorker> items = chbEdit.getItems();
							ThreadWorker edit = items.get(0);
							items.clear();
							items.add(edit);
							chbEdit.getSelectionModel().select(0);
						}));

    	comPortWorker = new ComPortWorker(chbPorts, btnConnect);
    	readFlashWorker = new ReadFlashWorker(chbRead);

    	final DeviceWorker deviceWorker = new DeviceWorker(txtArea);
        readFlashWorker.setDeviceWorker(deviceWorker);

    	uploadWorker = new UploadWorker(chbUpload, chbEdit);
    	readFlashWorker.setUploadWorker(uploadWorker);
    	deviceWorker.setUploadWorker(uploadWorker);

    	final ProfileWorker profileWorker = new ProfileWorker(chbEdit);
    	profileWorker.setUploadWorker(uploadWorker);
    	deviceWorker.setProfileWorker(profileWorker);
    	uploadWorker.setProfileWorker(profileWorker);

    	chbEdit.getSelectionModel().selectedItemProperty().addListener(
    			roop->{
    				final ReadOnlyObjectProperty<?> readOnlyObjectProperty = (ReadOnlyObjectProperty<?>)roop;
					final SelectionModel<?> sm = (SelectionModel<?>)readOnlyObjectProperty.getBean();
					final Object selectedItem = sm.getSelectedItem();
					Optional.ofNullable((ThreadWorker)selectedItem).ifPresent(ThreadWorker::start);
					sm.select(0);
    			});
    }

    @FXML
    void onBaudRate() {
    	List<Integer> choices = new ArrayList<>();
//       	choices.add(SerialPort.BAUDRATE_110);
//       	choices.add(SerialPort.BAUDRATE_300);
//       	choices.add(SerialPort.BAUDRATE_600);
       	choices.add(SerialPort.BAUDRATE_1200);
       	choices.add(SerialPort.BAUDRATE_4800);
       	choices.add(SerialPort.BAUDRATE_9600);
       	choices.add(SerialPort.BAUDRATE_14400);
       	choices.add(SerialPort.BAUDRATE_19200);
       	choices.add(SerialPort.BAUDRATE_38400);
       	choices.add(SerialPort.BAUDRATE_57600);
		choices.add(SerialPort.BAUDRATE_115200);
//       	choices.add(SerialPort.BAUDRATE_128000);
//       	choices.add(SerialPort.BAUDRATE_256000);

    	ChoiceDialog<Integer> dialog = new ChoiceDialog<>(baudRate, choices);
		Optional.ofNullable(node).map(Node::getScene).map(Scene::getWindow).ifPresent(dialog::initOwner);
    	dialog.setTitle("Choice Dialog");
    	dialog.setHeaderText(null);
    	dialog.setContentText("Choice Baud Rate");

     	dialog.showAndWait()
     	.ifPresent(
     			v->{
     				baudRate = v;
     				prefs.putInt(PREFS_KEY_BAUDRATE, v);
     			});
    }

    @FXML void onConnect(){

    	boolean setDisable = true;

    	try {

			setDisable  = comPortWorker.conect(baudRate )

					.map(
							catchFunctionException(
							sp->{
//								try {

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
									}catch (Exception e) { }

									return false;// enable
							}))
					.orElse(true);// disable


    	}catch (WrapperException e) {

    		if(e.getCause() instanceof SerialPortTimeoutException) {

    			logger.catching(Level.DEBUG, e);
    			showAlert("Connection error.", "Connection timeout", AlertType.ERROR);

    		}else
    			logger.catching(e);

    	} catch (SerialPortException e) {

			if(e.getMessage().contains("Port busy")) {

				final String format = String.format(SERIAL_PORT_IS_BUSY, e.getPortName());
				showAlert("Connection error.", format, AlertType.ERROR);
				return;

			}else
				logger.catching(e);

    	} catch (Exception e) {
			logger.catching(e);
		}
		setDisable(setDisable);
    }

    @FXML void onDragOwer(DragEvent event) {

    	Optional.of(chbRead).filter(cb->!cb.isDisabled())
    	.map(cb->event.getDragboard())
    	.filter(Dragboard::hasFiles)
    	.map(Dragboard::getFiles)
    	.filter(l->l.size()==1)
    	.map(l->l.get(0))
    	.filter(f->f.getName().toLowerCase().endsWith(".bin"))
    	.ifPresent(db->event.acceptTransferModes(TransferMode.LINK));
    	event.consume();
    }

    @FXML
    void onDragDropped(DragEvent event) {

    	Optional.of(event.getDragboard())
    	.map(Dragboard::getFiles)
    	.map(l->l.get(0))
    	.ifPresent(f->uploadWorker.uplodeToTheFlash(f.toPath()));

    	event.setDropCompleted(true);
    	event.consume();
    }

	void setDisable(boolean value) {
		chbRead.setDisable(value);
		chbUpload.setDisable(value);
		chbEdit.setDisable(value);
	}

	public static void disable(final boolean value) {
		logger.entry(value);
		Optional.ofNullable(controller).ifPresent(fc->Platform.runLater(()->fc.setDisable(value)));
	}

	private static Alert alert;
	public static void showAlert(String title, final String message, AlertType alertType) {

		Platform.runLater(
					()->{

						if(alert==null) {

							alert = new Alert(alertType);
							Optional.ofNullable(node).map(Node::getScene).map(Scene::getWindow).ifPresent(alert::initOwner);
							alert.setTitle(title);
							alert.setHeaderText(null);
							alert.setContentText(message);
							alert.showAndWait();
							alert = null;

						}else{

							alert.setAlertType(alertType);
							final String contentText = alert.getContentText() + '\n';
							alert.setContentText(contentText + message);

						}});
	}

	public static void closeAlert() {
		Optional.ofNullable(alert).ifPresent(al->Platform.runLater(()->al.close()));
	}

	private static Dialog<Void> progressBarDialog;
	private static ProgressIndicator progressBar;
	public static void showProgressBar() {
		if(progressBar==null)
			Platform.runLater(
					()->{

						progressBarDialog = new Dialog<>();

						final DialogPane dialogPane = progressBarDialog.getDialogPane();
						Optional.ofNullable(node).map(Node::getScene).map(Scene::getWindow).ifPresent(progressBarDialog::initOwner);
						dialogPane.setMinHeight(200);
						dialogPane.setMinWidth(200);
						Stage stage = (Stage) dialogPane.getScene().getWindow();
						stage.getIcons().add(new Image(FlashController.class.getResource("/images/progress_bar.png").toString()));

						progressBarDialog.setTitle("Upload progress.");
						progressBarDialog.setHeaderText(null);

						progressBar = new ProgressIndicator();
						AnchorPane ap = new AnchorPane();
						ap.getChildren().add(progressBar);
						AnchorPane.setLeftAnchor(progressBar, 0.0);
						AnchorPane.setRightAnchor(progressBar, 0.0);
						AnchorPane.setTopAnchor(progressBar, 0.0);
						AnchorPane.setBottomAnchor(progressBar, 0.0);

						dialogPane.setContent(ap);

						progressBarDialog.show();
						progressBarDialog.setOnCloseRequest(e->{
							progressBarDialog = null;
							progressBar = null;
						});
						scheduleTimer();
					});
	}

	private static Timer timer;
	private static void scheduleTimer() {
		ThreadWorker.runThread(
				()->{

					final TimerTask timerTask = new TimerTask() {
			
						@Override
						public void run() {
							Optional.ofNullable(progressBarDialog)
							.ifPresent(
									d->
									Platform.runLater(
											()->
											d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL)));
						}
					};

					synchronized(FlashController.class) {
						
						Optional.ofNullable(timer).ifPresent(Timer::cancel);
						timer = new Timer(true);
						timer.schedule(timerTask, TimeUnit.SECONDS.toMillis(20));
					}
		});
	}

	public static void setProgressBar(double value) {

		scheduleTimer();

		Optional.ofNullable(progressBar).ifPresent(pb->Platform.runLater(()->pb.setProgress(value)));
	}

	public static void closeProgressBar() {
		Optional.ofNullable(progressBarDialog).ifPresent(
				d->
				Platform.runLater(
						()->{

							final ObservableList<ButtonType> buttonTypes = d.getDialogPane().getButtonTypes();

							if(!buttonTypes.contains(ButtonType.CANCEL))
								buttonTypes.add(ButtonType.CANCEL);

							d.close(); }));
	}

	public void setGlobalOnKeyTyped() {
		btnConnect.getScene().setOnKeyReleased(
				e->{

					Optional.of(e)
					.filter(KeyEvent::isShortcutDown)
					.filter(v->v.getCode()==KeyCode.C)
					.map(v->txtArea.getSelection())
					.filter(s->s.getStart()==s.getEnd())
					.ifPresent(
							s->{
								btnConnect.fire();
								e.consume(); }); }); 
	}
}
