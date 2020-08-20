package irt.flash.helpers;

import static irt.flash.exception.ExceptionWrapper.catchConsumerException;
import static irt.flash.exception.ExceptionWrapper.catchFunctionException;
import static irt.flash.exception.ExceptionWrapper.catchRunnableException;
import static irt.flash.exception.ExceptionWrapper.catchSupplierException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.IntFunction;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import irt.flash.Flash3App;
import irt.flash.FlashController;
import irt.flash.data.UnitAddress;
import irt.flash.exception.WrapperException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

public class ProfileWorker {

	private static final Logger logger = LogManager.getLogger();

	private static final String WITHOUT_CHANGES = "Without changes";
	private static final String COPY = "Copy";
	private static final String RENAME = "Rename";

	private static final String TYPE = ".type";

	private static final String EDIT_PROFILE = "Edit Profile";
	public static final String EDIT = "Edit ...";
	private static final String PROPERTY_TO_SHOW = "#=";

	private static String newSerialNumber;
	private static UploadWorker uploadWorker;

	private final static Preferences prefs = Preferences.userNodeForPackage(Flash3App.class);
	private String profile;

	private static ChoiceBox<ThreadWorker> chbEdit;
	private static TextField tfSerialNumber;
	
	public ProfileWorker(ChoiceBox<ThreadWorker> chbEdit) {

		ProfileWorker.chbEdit = chbEdit;

		chbEdit.getItems().add(new ThreadWorker(EDIT, null));
		chbEdit.getSelectionModel().select(0);

	}

	public static void showConfirmationDialog() {
		Platform.runLater(
				()->{

					Alert alert = new Alert(AlertType.CONFIRMATION);
					initOwner(alert);
					alert.setTitle("Profile not found.");
					alert.setHeaderText("Profile memory is empty");
					alert.setContentText("If you want to create a new profile, click YES.");
					alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
					alert.showAndWait()
					.filter(b->b==ButtonType.YES)
					.ifPresent(b->showMessageOfChoice());
				});
	}

	private static void showMessageOfChoice() {
		Platform.runLater(
				()->{
					Dialog<File> dialog = new Dialog<>();
					initOwner(dialog);
					dialog.setTitle("Device Type Selector");
					dialog.setHeaderText("Select device type and press OK button.");
					final DialogPane dialogPane = dialog.getDialogPane();
					dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

					final Node okButton = dialogPane.lookupButton(ButtonType.OK);
					okButton.setDisable(true);

					GridPane grid = new GridPane();
					dialogPane.setContent(grid);

					grid.setHgap(10);
					grid.setVgap(10);
					grid.setPadding(new Insets(10, 10, 10, 10));

					Label label = new Label("Device Group:");
					label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
					grid.add(label, 0, 0);

					label = new Label("Device Type:");
					label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
					grid.add(label, 0, 1);

					final StringConverter<File> converter = new StringConverter<File>() {
						
						@Override
						public String toString(File file) {
							return file.getName();
						}
						@Override public File fromString(String string) { return null; }
					};

					ChoiceBox<File> chbDeviceGroup = new ChoiceBox<>();
					grid.add(chbDeviceGroup, 1, 0);
					chbDeviceGroup.setConverter(converter);
					chbDeviceGroup.setMaxWidth(Double.MAX_VALUE);

					ChoiceBox<File> chbDeviceType = new ChoiceBox<>();
					grid.add(chbDeviceType, 1, 1);
					chbDeviceType.setConverter(converter);
					chbDeviceType.setMaxWidth(Double.MAX_VALUE);

					DeviceWorker.fillDeviceGroup(chbDeviceGroup, chbDeviceType);

					final SingleSelectionModel<File> selectionModel = chbDeviceType.getSelectionModel();
					selectionModel.selectedIndexProperty().addListener((o,ov,nv)->okButton.setDisable(nv.intValue()<0));
					selectionModel.selectedItemProperty().addListener((o,ov,nv)->Optional.ofNullable(prefs).ifPresent(p->p.put("selected_device_type", nv.toString())));

					Optional.ofNullable(prefs).flatMap(p->Optional.ofNullable(p.get("selected_device_type", null))).map(File::new).ifPresent(f->chbDeviceType.getSelectionModel().select(f));

					dialog.setResultConverter(bt->Optional.of(bt).filter(b->b==ButtonType.OK).map(b->selectionModel.getSelectedItem()).orElse(null));

					showProfileSetup(dialog.showAndWait());

				});
	}

