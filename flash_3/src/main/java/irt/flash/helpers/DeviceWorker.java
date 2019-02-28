package irt.flash.helpers;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
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
								Optional.ofNullable(properties.getProperty("device-serial-number"))
								.map(String::toUpperCase)
								.ifPresent(
										serialNumber->{
					
											Flash3App.setTitle(serialNumber);
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
												.ifPresent(uw::setProfilePath);

											} catch (IOException e) {
												logger.catching(e);
											}
										});

							} catch (IOException e) {
								logger.catching(e);
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
		this.uploadWorker = uploadWorker;
	}
}
