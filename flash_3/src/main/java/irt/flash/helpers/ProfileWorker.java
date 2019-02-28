package irt.flash.helpers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.application.Platform;
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
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

public class ProfileWorker {

	private static final Logger logger = LogManager.getLogger();
	private static Node node;

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
					grid.setGridLinesVisible(true);

					final ObservableList<RowConstraints> rowConstraints = grid.getRowConstraints();
					final RowConstraints rc = new RowConstraints();
					rc.setVgrow(Priority.ALWAYS);
					rowConstraints.add(rc);
					rowConstraints.add(rc);

					final ObservableList<ColumnConstraints> columnConstraints = grid.getColumnConstraints();
					final ColumnConstraints cc = new ColumnConstraints();
					cc.setHgrow(Priority.ALWAYS);
					columnConstraints.add(cc);
					columnConstraints.add(cc);

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
		oProfile.ifPresent(profile->{
			final Properties properties = propertiesFrom(profile.getParentFile());
			final String propertyName = profile.getName();
			final String property = properties.getProperty(propertyName);
			final File saveDirectory = Optional.ofNullable(property).filter(String::isEmpty).map(File::new).orElseGet(()->chooseDirectory(propertyName, profile, properties));

			if(saveDirectory==null)
				return;

			Platform.runLater(
					()->{

						Dialog<File> dialog = new Dialog<>();
						Optional.ofNullable(node).map(Node::getScene).map(Scene::getWindow)
								.ifPresent(dialog::initOwner);
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
						grid.setPadding(new Insets(20, 150, 10, 10));
						grid.getRowConstraints().add(new RowConstraints());

						grid.add(new Label("Serial Number:"), 0, 0);
						dialog.showAndWait();
					});
		});
	}

	private static File chooseDirectory(String propertyName, File dir, Properties properties) {
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Select a profile folder to save " + propertyName);
		final Window window = Optional.ofNullable(node).map(Node::getScene).map(Scene::getWindow).orElse(null);
		final File selectedDirectory = chooser.showDialog(window);

		Optional.ofNullable(selectedDirectory).map(File::getAbsolutePath).ifPresent(
				path->{
					File saveDir = Optional.of(dir).filter(File::isDirectory).orElseGet(()->dir.getParentFile());
					final String name = saveDir.getName();
					File saveFile = new File(saveDir, name + ".properties");
					try(OutputStream os = new FileOutputStream(saveFile)){
						properties.put(propertyName, selectedDirectory.getAbsolutePath());
						properties.store(os, name + " - profile destination folders");
					} catch (IOException e) {
						logger.catching(e);
					}
					
				});

		return selectedDirectory;
	}

	private static Properties propertiesFrom(File dir) {
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

}