	private static void showProfileSetup(Optional<File> oProfile) {

		oProfile.ifPresent(

				profile->{

					// Get the directory where create new unit profile
					final File dir = profile.getParentFile();
					final Properties properties = propertiesFrom(dir);
					final String propertyName = profile.getName();

					final File saveDirectory = Optional.ofNullable(properties.getProperty(propertyName))

							.filter(saveDir->!saveDir.isEmpty())
							.map(File::new)
							.filter(File::exists)
							.orElseGet(()->chooseDirectory(propertyName, profile, properties));

					if(saveDirectory==null)
						return;

					ThreadWorker.runThread(()->{
						try {

							showProfileSetupDialog(profile.toPath())
							.ifPresent(
									catchConsumerException(
											profileToWrite->{
												String serialNumber = Optional.of(tfSerialNumber)

														.map(tf->tf.getText().trim())
														.filter(t->!t.isEmpty())
														.orElseGet(catchSupplierException(()->waitForSerialNumber(tfSerialNumber)));

												if(serialNumber==null)
													return;

												final byte[] bytes = setSerialNumber(serialNumber, profileToWrite);
												final File file = new File(saveDirectory, serialNumber + ".bin");
												logger.debug("Write to the file {}", file);

												saveAndUpload(file, profileToWrite, bytes);
											}));

						} catch (Exception e) {
							logger.catching(e);
						}
					});
				});
	}

	private static void saveAndUpload(final File file, String profileToWrite, final byte[] bytes) throws IOException {
		getUnitAddress(profileToWrite).ifPresent(
				unitAddress->{

					ThreadWorker.runThread(
							catchRunnableException(
									()->{

										final Path path = Files.write(file.toPath(), bytes);
										uploadWorker.uplodeToTheFlash(unitAddress, path);
									}));
				});
	}

	private static Optional<UnitAddress> getUnitAddress(String profile) throws IOException {
		logger.error(profile);

		try(	final StringReader stringReader = new StringReader(profile);
				final BufferedReader bufferedReader = new BufferedReader(stringReader);
				final Stream<String> stream = bufferedReader.lines();){

			return getUnitAddress(stream);
		}
	}

	private static Optional<String> showProfileSetupDialog(Path profilePath) throws InterruptedException, ExecutionException, IOException {

		Boolean isFrequencyConverter = isFrequencyConverter(profilePath);

		if(isFrequencyConverter==null)
			return Optional.empty();

		tfSerialNumber = new TextField();

		//	if text field is empty put auto generated serial number
		tfSerialNumber.textProperty().addListener(
				(o,ov,nv)->Optional.ofNullable(newSerialNumber).filter(nsn->nv.isEmpty()).ifPresent(nsn->tfSerialNumber.setText(nsn)));

		//set Serial Number from profile
		ThreadWorker.runThread(
				catchRunnableException(
						()->getSerialNumber(profilePath).ifPresent(
								sn->
								Platform.runLater(
										()->{
											tfSerialNumber.setText(sn);
											tfSerialNumber.addEventFilter(KeyEvent.KEY_PRESSED,
													e->{
														// on ESCAPE put back serial number from profile
														if (e.getCode() == KeyCode.ESCAPE) {
															e.consume();
															tfSerialNumber.setText(sn);
														}

													}); }))));

		//set Serial Number automatically
		ThreadWorker.runThread(
				catchRunnableException(
						()->{
							newSerialNumber = getSerialNumberAutomatically(isFrequencyConverter);
							Platform.runLater(
									()->
									Optional.ofNullable(tfSerialNumber)
									.filter(tf-> tf.getText().isEmpty())
									.ifPresent(tf->tf.setText(newSerialNumber)));
						}));


		return ThreadWorker.runFxFutureTask(()->{

			final GridPane grid = gridWithSerialNumber(tfSerialNumber);

			final List<PropertyLine> fieldsToEdit = getFieldsToEdit(profilePath);
			final List<ChoiceBox<PropertyValue>> collect = IntStream.range(0, fieldsToEdit.size()).mapToObj(addFields(grid, fieldsToEdit)).collect(Collectors.toList());

			Dialog<Object> setupDialog = creatDialog("Profile setup", null, grid, ButtonType.OK, ButtonType.CANCEL);
			setupDialog.setResultConverter(bt->Optional.of(bt).filter(b->b==ButtonType.OK).map(catchFunctionException(b->editProfile(profilePath, collect))).orElse(null));

			return setupDialog.showAndWait().map(String.class::cast);
		}).get();
	}

