package irt.flash.helpers;

import java.awt.Dimension;
import java.awt.Toolkit;
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

		stage.setHeight(Optional.of(prefs.getDouble("Height", 200)).filter(h->h>200).orElse(200.0));
		stage.setWidth(Optional.of(prefs.getDouble("Width", 200)).filter(w->w>200).orElse(200.0));

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		double posotionX = prefs.getDouble("X", 0);
		double posotionY = prefs.getDouble("Y", 0);

		stage.setX(Optional.of(posotionX).filter(x->x>0).filter(x->x<screenSize.getWidth()).orElse(0.0));
		stage.setY(Optional.of(posotionY).filter(y->y>0).filter(y->y<screenSize.getHeight()).orElse(0.0));
	}

	public void saveStageProperties() {
		saveStageProperties(stage);
	}

	public void saveStageProperties(Stage stage) {
		Optional
		.ofNullable(stage)
		.ifPresent(s->{

			final boolean fullScreen = stage.isFullScreen();
			if(fullScreen)
				return;

			Optional.of(stage.getHeight()).filter(h->h>=0).ifPresent(h->prefs.putDouble("Height", h));
			Optional.of(stage.getWidth()).filter(w->w>=0).ifPresent(w->prefs.putDouble("Width", w));
			Optional.of(stage.getX()).filter(x->x>=0).ifPresent(x->prefs.putDouble("X", x));
			Optional.of(stage.getY()).filter(y->y>=0).ifPresent(y->prefs.putDouble("Y", y));
		});
	}
}
