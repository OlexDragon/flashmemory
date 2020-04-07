package irt.flash.helpers;

import static irt.flash.exception.ExceptionWrapper.catchConsumerException;
import static irt.flash.exception.ExceptionWrapper.catchFunctionException;
import static irt.flash.exception.ExceptionWrapper.catchRunnableException;
import static irt.flash.exception.ExceptionWrapper.catchSupplierException;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

import javax.xml.bind.DatatypeConverter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import irt.flash.FlashController;
import irt.flash.data.FlashAnswer;
import irt.flash.data.FlashCommand;
import irt.flash.data.UnitAddress;
import irt.flash.exception.WrapperException;
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
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SingleSelectionModel;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;

public class UploadWorker {

	private static final Logger logger = LogManager.getLogger();

	private static final String KEY_PROGRAM = "last_program_path";
	private static final String OPEN_FILE_LOCATION = "Open File Location";
	private static final String OPEN = "Open Profile";
	private static final int MAX_VAR_RAM_SIZE = ReadFlashWorker.MAX_VAR_RAM_SIZE;

	private ChoiceBox<Object> chbUpload;
	private final ObservableList<Object> list;
	public final static PathHolder profile = new PathHolder();
	private final PathHolder otherProfile = new PathHolder("Other Profile");
	private final PathHolder program = new PathHolder();
	private final PathHolder otherProgram = new PathHolder("Other Program");

	private Preferences prefs;

	private SerialPort serialPort;

	private ChoiceBox<ThreadWorker> chbEdit;

	private ProfileWorker profileWorker;

