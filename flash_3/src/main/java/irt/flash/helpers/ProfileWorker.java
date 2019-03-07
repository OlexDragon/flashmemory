package irt.flash.helpers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import irt.flash.data.UnitAddress;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

public class ProfileWorker {

	private static final String CONVERTER = UnitAddress.CONVERTER.name();
	private static final String DEVICE_TYPE = "device-type";
	private static final String PROPERTY_TO_SHOW = "#=";
	private static final Logger logger = LogManager.getLogger();
	private static Node node;
	private static String newSerialNumber;
	private static UploadWorker uploadWorker;
	
	public ProfileWorker(Node node) {
		ProfileWorker.node = node;
	}

	public static void showConfirmationDialog() {
		Platform.runLater(
				()->{

					Alert alert = new Alert(AlertType.CONFIRMATION);
					Optional.ofNullable(node).map(Node::getScene).map(Scene::getWindow).ifPresent(alert::initOwner);
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
					Optional.ofNullable(node).map(Node::getScene).map(Scene::getWindow).ifPresent(dialog::initOwner);
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

					chbDeviceType.getSelectionModel().selectedIndexProperty().addListener(
							(o,oldV,newV)->{
								okButton.setDisable(newV.intValue()<0); });

					dialog.setResultConverter(bt->Optional.of(bt).filter(b->b==ButtonType.OK).map(b->getSelectedItem(chbDeviceType)).orElse(null));

					showProfileSetup(dialog.showAndWait());

				});
	}

	private static void showProfileSetup(Optional<File> oProfile) {

		oProfile.ifPresent(

				profile->{

					final File dir = profile.getParentFile();
					final Properties properties = propertiesFrom(dir);
					final String propertyName = profile.getName();
					final String property = properties.getProperty(propertyName);

					final File saveDirectory = Optional.ofNullable(property)

							.filter(path->!path.isEmpty())
							.map(File::new)
							.filter(File::exists)
							.orElseGet(()->chooseDirectory(propertyName, profile, properties));

					if(saveDirectory==null)
						return;

					ThreadWorker.runThread(
							()->{

								Boolean converter = Optional.ofNullable(properties.getProperty(DEVICE_TYPE)).map(dt->dt.equals(CONVERTER)).orElseGet(

										// Save unit type in the properties file
										()->{

											Callable<UnitAddress> callable = ()->{

												Dialog<UnitAddress> dialog = new Dialog<>();
												Optional.ofNullable(node).map(Node::getScene).map(Scene::getWindow).ifPresent(dialog::initOwner);
												dialog.setTitle("Device Type Selection");
												dialog.setHeaderText("What type of device does this profile belong to?");

												final DialogPane dp = dialog.getDialogPane();
												dp.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

												final ObservableList<UnitAddress> observableArrayList = FXCollections.observableArrayList(UnitAddress.values());
												ChoiceBox<UnitAddress> chbUnitAddress = new ChoiceBox<>(observableArrayList);
												dp.setContent(chbUnitAddress);

												final Node okButton = dp.lookupButton(ButtonType.OK);
												okButton.setDisable(true);

												chbUnitAddress.getSelectionModel().selectedIndexProperty().addListener(

														(o,oldV,newV)->{
															okButton.setDisable(newV.intValue()<0); });

												
												dialog.setResultConverter(btn->Optional.of(btn).filter(b->b==ButtonType.OK).map(b->chbUnitAddress.getSelectionModel().getSelectedItem()).orElse(null));
												return dialog.showAndWait().orElse(null);
											};

											FutureTask<UnitAddress> task = new FutureTask<>(callable);
											Platform.runLater(task);

											try {

												final UnitAddress unitAddress = task.get();
												properties.put(DEVICE_TYPE, unitAddress.name());

												savePropertiesFile(dir, properties);

												return Optional.ofNullable(unitAddress).map(ua->ua==UnitAddress.CONVERTER).orElse(null);

											} catch (InterruptedException | ExecutionException e) {
												logger.catching(e);
											}
											return null;});

								if(converter==null)
									return;

								final List<PropertyLine> fieldsToEdit = getFieldsToEdit(profile);

								Platform.runLater(
										()->{

											Dialog<String> setUpDialog = new Dialog<>();
											Optional.ofNullable(node).map(Node::getScene).map(Scene::getWindow).ifPresent(setUpDialog::initOwner);
											setUpDialog.setTitle("Profile setup");
											setUpDialog.setHeaderText(null);
											final DialogPane dialogPane = setUpDialog.getDialogPane();
											dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

//									final Node okButton = dialogPane.lookupButton(ButtonType.OK);
//									okButton.setDisable(true);

											GridPane grid = new GridPane();
											dialogPane.setContent(grid);

											grid.setHgap(10);
											grid.setVgap(10);
											grid.setPadding(new Insets(10, 10, 10, 10));
											grid.getRowConstraints().add(new RowConstraints());

											grid.add(new Label("Serial Number:"), 0, 0);

											final TextField tfSerialNumber = new TextField();
											grid.add(tfSerialNumber, 1, 0);
											tfSerialNumber.setPromptText("Auto");
											tfSerialNumber.setTooltip(new Tooltip("Leave this field blank to automatically receive the serial number."));
											tfSerialNumber.textProperty().addListener(
													(o,ov,nv)->Optional.ofNullable(newSerialNumber).filter(nsn->nv.isEmpty()).ifPresent(nsn->tfSerialNumber.setText(nsn)));

											ThreadWorker.runThread(()->getSerialNumberAutomatically(tfSerialNumber, converter));

											final List<ChoiceBox<PropertyValue>> collect = IntStream.range(0, fieldsToEdit.size()).mapToObj(addFields(grid, fieldsToEdit)).collect(Collectors.toList());
											setUpDialog.setResultConverter(bt->Optional.of(bt).filter(b->b==ButtonType.OK).map(b->editProfile(profile, collect)).orElse(null));

											setUpDialog.showAndWait().ifPresent(
													profileToWrite->{
														
														String serialNumber = Optional.of(tfSerialNumber)

																.map(tf->tf.getText().trim())
																.filter(t->!t.isEmpty())
																.orElseGet(()->waitForSerialNumber(tfSerialNumber));

														if(serialNumber==null)
															return;

														final byte[] bytes = setSerialNumber(serialNumber, profileToWrite);
														final File file = new File(saveDirectory, serialNumber + ".bin");
														logger.debug("Write to the file {}", file);

															Optional.ofNullable(properties.getProperty(DEVICE_TYPE, null)).map(UnitAddress::valueOf).ifPresent(
																	unitAddress->{

																		ThreadWorker.runThread(()->{

																			try {
																				
																				final Path path = Files.write(file.toPath(), bytes);
																				uploadWorker.uplodeToTheFlash(unitAddress, path);

																			} catch (IOException e) {
																				logger.catching(e);
																			}
																		});
																	});
													});
										});
							});
				});
	}

