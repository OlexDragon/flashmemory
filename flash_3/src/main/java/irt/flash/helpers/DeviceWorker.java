package irt.flash.helpers;

import static irt.flash.exception.ExceptionWrapper.catchConsumerException;
import static irt.flash.exception.ExceptionWrapper.catchSupplierException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeSet;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import irt.flash.Flash3App;
import irt.flash.exception.WrapperException;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * Collect information about Device (Unit)
 * @author Alex (Oleksandr Potomkin)
 */
public class DeviceWorker {
	private static final Logger logger = LogManager.getLogger();

	private static final String KEY_SELECTED_DEVICE_GROUP = "selected_device_group";

	private static final String TEMPLATE_PATH = "Z:\\4Olex\\flash\\templates";

	public static final String DEVICE_SERIAL_NUMBER	 = "device-serial-number";
	public static final String DEVICE_SUBTYPE		 = "device-subtype";
	public static final String DEVICE_REVISION		 = "device-revision";
	public static final String DEVICE_TYPE			 = "device-type";
	public static final String DEVICE_MAC_ADDRESS	 = "mac-address";

	private static final String DEFAULT_TEMPLATE_S_PATH = "Default Template's Path";

	private final static Preferences prefs = Preferences.userNodeForPackage(Flash3App.class);

	private static TextArea txtArea;
	private static UploadWorker uploadWorker;

	private ProfileWorker profileWorker;

	private static File defaultDirectory; 	public static File getDefaultDirectory() { return defaultDirectory; }
	private static String deviceType; 		public static String getDeviceType() { return deviceType; }

	private static File 		flash3PropertiesFile;
	private static Properties 	flash3Properties;

	public DeviceWorker(TextArea txtArea) throws IOException {

		DeviceWorker.txtArea = txtArea;

		loadFlashProperties();
	}

	public static void fillDeviceGroup(ChoiceBox<File> chbDeviceGroup, ChoiceBox<File> chbDeviceType) {

		Optional.of(defaultDirectory.listFiles(File::isDirectory))
		.filter(l->l.length>0)
		.map(FXCollections::observableArrayList)
		.ifPresent(oal->Platform.runLater(()->chbDeviceGroup.setItems(oal)));

		chbDeviceGroup.getSelectionModel().selectedItemProperty().addListener(
				e->{

					chbDeviceType.setItems(FXCollections.emptyObservableList());

					final File value = (File) ((ReadOnlyObjectProperty<?>)e).getValue();

					Optional.ofNullable(value)
					.map(dir->dir.listFiles(((d, n) -> n.toLowerCase().endsWith(".bin"))))
					.filter(l->l.length>0)
					.map(FXCollections::observableArrayList)
					.ifPresent(
							oal->{
								Optional.ofNullable(prefs).ifPresent(p->p.put(KEY_SELECTED_DEVICE_GROUP, value.toString()));
								Platform.runLater(()->chbDeviceType.setItems(oal.sorted()));
							});
				});

		Optional.ofNullable(prefs)
		.flatMap(p->Optional.ofNullable(p.get(KEY_SELECTED_DEVICE_GROUP, null))).map(File::new)
		.ifPresent(f->chbDeviceGroup.getSelectionModel().select(f));
	}

	public void setReadData(ByteBuffer byteBuffer) throws IOException {

		deviceType = null;

		byte[] bytes = Optional.ofNullable(byteBuffer).map(ByteBuffer::array).orElse(new byte[0]);

		int length = getProfileLength(bytes);

		if(length==0) {
			ProfileWorker.showConfirmationDialog();
			return;
		}
		final String profile = new String(bytes, 0, length);
		profileWorker.setProfile(profile);

		Platform.runLater(()->txtArea.setText(profile));
		logger.info("\n{}", profile);

		try(	final StringReader stringReader = new StringReader(profile);
				final BufferedReader bufferedReader = new BufferedReader(stringReader);
				final Stream<String> stream = bufferedReader.lines();){

			Properties deviceProperties = getDeviceProperties(stream);

			selectProgramByDeviceType(deviceProperties);
			searchFileBySerialNumber(deviceProperties.getProperty(DEVICE_SERIAL_NUMBER));
		}
	}