	public UploadWorker(ChoiceBox<Object> chbUpload, ChoiceBox<ThreadWorker> chbEdit) {

		this.chbUpload= chbUpload;
		this.chbEdit = chbEdit;

		prefs = Preferences.userNodeForPackage(getClass());

		list = FXCollections.observableArrayList("Upload ...", otherProfile, new SeparatorMenuItem(), otherProgram);
		program.setUnitAddress(UnitAddress.PROGRAM);
		otherProgram.setUnitAddress(UnitAddress.PROGRAM);

		final SingleSelectionModel<Object> selectionModel = chbUpload.getSelectionModel();

		Platform.runLater(
				()->{

					chbUpload.setItems(list);
					selectionModel.select(0);

					Optional.ofNullable(prefs.get(KEY_PROGRAM, null)).map(Paths::get).ifPresent(this::setProgramPath);
				});

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

									try {

										final Path path = getPath(pathHolder, unitAddress);
										logger.info("File to upload: {}", path);
										uplodeToTheFlash(unitAddress, path);

									}catch(WrapperException e) {

							    		if(isSerialPortTimeoutException(e)) {

							    			logger.catching(Level.DEBUG, e);
							    			FlashController.showAlert("Connection error.", "Connection timeout", AlertType.ERROR);
							    			return;
							    		}

							    		final NoSuchFileException noSuchFileException = ProfileWorker.getNoSuchFileException(e);

										if(noSuchFileException == null) {
											logger.catching(e);
											return;
										}

										logger.catching(Level.DEBUG, e);
										final String file = noSuchFileException.getFile();

										FlashController.showAlert("Upload profile.", "File not found: " + file, AlertType.ERROR);

							    	}catch(Exception e) {
										logger.catching(e);
									}
								});

						FlashController.disable(false);

						Platform.runLater(()->selectionModel.select(0));
					});
				});
	}

	private boolean isSerialPortTimeoutException(Throwable e){

		final Throwable cause = e.getCause();

		if(cause == null)
			return false;

		return Optional.of(cause).filter(SerialPortTimeoutException.class::isInstance).map(c->true).orElseGet(()->isSerialPortTimeoutException(cause));
	}

	public void uplodeToTheFlash(final Path path) {
		ThreadWorker.runThread(
				catchRunnableException(
						()->{

							final long fileLength = path.toFile().length();
							UnitAddress unitAddress = null;

							// if the file is not so big get UnitAddress from 'unit-type'
							if(fileLength<10*1024) {
								try(Stream<String> stream = Files.lines(path)){

									unitAddress = ProfileWorker.getUnitAddress(stream).orElse(null);

								} catch (MalformedInputException | UncheckedIOException e) {
									logger.info("This is not a profile, perhaps this is a program. {}", e.getMessage());
								}
							}

							// if the UnitAddress cannot be obtained from the profile
							if(unitAddress==null) {

								FutureTask<UnitAddress> task = ThreadWorker.runFxFutureTask(
										()->{
											Alert alert = new Alert(AlertType.CONFIRMATION);
											alert.setTitle("Select the Unit Type");
											alert.setHeaderText(null);
											alert.setContentText("Choose your option.");

											List<ButtonType> buttons = new ArrayList<>();
											final UnitAddress[] values = UnitAddress.values();
											for(int i=0; i<values.length; i++)
												buttons.add(new ButtonType(values[i].name()));
											buttons.add(new ButtonType("Cancel", ButtonData.CANCEL_CLOSE));

											alert.getButtonTypes().setAll(buttons);

											return alert.showAndWait()

													.filter(b->b.getButtonData()!=ButtonData.CANCEL_CLOSE)
													.map(ButtonType::getText)
													.map(UnitAddress::valueOf)
													.orElse(null);});
								unitAddress = task.get();
			}


			uplodeToTheFlash(unitAddress, path);
		}));
	}

	public void uplodeToTheFlash(UnitAddress unitAddress, final Path path) {
		logger.entry(unitAddress, path);

		if(unitAddress==null)
			return;

		Optional.ofNullable(path).ifPresent(
				catchConsumerException(
						pathToFile->{

								final byte[] fileAsBytes = Files.readAllBytes(pathToFile);

								//If not a program add the signature to the end of the file.
								final byte[] signature = Optional.of(unitAddress).filter(ua->!ua.equals(UnitAddress.PROGRAM))
														.map(ua->{
															try {
																return getSignature();
															} catch (IOException e) {
																logger.catching(e);
															}
															return new byte[0];
														}).orElse(new byte[0]);

								logger.debug("signature: {}", signature);

								//	size should always be a multiple of 4.
								int sizeWithSignature = fileAsBytes.length + signature.length;
								int sizeToWrite = Optional.of(sizeWithSignature % 4).filter(modulus->modulus>0).map(modulus->sizeWithSignature + modulus).orElse(sizeWithSignature);

								byte[] arrayToSend = ByteBuffer

										.allocate(sizeToWrite)
										.put(fileAsBytes)
										.put(signature)
										.array();

								FlashController.showAlert("Upload to the flash.", "Erasing Flash Memory.", AlertType.INFORMATION);

								logger.info("Start earasing");

								final int addr = unitAddress.getAddr();
								final boolean erased = FlashWorker.erase(serialPort, addr, arrayToSend.length)

										.filter(answer->answer==FlashAnswer.ACK)
										.isPresent();

								FlashController.closeAlert();

								if(erased) {
									logger.info("Write file to {} memory area ({})", unitAddress, pathToFile);
									writeToFlash(arrayToSend, addr , 0);
								}

								FlashController.showAlert("Upload to the flash.", "The file " + path.getFileName() + " has been loaded into the memory area of the " + unitAddress + ".", AlertType.INFORMATION);
						}));
	}

	private byte[] getSignature() throws IOException, UnknownHostException {
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
		return signature;
	}

	private void writeToFlash(byte[] bytesToWrite, int addr, int offset) throws SerialPortException, SerialPortTimeoutException, InterruptedException {
		logger.debug("addr: {}; offset: {}; bytesToWrite length: {}", addr, offset, bytesToWrite.length);

		FlashController.showProgressBar();

		final int totalLength = bytesToWrite.length;
		while(offset<totalLength && FlashWorker.sendCommand(serialPort, FlashCommand.WRITE_MEMORY).filter(FlashAnswer.ACK::equals).isPresent()) {

			//	Number of bytes to be received (0 < N ≤ 255);	N +1 data bytes:(Max 256 bytes)
			int length = Optional.of(totalLength-offset).filter(l->l<MAX_VAR_RAM_SIZE).orElse(MAX_VAR_RAM_SIZE);

			final byte lengthToWrite = (byte) (length-1);
			final byte[] byteArray = ByteBuffer.allocate(length+1).put(lengthToWrite).put(bytesToWrite, offset, length).array();
			logger.debug("bytes to write: {}; offset: {}; byteArray.length: {}", length, offset, byteArray.length);

			final Optional<byte[]> withCheckSum = FlashWorker.addCheckSum(byteArray);
			if(!withCheckSum.isPresent()) {
				logger.error("Cannot add checksum of 0x{}", DatatypeConverter.printHexBinary(byteArray));
				break;
			}

			byte[] bs = withCheckSum.get();
			int writeTo = addr + offset;
			logger.debug("address to write: {}", writeTo);
			final Optional<FlashAnswer> oFlashAnswer = FlashWorker.addCheckSum(UnitAddress.intToBytes(writeTo))				

					//Bytes 3 to 7: Send the address + checksum
					.flatMap(catchFunctionException(this::writeAndWaitForAck))
					.filter(answer->answer==FlashAnswer.ACK)
					.flatMap(catchFunctionException(b->writeAndWaitForAck(bs)));

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
						catchSupplierException(
								()->{

									FileChooser fileChooser = new FileChooser();
									fileChooser.getExtensionFilters().add(new ExtensionFilter("IRT Technologies BIN file", "*.bin"));
									fileChooser.setTitle("Select the Profile");

									final String key = (unitAddress + "_file");
									Optional.ofNullable(prefs.get(key, null)).map(File::new).filter(File::exists).ifPresent(

											f->{
												fileChooser.setInitialDirectory(f.getParentFile());
												fileChooser.setInitialFileName(f.getName());});
									


									Callable<File> callable = ()->fileChooser.showOpenDialog(chbUpload.getScene().getWindow());
									FutureTask<File> task = new FutureTask<>(callable);
									Platform.runLater(task);

									File file = null;

									file = task.get();

									final Optional<File> oFile = Optional.ofNullable(file);
									oFile
									.ifPresent(f->prefs.put(key, f.toString()));

									return oFile.map(File::toPath).orElse(null);
								}));
	}

	private UnitAddress getUnitAddress(final PathHolder pathHolder) {

		return Optional.of(pathHolder)

				.map(selectedItem->selectedItem.unitAddress)
				.orElseGet(
						catchSupplierException(
								()->{

									FutureTask<UnitAddress> task = ThreadWorker.runFxFutureTask(
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

									return task.get();
						}));
	}

	/**
	 * @param path to profile
	 * This function remove item 'profile' (PathHolder) from chbUpload (ChoiceBox)
	 * @param serialNumber 
	 */
	public void setProfilePath(Path path, String serialNumber) {

		final ObservableList<Object> uploadItems = chbUpload.getItems();
		final ObservableList<ThreadWorker> editItems = chbEdit.getItems();

		profile.setPath(path);
		Platform.runLater(()->uploadItems.remove(profile));

		if(path==null) {
			Platform.runLater(
					()->{

						editItems.add(new ThreadWorker("Save " + serialNumber, ()->profileWorker.saveProfile(null)));
					});
			return;
		}

		Platform.runLater(
				()->{
					uploadItems.add(1, profile);

					editItems.add(new ThreadWorker(OPEN, ()->{ try { Desktop.getDesktop().open(path.toFile()); } catch (IOException e) { logger.catching(e); }}));
					editItems.add(new ThreadWorker(OPEN_FILE_LOCATION, ()->{try { Runtime.getRuntime().exec("explorer.exe /select," + path); } catch (IOException e) { logger.catching(e); }})); });
	}

	public void setProgramPath(Path path) {

		final ObservableList<Object> uploadItems = chbUpload.getItems();

		program.setPath(path);
		Platform.runLater(()->uploadItems.remove(program));

		Optional.ofNullable(path).ifPresent(
				p->{

					prefs.put(KEY_PROGRAM, path.toString());
					Platform.runLater(
							()->{
								final int index = uploadItems.indexOf(otherProgram);
								uploadItems.add(index, program); }); });
	}

	private Optional<FlashAnswer> writeAndWaitForAck(byte[] bytes) throws SerialPortException, SerialPortTimeoutException, InterruptedException {

			return FlashWorker.sendBytes(serialPort, bytes, 500);
	}

	public static class PathHolder{

		private String title;
		private Path path;
		private UnitAddress unitAddress;

		public PathHolder() {}
		public PathHolder(String title) {
			this.title = title;
		}

		public Path getPath() {
			return path;
		}
		public void setPath(Path path) {
			this.path = path;
			this.title = Optional.ofNullable(path).map(Path::getFileName).map(Path::toString).orElse("No Path Specified");
		}

		public UnitAddress getUnitAddress() {
			return unitAddress;
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

	public void setProfileWorker(ProfileWorker profileWorker) {
		this.profileWorker = profileWorker;
	}
}
