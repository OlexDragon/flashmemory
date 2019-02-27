package irt.flash.helpers;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.prefs.Preferences;

import javax.xml.bind.DatatypeConverter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import irt.flash.FlashController;
import irt.flash.data.FlashAnswer;
import irt.flash.data.FlashCommand;
import irt.flash.data.UnitAddress;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.SingleSelectionModel;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;

public class UploadWorker {

	private static final Logger logger = LogManager.getLogger();

	private static final int MAX_VAR_RAM_SIZE = ReadFlashWorker.MAX_VAR_RAM_SIZE;

	private ChoiceBox<Object> chbUpload;
	private final ObservableList<Object> list;
	private final PathHolder profile = new PathHolder();
	private final PathHolder otherProfile = new PathHolder("Other Profile");
	private final PathHolder program = new PathHolder();
	private final PathHolder otherProgram = new PathHolder("Other Program");

	private Preferences prefs;

	private SerialPort serialPort;

	public UploadWorker(ChoiceBox<Object> chbUpload) {

		this.chbUpload= chbUpload;
		prefs = Preferences.userNodeForPackage(getClass());

		list = FXCollections.observableArrayList("Upload ...", otherProfile, otherProgram);
		program.setUnitAddress(UnitAddress.PROGRAM);
		otherProgram.setUnitAddress(UnitAddress.PROGRAM);

		final SingleSelectionModel<Object> selectionModel = chbUpload.getSelectionModel();

		Platform.runLater(
				()->{
					chbUpload.setItems(list);
					selectionModel.select(0);});

		selectionModel.selectedItemProperty().addListener(
				roop->{

					final ReadOnlyObjectProperty<?> readOnlyObjectProperty = (ReadOnlyObjectProperty<?>)roop;
					final Object bean = readOnlyObjectProperty.getBean();
					final int selectedIndex = ((SelectionModel<?>)bean).getSelectedIndex();

					// 	First index is the message: "Upload ..."
					if(selectedIndex==0)
						return;

					FlashController.disable(true);
					ThreadWorker.runThread(()->{

						final PathHolder pathHolder = (PathHolder)readOnlyObjectProperty.getValue();
						Optional.ofNullable(getUnitAddress(pathHolder)).ifPresent(

								unitAddress->{
									
									final Path path = getPath(pathHolder, unitAddress);
									logger.info("File to upload: {}", path);
									Optional.ofNullable(path).ifPresent(
											p->{

												try {

													final byte[] fileAsBytes = Files.readAllBytes(p);

													//Add signature to the end of the file.
													Properties properties = new Properties();
													properties.load(getClass().getResourceAsStream("/project.properties"));
													String version = properties.getProperty("version");
													final byte[] signature = new StringBuffer()

															.append("\n#Uploaded by Flash v")
															.append(version)
															.append(" on ")
															.append(new Timestamp(new Date().getTime()))
															.append(" from ")
															.append(InetAddress.getLocalHost().getHostName())
															.append(" computer.")
															.toString()
															.getBytes();

													//	size should always be a multiple of 4.
													int size = fileAsBytes.length + signature.length;
													int sizeToWrite = Optional.of(size % 4).filter(modulus->modulus>0).map(modulus->size + modulus).orElse(size);
													logger.error("sizeToWrite: {}", sizeToWrite);

													byte[] arrayToSend = ByteBuffer

															.allocate(sizeToWrite)
															.put(fileAsBytes)
															.put(signature)
															.array();

													FlashController.showAlert("Erasing Flash Memory.", AlertType.INFORMATION);
													final boolean erased = FlashWorker.erase(serialPort, unitAddress.getAddr(), arrayToSend.length).filter(answer->answer==FlashAnswer.ACK).isPresent();
													FlashController.closeAlert();

													if(erased)
														writeToFlash(arrayToSend, unitAddress.getAddr() , 0);

												} catch (IOException | SerialPortException e) {
													logger.catching(e);
												} catch (SerialPortTimeoutException e) {
													logger.catching(Level.DEBUG, e);
													FlashController.showAlert("Connection timeout", AlertType.ERROR);
												}});

								});

						FlashController.disable(false);

						Platform.runLater(()->selectionModel.select(0));
					});
				});
	}

