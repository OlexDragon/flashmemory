package irt.flash.helpers;

import java.util.Optional;
import java.util.prefs.Preferences;

import javafx.stage.Stage;

public class StageSizeAndPosition {

	private Stage stage;
	private Preferences prefs;

	public StageSizeAndPosition(Class<?> key) {
		prefs = Preferences.userNodeForPackage(key);
	}

	public void setStageProperties(Stage stage) {
		this.stage = stage;
		stage.setFullScreen(prefs.getBoolean("FullScreen", false));
		stage.setHeight(prefs.getDouble("Height", 200));
		stage.setWidth(prefs.getDouble("Width", 200));
		stage.setX(prefs.getDouble("X", 0));
		stage.setY(prefs.getDouble("Y", 0));
	}

	public void saveStageProperties() {
		saveStageProperties(stage);
	}

	public void saveStageProperties(Stage stage) {
		Optional
		.ofNullable(stage)
		.ifPresent(s->{

			final boolean fullScreen = stage.isFullScreen();
			prefs.putBoolean("FullScreen", fullScreen);
			if(fullScreen)
				return;

			prefs.putDouble("Height", stage.getHeight());
			prefs.putDouble("Width", stage.getWidth());
			prefs.putDouble("X", stage.getX());
			prefs.putDouble("Y", stage.getY());
		});
	}
}