	private static Boolean isFrequencyConverter(final Path profilePath) throws IOException {
	
		try(final Stream<String> lines = Files.lines(profilePath);) {
	
			return getUnitAddress(lines)
					.map(ua->ua==UnitAddress.CONVERTER).orElse(null);
		}
	}

	private static Optional<String> getDeviceType(final Stream<String> stream) {
		final Properties deviceProperties = DeviceWorker.getDeviceProperties(stream);
		return DeviceWorker.getDeviceType(deviceProperties);
	}

	/**
	 * @param profilePath - Path to the unit profile
	 * @return	unit serial number ("device-serial-number") or null. if serial number contains 'X' returns  null
	 * @throws IOException
	 */
	private static Optional<String> getSerialNumber(Path profilePath) throws IOException {

		try(	final Stream<String> lines = Files.lines(profilePath);){

			final Properties deviceProperties = DeviceWorker.getDeviceProperties(lines);
			final String property = deviceProperties.getProperty(DeviceWorker.DEVICE_SERIAL_NUMBER);
			return Optional.ofNullable(property).filter(sn->!sn.contains("X"));
		}
	}

	private Optional<String> getSerialNumber(String profile) throws IOException {

		try(	final StringReader stringReader = new StringReader(profile);
				final BufferedReader bufferedReader = new BufferedReader(stringReader);
				final Stream<String> lines = bufferedReader.lines();){

			final Properties deviceProperties = DeviceWorker.getDeviceProperties(lines);
			final String property = deviceProperties.getProperty(DeviceWorker.DEVICE_SERIAL_NUMBER);

			return Optional.ofNullable(property).filter(sn->!sn.contains("X"));
		}
	}

	public static Optional<UnitAddress> getUnitAddress(final Stream<String> stream) {

		final String key = getDeviceType(stream).map(dt->dt +  TYPE).orElse(null);

		if(key==null)
			return Optional.empty();

		return DeviceWorker.getFlash3Property(key)

				.map(UnitAddress::valueOf).map(Optional::of).orElseGet(
						catchSupplierException(

								()->{

									FutureTask<UnitAddress> task = ThreadWorker.runFxFutureTask(()->deviceTypeSelection());

									// Save unit type in the properties file
									final UnitAddress unitAddress = task.get();

									if(unitAddress==null)
										return Optional.empty();

									DeviceWorker.saveProperty(key, unitAddress.name());

									return Optional.ofNullable(unitAddress);
								}));
	}

	private static UnitAddress deviceTypeSelection() {

		Dialog<UnitAddress> dialog = new Dialog<>();
		initOwner(dialog);
		dialog.setTitle("Device Type Selection");
		dialog.setHeaderText("What type of device does this profile belong to?");

		final DialogPane dp = dialog.getDialogPane();
		dp.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		final ObservableList<UnitAddress> observableArrayList = FXCollections.observableArrayList(UnitAddress.values());
		ChoiceBox<UnitAddress> chbUnitAddress = new ChoiceBox<>(observableArrayList);
		dp.setContent(chbUnitAddress);

		final Node okButton = dp.lookupButton(ButtonType.OK);
		okButton.setDisable(true);

		final SingleSelectionModel<UnitAddress> selectionModel = chbUnitAddress.getSelectionModel();
		selectionModel.selectedIndexProperty().addListener(

				(o,oldV,newV)->{
					okButton.setDisable(newV.intValue()<0); });

		dialog.setResultConverter(btn->Optional.of(btn).filter(b->b==ButtonType.OK).map(b->selectionModel.getSelectedItem()).orElse(null));

		return dialog.showAndWait().orElse(null);
	}

	private static GridPane gridWithSerialNumber(final TextField tfSerialNumber) {
		GridPane grid = new GridPane();

		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(10, 10, 10, 10));
		grid.getRowConstraints().add(new RowConstraints());

		grid.add(new Label("Serial Number:"), 0, 0);
		grid.add(tfSerialNumber, 1, 0);