	private static byte[] setSerialNumber(String serialNumber, String profile) {
		int index = profile.indexOf("device-serial-number ");
		StringBuffer sb = new StringBuffer(profile.substring(0, index));
		logger.error(serialNumber);
		sb.append("device-serial-number ").append(serialNumber).append(System.lineSeparator());
		sb.append(profile.substring(profile.indexOf("\n", index)+1));
		return sb.toString().getBytes();
	}

	private static String waitForSerialNumber(TextField tfSerialNumber) {

		if(newSerialNumber!=null)
			return newSerialNumber;

		Dialog<ButtonType> dialog = new Dialog<>();
		Optional.ofNullable(node).map(Node::getScene).map(Scene::getWindow).ifPresent(dialog::initOwner);
		dialog.setTitle("Wait for Serial Number");
		dialog.setHeaderText("The program scans the file system to get a new serial number.\nWait or enter your serial number.");
		final DialogPane dialogPane = dialog.getDialogPane();
		dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		dialogPane.setContent(tfSerialNumber);

		final Node okButton = dialogPane.lookupButton(ButtonType.OK);
		if(newSerialNumber==null)
			okButton.setDisable(true);
		tfSerialNumber.textProperty().addListener((observable, oldValue, newValue)->okButton.setDisable(newValue.trim().isEmpty()));

		return dialog.showAndWait().filter(btn->btn==ButtonType.OK).map(btn->tfSerialNumber.getText().trim()).filter(text->!text.isEmpty()).orElse(null);
	}

