package irt.flash;

import static irt.flash.exception.ExceptionWrapper.catchFunctionException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

import javax.xml.bind.DatatypeConverter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import irt.flash.data.FlashAnswer;
import irt.flash.data.FlashCommand;
import irt.flash.data.UnitAddress;
import irt.flash.exception.WrapperException;
import irt.flash.helpers.DeviceWorker;
import irt.flash.helpers.FlashWorker;
import irt.flash.helpers.ProfileWorker;
import irt.flash.helpers.ReadFlashWorker;
import irt.flash.helpers.ThreadWorker;
import irt.flash.helpers.UploadWorker;
import irt.flash.helpers.serial_port.ComPortWorker;
import irt.flash.helpers.serial_port.SerialPortJssc;
import irt.flash.helpers.serial_port.SerialPortjSerialComm;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
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
import javafx.scene.control.IndexRange;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;

public class FlashController {

	public static final int MAX_WAIT_TIME_IN_MINUTES = 1;

	private final static String PREFS_KEY_BAUDRATE = "BAUDRATE";
	private final static Logger logger = LogManager.getLogger();
	private final String SERIAL_PORT_IS_BUSY = "Serial port %s is busy.";

    @FXML private Button btnConnect;
	@FXML private ChoiceBox<String> chbPorts;
    @FXML private ChoiceBox<UnitAddress> chbRead;
    @FXML private ChoiceBox<Object> chbUpload;
    @FXML private ChoiceBox<ThreadWorker> chbEdit;
    @FXML private TextArea txtArea;
    @FXML private RadioMenuItem menuJssc;
    @FXML private RadioMenuItem menuJSerialComm;

	private final static Preferences prefs = Preferences.userNodeForPackage(Flash3App.class);

	private static final String SELECTED_MENU = "selected driver";
	private static int baudRate = SerialPort.BAUDRATE_115200;
    private static Node node;
    private static FlashController controller;

    private ComPortWorker	 comPortWorker;
	private Optional<byte[]> controllerID;
	private ReadFlashWorker	 readWorker;
	private UploadWorker	 uploadWorker;

    @FXML public void initialize() throws IOException {
    	controller = this;
    	node = btnConnect;
    	baudRate = prefs.getInt(PREFS_KEY_BAUDRATE, SerialPort.BAUDRATE_115200);

    	chbPorts.focusedProperty().addListener(focusListener);
    	chbRead.focusedProperty().addListener(focusListener);
    	chbUpload.focusedProperty().addListener(focusListener);
    	chbEdit.focusedProperty().addListener(focusListener);

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
    	readWorker = new ReadFlashWorker(chbRead);

    	final DeviceWorker deviceWorker = new DeviceWorker(txtArea);
        readWorker.setDeviceWorker(deviceWorker);

    	uploadWorker = new UploadWorker(chbUpload, chbEdit);
    	readWorker.setUploadWorker(uploadWorker);
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

    	// Setup serial port menu
    	ToggleGroup group = new ToggleGroup();

    	menuJssc.setUserData(SerialPortJssc.class);
    	menuJssc.setToggleGroup(group);

    	menuJSerialComm.setUserData(SerialPortjSerialComm.class);
    	menuJSerialComm.setToggleGroup(group);

    	final Optional<String> oSelected = Optional.ofNullable(prefs.get(SELECTED_MENU, null));

    	final Stream<RadioMenuItem> stream = Stream.of(menuJssc, menuJSerialComm);
		if(oSelected.isPresent()) {

    		stream.filter(mi->mi.getId().equals(prefs.get(SELECTED_MENU, null))).findAny()
    		.ifPresent(
    				mi->{
    					mi.setSelected(true);
    					ComPortWorker.setSerialPortClass((Class<?>) mi.getUserData());
    				});

		}else {

			stream.filter(mi->mi.getUserData().equals(ComPortWorker.getSerialPortClass())).findAny()
       		.ifPresent(mi->mi.setSelected(true));
    	}

		// Checking if this application is closed correctly. If not show the message.
		Platform.runLater(
				()->{
					// Check if this application close properly
					boolean guiBeenClosedProperly = prefs.getBoolean(Flash3App.IS_CLOSED_PROPERLY, true);
					prefs.putBoolean(Flash3App.IS_CLOSED_PROPERLY, false);

					if(!guiBeenClosedProperly) {

						ChoiceDialog<Class<?>> alert = new ChoiceDialog<>(ComPortWorker.getSerialPortClass(), SerialPortJssc.class, SerialPortjSerialComm.class);
						alert.setTitle("The GUI was not closed properly.");
						alert.setHeaderText("Try to select a different serial port driver.");
						Node comboBox = (Node) alert.getDialogPane().lookup(".combo-box");
						logger.error(comboBox);//TODO add conversation
						Optional<Class<?>> oClass = alert.showAndWait();

						if(!oClass.isPresent())
							return;

						ComPortWorker.setSerialPortClass(oClass.get());
						Stream.of(menuJssc, menuJSerialComm).filter(mi->mi.getUserData().equals(ComPortWorker.getSerialPortClass())).findAny()
						.ifPresent(
								mi->{
									mi.setSelected(true);
									prefs.put(SELECTED_MENU, mi.getId());
								});
					}
				});
    }