	private void writeToFlash(byte[] bytesToWrite, int addr, int offset) throws SerialPortException, SerialPortTimeoutException {

		FlashController.showProgressBar();

		final int totalLength = bytesToWrite.length;
		while(offset<totalLength && FlashWorker.sendCommand(serialPort, FlashCommand.WRITE_MEMORY).filter(answer->answer==FlashAnswer.ACK).isPresent()) {

			//	Number of bytes to be received (0 < N ≤ 255);	N +1 data bytes:(Max 256 bytes)
			int length = Optional.of(totalLength-offset).filter(l->l<MAX_VAR_RAM_SIZE).orElse(MAX_VAR_RAM_SIZE);

			final byte lengthToWrite = (byte) (length-1);
			final byte[] byteArray = ByteBuffer.allocate(length+1).put(lengthToWrite).put(bytesToWrite, offset, length).array();
			logger.debug("bytes to write: {}; offset: {}; byteArray.length: {}", length, offset, byteArray.length);

			final Optional<byte[]> addCheckSum = FlashWorker.addCheckSum(byteArray);
			if(!addCheckSum.isPresent()) {
				logger.error("Cannot add checksum of 0x{}", DatatypeConverter.printHexBinary(byteArray));
				break;
			}

			byte[] bs = addCheckSum.get();
			final Optional<FlashAnswer> oFlashAnswer = FlashWorker.addCheckSum(UnitAddress.intToBytes(addr + offset))				

					//Bytes 3 to 7: Send the address + checksum
					.flatMap(this::writeAndWaitForAck)
					.flatMap(b->writeAndWaitForAck(bs));

			if(!oFlashAnswer.filter(a->a==FlashAnswer.ACK).isPresent())
				break;

			offset += length;
			double progress = offset/(double)totalLength;
			FlashController.setProgressBar(progress);
		}
		FlashController.closeProgressBar();
	}

	private Path getPath(final PathHolder pathHolder, UnitAddress unitAddress) {

		return Optional.ofNullable(pathHolder.path)
				.orElseGet(

						()->{

							FileChooser fileChooser = new FileChooser();
							fileChooser.getExtensionFilters().add(new ExtensionFilter("IRT Technologies BIN file", "*.bin"));
							fileChooser.setTitle("Select the Profile");

							final String key = (unitAddress + "_file");
							Optional.ofNullable(prefs.get(key, null)).ifPresent(

									path->{

										File p = new File(path);
										fileChooser.setInitialDirectory(p.getParentFile());
										fileChooser.setInitialFileName(p.getName());});


							Callable<File> callable = ()->fileChooser.showOpenDialog(chbUpload.getScene().getWindow());
							FutureTask<File> ft = new FutureTask<>(callable);
							Platform.runLater(ft);

							File file = null;
							try {
								file = ft.get();
							} catch (InterruptedException | ExecutionException e) {
								logger.catching(e);
							}

							final Optional<File> oFile = Optional.ofNullable(file);
							oFile
							.ifPresent(f->prefs.put(key, f.toString()));

							return oFile.map(File::toPath).orElse(null);
						});
	}

	private UnitAddress getUnitAddress(final PathHolder pathHolder) {

		return Optional.of(pathHolder)

				.map(selectedItem->selectedItem.unitAddress)
				.orElseGet(
						()->{
							FutureTask<UnitAddress> ft = new FutureTask<UnitAddress>(
									()->{
										Alert alert = new Alert(AlertType.CONFIRMATION);
										alert.setTitle("Select the Unit Type");
										alert.setHeaderText(null);
										alert.setContentText("Choose your option.");

										List<ButtonType> buttons = new ArrayList<>();
										final UnitAddress[] values = UnitAddress.values();
										for(int i=1; i<values.length; i++)
											buttons.add(new ButtonType(values[i].name()));
										buttons.add(new ButtonType("Cancel", ButtonData.CANCEL_CLOSE));

										alert.getButtonTypes().setAll(buttons);

										return alert.showAndWait()

												.filter(b->b.getButtonData()!=ButtonData.CANCEL_CLOSE)
												.map(ButtonType::getText)
												.map(UnitAddress::valueOf)
												.orElse(null);});
							try {

								Platform.runLater(ft);
								return ft.get();

							} catch (InterruptedException | ExecutionException e) {
								logger.catching(e);
							}

							return null;
						});
	}

	public void setProfilePath(Path path) {

		final Optional<Path> oPath = Optional.ofNullable(path);

		Platform.runLater(()->chbUpload.getItems().remove(profile));

		oPath.ifPresent(
				p->{

					profile.setPath(p);

					Platform.runLater(()->chbUpload.getItems().add(1, profile));});
	}

	private Optional<FlashAnswer> writeAndWaitForAck(byte[] bytes) {

		try {

			return FlashWorker.sendBytes(serialPort, bytes, 500);

		} catch (SerialPortException e) {
			logger.catching(e);
		} catch (SerialPortTimeoutException e) {
			logger.catching(Level.DEBUG, e);
			FlashController.showAlert("Connection timeout", AlertType.ERROR);
		}
		return Optional.empty();
	}

	private class PathHolder{

		private String title;
		private Path path;
		private UnitAddress unitAddress;

		public PathHolder() {}
		public PathHolder(String title) {
			this.title = title;
		}

		public void setPath(Path path) {
			this.path = path;
			this.title = path.getFileName().toString();
		}

		public void setUnitAddress(UnitAddress unitAddress) {
			this.unitAddress = unitAddress;
		}

		@Override
		public String toString() {
			return title;
		}
	}

	public void setUnitAddress(UnitAddress unitAddress) {
		profile.setUnitAddress(unitAddress);
		otherProfile.setUnitAddress(unitAddress);
	}

	public void setSerialPort(SerialPort serialPort) {
		this.serialPort = serialPort;
	}
}