	private static void getSerialNumberAutomatically(TextField tfSerialNumber, boolean converter) {

		newSerialNumber = null;

		Calendar calendar = Calendar.getInstance();
		String newSN = Integer.toString(calendar.get(Calendar.YEAR)).substring(2);
		newSN += String.format("%02d", calendar.get(Calendar.WEEK_OF_YEAR));

		try {
			String fn = newSN;
			int sequence = Files.walk(Paths.get("Z:\\4alex\\boards\\profile"))
					.filter(Files::isRegularFile)
					.map(Path::getFileName)
					.map(Path::toString)
					.map(fileName->fileName.toUpperCase().replace(".BIN", ""))
					.filter(fileName->converter ? fileName.endsWith("C") : true)
					.map(fileName->fileName.replaceAll("\\D", ""))
					.filter(fileName->fileName.startsWith(fn))
					.map(fileName->fileName.replace(fn, ""))
					.mapToInt(Integer::parseInt)
					.max()
					.orElse(0);

			newSN += String.format("%03d", ++sequence);
			if(converter)
				newSN += 'C';

			newSerialNumber = "IRT-" + newSN;

			logger.debug("newSerialNumber = {}", newSerialNumber);

			Platform.runLater(()->Optional.of(tfSerialNumber).filter(tf->tf.getText().trim().isEmpty()).ifPresent(tf->tf.setText(newSerialNumber)));

		} catch (IOException e) {
			logger.catching(e);
		}
	}

	private static String editProfile(File profile, List<ChoiceBox<PropertyValue>> choiceBoxs) {

		final Map<String, PropertyValue> proprties = selectedToMap(choiceBoxs);

		try {
			return Files.lines(profile.toPath())

					.map(
							line->
							Optional.of(line)
							.filter(l->l.contains(PROPERTY_TO_SHOW))
							.map(l->l.trim())
							.map(l->l.split("\\s+", 2)[0].trim().replace("#", ""))
							.map(l->proprties.get(l))
							.map(p->p.owner.key + " " + p.value + "\t" + PROPERTY_TO_SHOW + p.owner.commentLine)
							.orElse(line))
					.collect(Collectors.joining(System.lineSeparator()));

		} catch (Exception e) {
			logger.catching(e);
		}
		return null;
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

	private static List<PropertyLine> getFieldsToEdit(File profile) {

		try (Stream<String> fileLines = Files.lines(profile.toPath())) {

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

		} catch (IOException e) {
			logger.catching(e);
		}
		return null;
	}

	private static File chooseDirectory(String propertyName, File dir, Properties properties) {
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Select a profile folder to save " + propertyName);
		final Window window = Optional.ofNullable(node).map(Node::getScene).map(Scene::getWindow).orElse(null);
		final File selectedDirectory = chooser.showDialog(window);

		Optional.ofNullable(selectedDirectory).map(File::getAbsolutePath).ifPresent(
				path->{

					properties.put(propertyName, selectedDirectory.getAbsolutePath());

					savePropertiesFile(dir, properties);
					
				});

		return selectedDirectory;
	}

	private static synchronized void savePropertiesFile(File dir, Properties properties) {
		logger.entry(dir, properties);

		File saveDir = Optional.of(dir).filter(File::isDirectory).orElseGet(()->dir.getParentFile());
		final String name = saveDir.getName();
		File saveFile = new File(saveDir, name + ".properties");
		copyFile(saveFile, name + ".old");

		try(OutputStream os = new FileOutputStream(saveFile)){

			properties.store(os, name + " - profile destination folders");

		} catch (IOException e) {
			logger.catching(e);
		}
	}

	private static void copyFile(File file, String name) {
		logger.entry(file, name);

		Optional.ofNullable(file).filter(File::exists).ifPresent(
				f->{
					final Path path = f.toPath();

					try {

						Files.copy(path, path.resolveSibling(name), StandardCopyOption.REPLACE_EXISTING);

					} catch (IOException e) {
						logger.catching(e);
					}
				});
	}

	private static synchronized Properties propertiesFrom(File dir) {
		Properties properties = new Properties();
		Arrays.stream(dir.listFiles(f->f.getName().toLowerCase().endsWith(".properties")))
		.forEach(
				f -> {
					try {
						properties.load(new FileReader(f));
					} catch (IOException e) {
						logger.catching(e);
					}
				});
		return properties;
	}

	private static File getSelectedItem(ChoiceBox<File> choiceBox) {
		return choiceBox.getSelectionModel().getSelectedItem();
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
					.collect(Collectors.toList());
		}

		@Override
		public String toString() {
			return "PropertyLine [key=" + key + ", value=" + value + ", title=" + title + ", comment=" + commentLine
					+ ", propertyValues=" + propertyValues + ", dependsOn=" + dependsOn + ", dependent=" + dependent
					+ "]";
		}
	}

	public static class PropertyValue {

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

	}

	public void setUploadWorker(UploadWorker uploadWorker) {
		this.uploadWorker = uploadWorker;
	}
}