		tfSerialNumber.setPromptText("Auto");
		tfSerialNumber.setTooltip(new Tooltip("Leave this field blank to automatically receive the serial number."));
		return grid;
	}

	private static Dialog<Object> creatDialog(String title, String headerText, Node content, ButtonType... buttonTypes) {
		Dialog<Object> dialog = creatDialog(title, headerText, (String)null, buttonTypes);
		dialog.getDialogPane().setContent(content);
		return dialog;
	}

	private static Dialog<Object> creatDialog(String title, String headerText, String contentText, ButtonType... buttonTypes) {
		Dialog<Object> dialog = new Dialog<>();
		initOwner(dialog);
		dialog.setTitle(title);
		dialog.setHeaderText(headerText);
		dialog.setContentText(contentText);
		dialog.getDialogPane().getButtonTypes().addAll(buttonTypes);
		return dialog;
	}

	private static byte[] setSerialNumber(String serialNumber, String profile) {
		int index = profile.indexOf("device-serial-number ");
		StringBuffer sb = new StringBuffer(profile.substring(0, index));
		sb.append("device-serial-number ").append(serialNumber).append(System.lineSeparator());
		sb.append(profile.substring(profile.indexOf("\n", index)+1));
		return sb.toString().getBytes();
	}

	private static String waitForSerialNumber(TextField tfSerialNumber) throws InterruptedException, ExecutionException {

		return ThreadWorker.runFxFutureTask(

				()->{

					if(newSerialNumber!=null)
						return newSerialNumber;

					Dialog<Object> dialog = creatDialog(

							"Wait for Serial Number",
							"The program scans the file system to get a new serial number.\nWait or enter your serial number.",
							tfSerialNumber,
							ButtonType.OK, ButtonType.CANCEL);

					final Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
					if(newSerialNumber==null)
						okButton.setDisable(true);
					tfSerialNumber.textProperty().addListener((observable, oldValue, newValue)->okButton.setDisable(newValue.trim().isEmpty()));

					return dialog.showAndWait().filter(btn->btn==ButtonType.OK).map(btn->tfSerialNumber.getText().trim()).filter(text->!text.isEmpty()).orElse(null);
				}).get();
	}

	private static String getSerialNumberAutomatically(boolean converter) throws IOException {

		Calendar calendar = Calendar.getInstance();
		String newSN = Integer.toString(calendar.get(Calendar.YEAR)).substring(2);
		newSN += String.format("%02d", calendar.get(Calendar.WEEK_OF_YEAR));

		try(	final Stream<Path> walk = Files.walk(Paths.get("Z:\\4alex\\boards\\profile"));) {
			String yearAndWeek = newSN;
			
			int sequence = walk

						.map(Path::getFileName)
						.map(Path::toString)
						.map(fileName->fileName.toUpperCase().replace(".BIN", ""))
						.filter(fileName->converter ? fileName.endsWith("C") : true)
						.map(fileName->fileName.replaceAll("\\D", ""))
						.filter(fileName->fileName.startsWith(yearAndWeek))
						.map(fileName->fileName.replace(yearAndWeek, ""))
						.mapToInt(Integer::parseInt)
						.max()
						.orElse(0);

			newSN += String.format("%03d", ++sequence);
			if(converter)
				newSN += 'C';

			return "IRT-" + newSN;

		}
	}

	private static String editProfile(Path path, List<ChoiceBox<PropertyValue>> choiceBoxs) throws IOException {

		final Map<String, PropertyValue> properties = selectedToMap(choiceBoxs);

		try(	final Stream<String> lines = Files.lines(path);) {
			
			return lines

					.map(
							line->
							Optional.of(line)
							.filter(l->l.contains(PROPERTY_TO_SHOW))
							.map(l->l.trim())
							.map(l->l.split("\\s+", 2)[0].trim().replace("#", ""))
							.map(l->properties.get(l))
							.map(p->p.owner.key + " " + p.value + "\t " + PROPERTY_TO_SHOW + p.owner.commentLine)
							.orElse(line))
					.collect(Collectors.joining(System.lineSeparator()));

		}
	}

	private static Map<String, PropertyValue> selectedToMap(List<ChoiceBox<PropertyValue>> choiceBoxs) {
		final List<PropertyValue> dependents = new ArrayList<>();

		final Map<String, PropertyValue> proprties = choiceBoxs.stream()

				.map(ChoiceBox::getSelectionModel)
				.map(SingleSelectionModel::getSelectedItem)
				.peek(p->Optional.of(p).filter(v->!v.owner.dependent.isEmpty()).ifPresent(dependents::add))
				.collect(Collectors.toMap(p->p.owner.key, p->p));

		// Properties dependent on ChoiceBoxes selection
		final Map<String, PropertyValue> dep =  dependents.stream()

				.flatMap(
						pv->
						pv.owner.dependent.stream()
						.map(d->d.propertyValues)
						.flatMap(List::stream)
						.filter(v->v.key.equals(pv.key)))
				.collect(Collectors.toMap(p->p.owner.key, p->p));


		proprties.putAll(dep);

//		proprties.entrySet().stream().forEach(p->logger.error("{} : {}", p.getKey(), p.getValue().owner));

		return proprties;
	}

	private static IntFunction<ChoiceBox<PropertyValue>> addFields(GridPane grid, List<PropertyLine> fieldsToEdit) {
		return index->{

			final PropertyLine propertyLine = fieldsToEdit.get(index);
			final int rowIndex = ++index;
			grid.add(new Label(propertyLine.title), 0, rowIndex);

			return Optional.ofNullable(propertyLine.propertyValues).map(

					list->{

						ObservableList<PropertyValue> oList = FXCollections.observableArrayList(list);
						final ChoiceBox<PropertyValue> choiceBox = new ChoiceBox<>(oList);
						final SingleSelectionModel<PropertyValue> selectionModel = choiceBox.getSelectionModel();
						selectionModel.select(0);
						grid.add(choiceBox, 1, rowIndex);

						Optional.ofNullable(propertyLine.value).ifPresent(

								value->
								list.stream().filter(pv->pv.value.equals(value)).findAny().ifPresent(selectionModel::select));
						return choiceBox;
					})
					.orElse(null);
		};
	}

	private static List<PropertyLine> getFieldsToEdit(Path path) throws IOException {

		try (Stream<String> fileLines = Files.lines(path)) {

			return getFieldsToEdit(fileLines);

		}
	}

	private static List<PropertyLine> getFieldsToEdit(Stream<String> fileLines) {

		final Map<Boolean, List<PropertyLine>> collect = fileLines

				.filter(l->l.contains(PROPERTY_TO_SHOW))
				.map(PropertyLine::new)
				.collect(Collectors.partitioningBy(pl->pl.dependsOn==null));

		final List<PropertyLine> propertiesList = collect.get(true);

		//	Set dependent
		collect
		.get(false)
		.stream()
		.forEach(
				d->
				propertiesList
				.stream()
				.filter(
						pl->
						pl.key.equals(d.dependsOn))
				.findAny()
				.ifPresent(
						pl->
						pl.dependent.add(d)));

		return propertiesList;
	}

	private static File chooseDirectory(String propertyName, File propertiesDir, Properties properties) {

		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Select a profile folder to save " + propertyName);

		final File selectedDirectory = chooser.showDialog(getWindow());

		Optional.ofNullable(selectedDirectory).map(File::getAbsolutePath).ifPresent(
				catchConsumerException(
				path->{

					properties.put(propertyName, selectedDirectory.getAbsolutePath());

					savePropertiesFile(propertiesDir, properties);
					
				}));

		return selectedDirectory;
	}

	private static synchronized void savePropertiesFile(File dir, Properties properties) throws FileNotFoundException, IOException {
		logger.traceEntry("{}; {}", dir, properties);

		File saveDir = Optional.of(dir).filter(File::isDirectory).orElseGet(()->dir.getParentFile());
		final String name = saveDir.getName();
		File saveFile = new File(saveDir, name + ".properties");
		copyFile(saveFile, name + ".old");

		try(OutputStream os = new FileOutputStream(saveFile)){

			properties.store(os, name + " - profile destination folders");

		}
	}

	private static void copyFile(File file, String name) {
		logger.traceEntry("{}; {}", file, name);

		Optional.ofNullable(file).filter(File::exists).ifPresent(
				catchConsumerException(
						f->{
							final Path path = f.toPath();
							Files.copy(path, path.resolveSibling(name), StandardCopyOption.REPLACE_EXISTING);
						}));
	}

	private static synchronized Properties propertiesFrom(File dir) {

		Properties properties = new Properties();
		final File[] listFiles = dir.listFiles(f->f.getName().toLowerCase().endsWith(".properties"));

		Arrays.stream(listFiles)
		.forEach(
				catchConsumerException(
						f->
						properties.load(new FileReader(f))));

		return properties;
	}

	private static class PropertyLine{
		private String key;
		private String value;
		private String title;
		private String commentLine;
		private List<PropertyValue>  propertyValues;
		private String dependsOn;

		private List<PropertyLine> dependent = new ArrayList<>();

		public PropertyLine(String line) {

			final String[] splitComment = line.split(PROPERTY_TO_SHOW, 2);

			final String propertyLine = splitComment[0].trim();
			final String[] arrayKeyValue = propertyLine.replace("#", "").trim().split("\\s+", 3);

			key = arrayKeyValue[0].trim();
			value = arrayKeyValue[1].trim();

			commentLine = splitComment[1].trim();
			final Optional<Integer> oValueStartAt = Optional.of(commentLine.indexOf("{")).filter(indexOf->indexOf>=0);

			if(commentLine.startsWith("\"")) {
				
				title = Optional.ofNullable(commentLine.indexOf("\"", 1)).filter(indexOf->indexOf>=0).map(indexOf->commentLine.substring(1, indexOf).trim()).orElse(null);
			} else
				dependsOn = oValueStartAt.map(indexOf->commentLine.substring(0, indexOf).trim()).orElse(null);

			propertyValues = oValueStartAt.map(start->++start)

					.flatMap(
							start ->
							Optional.of(commentLine.indexOf("}", start))
							.filter(indexOf -> indexOf > 0)
							.map(stop -> commentLine.substring(start, stop)))
					.map(vs ->Arrays.stream(vs.split(",")))
					.orElse(Stream.empty())
					.map(v -> v.split("="))
					.filter(v -> v.length > 1)
					.map(v->new PropertyValue(this, v[0].trim(), v[1].trim()))
					.sorted()
					.collect(Collectors.toList());
		}

		@Override
		public String toString() {
			return "PropertyLine [key=" + key + ", value=" + value + ", title=" + title + ", comment=" + commentLine
					+ ", propertyValues=" + propertyValues + ", dependsOn=" + dependsOn + ", dependent=" + dependent
					+ "]";
		}
	}

	public static class PropertyValue implements Comparable<PropertyValue>{

		private PropertyLine owner;
		private String key;
		private String value;

		public PropertyValue(PropertyLine owner, String key, String value) {
			this.owner = owner;
			this.key = key;
			this.value = value;
		}

		@Override
		public String toString() {
			return key;
		}

		@Override
		public int compareTo(PropertyValue propertyValue) {
			return Optional.ofNullable(propertyValue).map(pv->key.compareTo(pv.key)).orElse(1);
		}

	}

	public void setUploadWorker(UploadWorker uploadWorker) {
		ProfileWorker.uploadWorker = uploadWorker;
	}

	public void setProfile(final String profile) {
		this.profile = profile;
		ThreadWorker.runThread(
				()->{

					try(	final StringReader stringReader = new StringReader(profile);
							final BufferedReader bufferedReader = new BufferedReader(stringReader);
							final Stream<String> lines = bufferedReader.lines();){

						
						final boolean editable = lines.filter(l->l.contains(PROPERTY_TO_SHOW)).findAny().isPresent();

						Platform.runLater(
								()->{
	
									final ObservableList<ThreadWorker> items = chbEdit.getItems();

									if(editable)
										items.add(
												new ThreadWorker(EDIT_PROFILE, editProfile()));
									else
										items.stream().filter(tw->tw.toString().equals(EDIT_PROFILE)).findAny().ifPresent(

												tw->
												Platform.runLater(()->items.remove(tw)));
								});

					} catch (IOException e) {
						logger.catching(e);
					}});
	}

	private Runnable editProfile() {
		return ()->{

			final Path profilePath = UploadWorker.profile.getPath();

			if(profilePath==null) {
				FlashController.showAlert("Edit profile.", "The program did not find the link to the file. Try again later or save profile.", AlertType.WARNING);
				return;
			}
		
			try {
				showProfileSetupDialog(profilePath)
				.ifPresent(
						s->{
							try {

								compareSerialNumbers(s)
								.filter(bt->bt!=ButtonType.CANCEL)
								.ifPresent(catchConsumerException(
										bt->{

									byte[] bytes;
									File file = null;
									final String text = bt.getText();
									final Optional<File> oFile = Optional.of(profilePath).map(Path::toFile);

									switch(text) {

									case RENAME:

										oFile.ifPresent(
												profileFile->{
													copyFile(profileFile, profileFile.getName().replace(".bin", ".bak"));
													profileFile.delete();
												});

									case COPY:

										final String newSN = tfSerialNumber.getText().trim();
										bytes = setSerialNumber(newSN, s);
										file = new File(oFile.map(File::getParent).get(), newSN + ".bin");
										logger.debug("new SN: {};", text);
										break;

									case WITHOUT_CHANGES:

										oFile.ifPresent(f->copyFile(f, f.getName().replace(".bin", ".bak")));
										bytes = s.getBytes();
										file = oFile.get();
										break;
									
									default:
										return;
									}

									saveAndUpload(file, s, bytes);
								}));	//TODO edit profile

							} catch (Exception e) {
								throw new RuntimeException(e);
							}
						});
			} catch (WrapperException | NoSuchFileException e) {

				final NoSuchFileException noSuchFileException = getNoSuchFileException(e);

				if(noSuchFileException == null) {
					logger.catching(e);
					return;
				}

				logger.catching(Level.DEBUG, e);
				final String file = noSuchFileException.getFile();

				FlashController.showAlert("Edit profile.", "File not found: " + file, AlertType.ERROR);

			} catch (Exception e) {
				logger.catching(e);
			}
		};
	}

	public static NoSuchFileException getNoSuchFileException(Throwable e) {

		if(e instanceof NoSuchFileException)
			return (NoSuchFileException) e;

		final Throwable cause = e.getCause();
		if(cause == null)
			return null;

		return Optional.ofNullable(cause).filter(c->c instanceof NoSuchFileException).map(NoSuchFileException.class::cast).orElseGet(()->getNoSuchFileException(cause));
	}

	private Optional<ButtonType> compareSerialNumbers(String s) throws IOException {

		final String setSerialNumber = tfSerialNumber.getText().trim();

		final ButtonType renameButton	 = new ButtonType(RENAME);
		final ButtonType copyButton		 = new ButtonType(COPY);
		final ButtonType noChangeButton	 = new ButtonType(WITHOUT_CHANGES);

		return getSerialNumber(s)

				.filter(sn->sn.equals(setSerialNumber))
				.map(sn->Optional.of(noChangeButton))
				.orElseGet(
						catchSupplierException(
								()->{

									final FutureTask<Optional<ButtonType>> task = ThreadWorker.runFxFutureTask(
											()->{
												final Dialog<Object> dialog = creatDialog("Serial number has been changed.", null,

														"Serial number has been changed.\n"
																+ "The program cannot make a decision.\n"
																+ "Select the desired button.",

																renameButton,
																copyButton,
																noChangeButton,
																ButtonType.CANCEL);

												return dialog.showAndWait().map(ButtonType.class::cast);
											});
									return task.get();
								}));
	}

	public void saveProfile(Path path) {

		if(profile==null) {
			FlashController.showAlert("Save profile.", "Unable to save profile", AlertType.ERROR);
			return;
		}

		String serialNumber = profile.split("device-serial-number", 2)[1].trim().split("[\\s#]++", 2)[0];
		String fileName = serialNumber + ".bin";

		final String pathToSave = Optional.ofNullable(path).map(Path::toString).orElseGet(
				catchSupplierException(

				()->{

					Preferences prefs = Preferences.userNodeForPackage(getClass());
					final String profilesDir = prefs.get("last-profile-folder", "Z:\\4alex\\boards\\profile");

					DirectoryChooser chooser = new DirectoryChooser();
					chooser.setTitle("Select a profile folder to save " + fileName);
					chooser.setInitialDirectory(new File(profilesDir));

					final FutureTask<String> task = ThreadWorker.runFxFutureTask(

							()->Optional.ofNullable(
									chooser.showDialog(getWindow()))
							.map(File::toString)
							.orElse(null));

						final String selectedFolder = task.get();
						prefs.put("last-profile-folder", selectedFolder);
						return selectedFolder;
				}));

		Optional.ofNullable(pathToSave).ifPresent(
				catchConsumerException(
						p->
						Files.write(Paths.get(p, fileName), profile.getBytes())));
	}

	private static void initOwner(Dialog<?> dialog) {
		Optional.ofNullable(chbEdit).map(Node::getScene).map(Scene::getWindow).ifPresent(dialog::initOwner);
	}

	private static Window getWindow() {
		return Optional.ofNullable(chbEdit)
				.map(Node::getScene)
				.map(Scene::getWindow)
				.orElse(null);
	}
}