	/**
	 * @param stream
	 * @return Properties contains keys DEVICE_TYPE, DEVICE_REVISION, DEVICE_SUBTYPE and DEVICE_SERIAL_NUMBER.
	 */
	public static Properties getDeviceProperties(final Stream<String> stream) {
		logger.traceEntry();

		Properties deviceProperties = new Properties();

		stream
		.map(String::trim)
		.filter(
				line->
				line.startsWith(DEVICE_TYPE) ||
				line.startsWith(DEVICE_REVISION) ||
				line.startsWith(DEVICE_SUBTYPE) ||
				line.startsWith(DEVICE_SERIAL_NUMBER) ||
				line.startsWith(DEVICE_MAC_ADDRESS))
		.limit(4)
		.map(line->line.split("\\s++", 2))
		.forEach(
				split->{
					final String key = split[0];
					final String value = split[1].split("[\\s#]++", 2)[0];
					deviceProperties.put(key, value);
				});

		return deviceProperties;
	}

	public static void selectProgramByDeviceType(Properties properties) throws IOException {
		logger.traceEntry("{}", properties);

		deviceType = getDeviceType(properties).orElse(null);

		if(deviceType==null)
			return;

		ThreadWorker.runThread(
				()->{

					final String pathToProfileFolder = deviceType + ".path";
					Path programPath = Optional.ofNullable(flash3Properties.getProperty(pathToProfileFolder)).map(Paths::get)

							.orElseGet(
									catchSupplierException(

											()->{

												FutureTask<File> task = ThreadWorker.runFxFutureTask(

														()->{

															FileChooser fileChooser = new FileChooser();
															fileChooser.setTitle("Select the program to upload");
															fileChooser.getExtensionFilters().add(new ExtensionFilter("IRT Technologies BIN file", "*.bin"));
															return fileChooser.showOpenDialog(txtArea.getScene().getWindow());
														});

												File fileForProperties = null;
												fileForProperties = task.get();

												final Optional<File> oResult = Optional.ofNullable(fileForProperties);
												// Save link to profile folder
												oResult.ifPresent(
																f->{
																	try {
																		saveProperty(pathToProfileFolder, f.toString());
																	} catch (IOException e) {
																		throw new WrapperException(e);
																	}
																});

												return oResult.map(File::toPath).orElse(null);
											}));

					uploadWorker.setProgramPath(programPath);
				});
	}

	private static void loadFlashProperties() {

		final String defaultPath = Optional.ofNullable(prefs).map(p->p.get(DEFAULT_TEMPLATE_S_PATH, TEMPLATE_PATH)).orElse(TEMPLATE_PATH);
		defaultDirectory = new File(defaultPath);

		FutureTask<Object> task = null;
		if(!defaultDirectory.exists() || !defaultDirectory.isDirectory()) {

			task = ThreadWorker.runFxFutureTask(
					()->{

						DirectoryChooser chooser = new DirectoryChooser();
						chooser.setTitle("Default Profile's Template Folder");
						defaultDirectory = chooser.showDialog(txtArea.getScene().getWindow());

						if(defaultDirectory==null) {
							Optional.ofNullable(prefs).ifPresent(p->p.remove(DEFAULT_TEMPLATE_S_PATH));
							return null;
						}

						Optional.ofNullable(prefs).ifPresent(p->p.put(DEFAULT_TEMPLATE_S_PATH, defaultDirectory.toString()));
						return null;
					});
		}

		final Optional<FutureTask<Object>> oTask = Optional.ofNullable(task);

		Optional.ofNullable(flash3Properties).orElseGet(
				catchSupplierException(
						()->{

							flash3Properties = new Properties() {
								private static final long serialVersionUID = 3251877953625560093L;

								@Override
							    public synchronized Enumeration<Object> keys() {
							        return Collections.enumeration(new TreeSet<Object>(super.keySet()));
							    }
							};

							if(oTask.isPresent())
								oTask.get().get();

							flash3PropertiesFile = new File(defaultDirectory, "flash3.properties");

							if(!flash3PropertiesFile.exists())
								if(!flash3PropertiesFile.createNewFile())
									return flash3Properties;

							flash3Properties.load(new FileReader(flash3PropertiesFile));
							return flash3Properties;
						}));
	}