    @FXML public  void onBaudRate() {
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

    private Thread thread;
    @FXML public void onConnect(){
    	Optional.ofNullable(thread).filter(Thread::isAlive).filter(t->!t.isInterrupted()).ifPresent(Thread::interrupt);

    	thread = ThreadWorker.runThread(
    			()->{
    		    	boolean setDisable = true;

    		    	try {

    					setDisable  = comPortWorker.conect(baudRate )

    							.map(
    									catchFunctionException(
    											sp->{

    												logger.error(sp);
    												showAlert("Connecting.", "Connection to the unit.", AlertType.INFORMATION);

    												readWorker.setSerialPort(sp);
    												uploadWorker.setSerialPort(sp);
    												final Optional<FlashAnswer> oAnswer = FlashWorker.sendCommand(sp, FlashCommand.CONNECT).filter(answer->answer.ordinal()>FlashAnswer.NULL.ordinal());

    												if(!oAnswer.isPresent()) {
        												showAlert("Connecting.", "It is not possible to connect to the unit.", AlertType.WARNING);
    													return true;	//disable
    												}

    												Thread.sleep(100);
    												closeAlert();

    												//Maybe I will use this id.
    												try {
    													controllerID = FlashWorker.getControllerID(sp);
    													final String cId = "controller ID: " + controllerID.map(DatatypeConverter::printHexBinary).map(id->"0x" + id).orElse("no ID");
    													Flash3App.setAppTitle(cId);

    												}catch (Exception e) { logger.catching(e); }

    							    				return false;// enable
    											}))
    							.orElse(true);// disable


    		    	}catch (WrapperException e) {

    		    		final Throwable cause = e.getCause();

    		    		if(cause instanceof SerialPortTimeoutException) {

    		    			logger.catching(Level.DEBUG, e);
    		    			showAlert("Connection error.", "Connection timeout", AlertType.ERROR);

    		    		}else if(cause instanceof InterruptedException) {

    		    			logger.catching(Level.DEBUG, e);

    		    		}else if(cause instanceof SerialPortException) {

    		    			logger.catching(Level.DEBUG, e);

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
   			});
    }

    @FXML public void onDragOwer(DragEvent event) {

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

    @FXML public void onDragDropped(DragEvent event) {

    	Optional.of(event.getDragboard())
    	.map(Dragboard::getFiles)
    	.map(l->l.get(0))
    	.ifPresent(f->uploadWorker.uplodeToTheFlash(f.toPath()));

    	event.setDropCompleted(true);
    	event.consume();
    }

    @FXML void onDriverSelect(ActionEvent event) {
    	RadioMenuItem source = (RadioMenuItem) event.getSource();
    	prefs.put(SELECTED_MENU, source.getId());
    	ComPortWorker.setSerialPortClass((Class<?>)source.getUserData());
    }

	void setDisable(final boolean disable) {
		Platform.runLater(
				()->{
					chbRead.setDisable(disable);
					chbUpload.setDisable(disable);
					chbEdit.setDisable(disable);
				});
	}

	public static void disable(final boolean disable) {
		logger.traceEntry("{}", disable);
		Optional.ofNullable(controller).ifPresent(fc->fc.setDisable(disable));
	}

	private static Alert alert;
	public static void showAlert(String title, final String message, AlertType alertType) {

		Platform.runLater(
					()->{

						if(alert==null) {

							alert = new Alert(alertType);
							alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
							Optional.ofNullable(node).map(Node::getScene).map(Scene::getWindow).ifPresent(alert::initOwner);
							alert.setTitle(title);
							alert.setHeaderText(null);
							alert.setContentText(message);
							alert.showAndWait();
							alert = null;

						}else{

							alert.setTitle(title);
							alert.setAlertType(alertType);
							final String contentText = alert.getContentText() + '\n';
							alert.setContentText(contentText + message);
							sizeToScene(alert);
						}});
	}

	private static void sizeToScene(Alert alert) {
		try {

			final Field field = Dialog.class.getDeclaredField("dialog");
			field.setAccessible(true);
			final Object object = field.get(alert);
			final Method method = object.getClass().getMethod("sizeToScene");
			method.setAccessible(true);
			method.invoke(object);

		} catch (SecurityException | IllegalArgumentException | NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
			logger.catching(e);
		}
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

					if(!e.isShortcutDown())
						return;

					final KeyCode keyCode = e.getCode();
					switch (keyCode) {

					case U: chbUpload.show();	 chbUpload.requestFocus(); break;
					case E: chbEdit	 .show();	 chbEdit  .requestFocus(); break;
					case R:

						if(readWorker.readFromFlash())
							break;

						chbRead	 .show();
						chbRead  .requestFocus();
						break;
					case C:

						final Optional<IndexRange> filter = Optional.of(txtArea)

																.filter(TextArea::isFocused)
																.map(TextArea::getSelection)
																.filter(s->s.getStart()!=s.getEnd());

						// Return if text area is focused and has selection
						if( filter.isPresent())
							return;

						btnConnect.fire();
						btnConnect.requestFocus();
						break;

					default: return;
					}

					e.consume();
				}); 
	}

	private ChangeListener<? super Boolean> focusListener = (o,ov,nv)->{
		if(!nv)
			((ChoiceBox<?>)((ReadOnlyBooleanProperty)o).getBean()).hide();
	};
}
