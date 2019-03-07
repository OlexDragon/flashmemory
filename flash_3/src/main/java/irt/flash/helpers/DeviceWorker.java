package irt.flash.helpers;

import java.io.File;
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
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.prefs.Preferences;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import irt.flash.Flash3App;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class DeviceWorker {

	private static final Logger logger = LogManager.getLogger();

	private static final String DEFAULT_TEMPLATE_S_PATH = "Default Template's Path";

	private Preferences prefs;
	private TextArea txtArea;
	private UploadWorker uploadWorker;

	private static File defaultDirectory; public static File getDefaultDirectory() { return defaultDirectory; }

	public DeviceWorker(TextArea txtArea) throws IOException {

		this.txtArea = txtArea;

		prefs = Preferences.userNodeForPackage(getClass());

		final String defaultPath = prefs.get(DEFAULT_TEMPLATE_S_PATH, "Z:\\4Olex\\flash\\templates");
		defaultDirectory = new File(defaultPath);

		if(!defaultDirectory.exists() || !defaultDirectory.isDirectory()) {

			Platform.runLater(
					()->{

						DirectoryChooser chooser = new DirectoryChooser();
						chooser.setTitle("Default Profile's Template Folder");
						defaultDirectory = chooser.showDialog(txtArea.getScene().getWindow());

						if(defaultDirectory==null) {
							prefs.remove(DEFAULT_TEMPLATE_S_PATH);
							return;
						}

						prefs.put(DEFAULT_TEMPLATE_S_PATH, defaultDirectory.toString());
					});
			return;
		}
	}

	public static void fillDeviceGroup(ChoiceBox<File> chbDeviceGroup, ChoiceBox<File> chbDeviceType) {

		Optional.of(defaultDirectory.listFiles(File::isDirectory))
		.filter(l->l.length>0)
		.map(FXCollections::observableArrayList)
		.ifPresent(oal->Platform.runLater(()->chbDeviceGroup.setItems(oal)));

		chbDeviceGroup.getSelectionModel().selectedItemProperty().addListener(
				e->{

					chbDeviceType.getItems().clear();

					final File value = (File) ((ReadOnlyObjectProperty<?>)e).getValue();

					Optional.ofNullable(value)
					.map(dir->dir.listFiles(((d, n) -> n.toLowerCase().endsWith(".bin"))))
					.filter(l->l.length>0)
					.map(FXCollections::observableArrayList)
					.ifPresent(oal->Platform.runLater(()->chbDeviceType.setItems(oal)));
				});
	}

	public void setReadData(ByteBuffer byteBuffer) {
		byte[] bytes = Optional.ofNullable(byteBuffer).map(ByteBuffer::array).orElse(new byte[0]);

		int length = getProfileLength(bytes);

		if(length==0) {
			ProfileWorker.showConfirmationDialog();
			return;
		}
		final String string = new String(bytes, 0, length);

		Platform.runLater(()->txtArea.setText(string));
		logger.info("\n{}", string);

		Optional.ofNullable(uploadWorker).ifPresent(
				uw->ThreadWorker.runThread(
						()->{
			
							Properties properties = new Properties();
							try {

								properties.load(new StringReader(string));
								selectProgramByDeviceType(properties);
								searchFileBySerialNumber(properties);

							} catch (IOException e) {
								logger.catching(e);
							}
		}));
	}

	private void selectProgramByDeviceType(Properties properties) throws IOException {
		final String key = 	getProperty(properties, "device-type") + "." +
							getProperty(properties, "device-revision") + "." +
							getProperty(properties, "device-subtype");

		File propertiesFile = new File(defaultDirectory, "flash3.properties");
		if(!propertiesFile.exists())
			if(!propertiesFile.createNewFile())
				return;

		Properties flash3Properties = new Properties();
		flash3Properties.load(new FileReader(propertiesFile));
		Path programPath = Optional.ofNullable(flash3Properties.getProperty(key))

				.map(Paths::get).orElseGet(
						()->{
							FileChooser fileChooser = new FileChooser();
							fileChooser.setTitle("Select the program to upload");
							fileChooser.getExtensionFilters().add(new ExtensionFilter("IRT Technologies BIN file", "*.bin"));

							final String prefsKey = (key + "_file");
							Optional.ofNullable(prefs.get(prefsKey, null)).ifPresent(

									path->{

										File fileFromPref = new File(path);
										fileChooser.setInitialDirectory(fileFromPref.getParentFile());
										fileChooser.setInitialFileName(fileFromPref.getName());});

							Callable<File> callable = ()->fileChooser.showOpenDialog(txtArea.getScene().getWindow());
							FutureTask<File> task = new FutureTask<>(callable);
							Platform.runLater(task);

							File fileForProperties = null;
							try {
								fileForProperties = task.get();
							} catch (InterruptedException | ExecutionException e) {
								logger.catching(e);
							}

							final Optional<File> oResult = Optional.ofNullable(fileForProperties);
							oResult.ifPresent(
									f->{

										final String name = propertiesFile.getName().replace(".properties", ".old");
										final Path path = propertiesFile.toPath();
										try {
											Files.copy(path, path.resolveSibling(name), StandardCopyOption.REPLACE_EXISTING);
										} catch (IOException e1) {
											logger.catching(e1);
										}

										flash3Properties.put(key, f.toString());
										try(OutputStream os = new FileOutputStream(propertiesFile)){

											flash3Properties.store(os, "Flash v3 properties");

										} catch (IOException e) {
											logger.catching(e);
										}
										});

							return oResult.map(File::toPath).orElse(null);
						});

		uploadWorker.setProgramPath(programPath);
	}

	private String getProperty(Properties properties, String key) {
		return properties.getProperty(key, "_").split("[\\s#]++")[0];
	}

	private void searchFileBySerialNumber(Properties properties) {
		Optional.ofNullable(properties.getProperty("device-serial-number"))
		.map(String::toUpperCase)
		.ifPresent(
				serialNumber->{

					Flash3App.setSerialNumber(serialNumber);
					Path start = Optional.ofNullable(Flash3App.properties)

							.map(p->p.getProperty("profiles_path"))
							.map(Paths::get)
							.orElseGet(
									()->{
										DirectoryChooser chooser = new DirectoryChooser();
										chooser.setTitle("Select Profile Folder To Search");
										return Optional.ofNullable(chooser.showDialog(txtArea.getScene().getWindow()))
												.map(File::toPath)
												.orElse(null);});
					try {

						Files.walk(start)
						.filter(Files::isRegularFile)
						.filter(p->p.getFileName().toString().toUpperCase().equals(serialNumber + ".BIN"))
						.findFirst()
						.ifPresent(uploadWorker::setProfilePath);

					} catch (IOException e) {
						logger.catching(e);
					}
				});
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
		this.uploadWorker = uploadWorker;
	}
}