	/**
	 * @param properties containing DEVICE_TYPE, DEVICE_REVISION and DEVICE_SUBTYPE
	 * @return	String like DEVICE_TYPE.DEVICE_REVISION.DEVICE_SUBTYPE (ex. 1002.0.0)
	 */
	public static Optional<String> getDeviceType(Properties properties) {

		return 	Optional
				.ofNullable(properties.getProperty(DEVICE_TYPE))
				.flatMap(
						t->
						Optional.ofNullable(properties.getProperty(DEVICE_REVISION))
						.map(r->t + "." + r))
				.flatMap(
						r->
						Optional.ofNullable(properties.getProperty(DEVICE_SUBTYPE))
						.map(s->r + "." + s));
	}

	public static Optional<String> getFlash3Property(String key) {
		return Optional.ofNullable(flash3Properties).map(p->p.getProperty(key));
	}

	private void searchFileBySerialNumber(String serialNumber) {
		logger.traceEntry();

		Optional.ofNullable(serialNumber)
		.map(String::toUpperCase)
		.ifPresent(
				catchConsumerException(
						sn->{

							Flash3App.setSerialNumber(sn);
							Path folderToStart = Optional.ofNullable(Flash3App.properties)

									.map(p->p.getProperty("profiles_path"))
									.map(Paths::get)
									.orElseGet(
											()->{
												DirectoryChooser chooser = new DirectoryChooser();
												chooser.setTitle("Select Profile Folder To Search");
												return Optional.ofNullable(chooser.showDialog(txtArea.getScene().getWindow()))
														.map(File::toPath)
														.orElse(null);});
							try(	final Stream<Path> walk = Files.walk(folderToStart);) {

								Path profilePath = walk

										.filter(p->p.getFileName().toString().toUpperCase().equals(sn + ".BIN"))
										.findFirst()
										.orElse(null);

								uploadWorker.setProfilePath(profilePath, sn);
							}
						}));
	}

	/**
	 * 
	 * @param bytes
	 * @return position of the profile end (0x00);
	 */
	private int getProfileLength(byte[] bytes) {
		for(int length = 0, ffCount = 0, firstFFPosition = -1; length<bytes.length; length++) {

			//Find end of the data (end byte is 0)
			final byte b = bytes[length];
			if(b==0) 
				return length;

			// Several 0xff in a row means no data
			if(b==(byte)0xFF) {

				ffCount++;

				//remember when was the first ff
				if(firstFFPosition<0)
					firstFFPosition = length;

				if(ffCount>6)
					return firstFFPosition;

			}else {
				ffCount =  0;
				firstFFPosition = -1;
			}
		}

		return bytes.length;
	}

	public void setUploadWorker(UploadWorker uploadWorker) {
		DeviceWorker.uploadWorker = uploadWorker;
	}

	public void setProfileWorker(ProfileWorker profileWorker) {
		this.profileWorker = profileWorker;
	}

	public static void saveProperty(String key, String value) throws FileNotFoundException, IOException {

		flash3Properties.put(key, value);
		saveFlash3Properties();
	}

	private static void saveFlash3Properties() throws FileNotFoundException, IOException {
		final String name = flash3PropertiesFile.getName().replace(".properties", ".old");
		final Path path = flash3PropertiesFile.toPath();
		try {
			Files.copy(path, path.resolveSibling(name), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e1) {
			logger.catching(e1);
		}

		try(OutputStream os = new FileOutputStream(flash3PropertiesFile)){

			flash3Properties.store(os, "Flash v3 properties");

		}
	}
}
