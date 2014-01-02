package irt.flash.presentation.panel.edit_profile;

import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;

import java.awt.Color;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

public class EditPanel extends JPanel {

	private static final long serialVersionUID = -5075735149795277563L;

	public enum Scope{
		COMMON,
		FCM,
		BUC
	}

	private final ProfileProperties profileProperties;
	private Scope scope;

	public EditPanel(String title, ProfileProperties profileProperties) throws ClassNotFoundException, SQLException, IOException {
		this.profileProperties = profileProperties;
		setScope(profileProperties);
		setBorder(new TitledBorder(new LineBorder(new Color(0, 0, 0), 1, true), title, TitledBorder.LEFT, TitledBorder.TOP, getFont().deriveFont(16F), Color.BLUE));
	}

	public ProfileProperties getProfileProperties() {
		return profileProperties;
	}

	public Scope getScope() {
		return scope;
	}

	private void setScope(ProfileProperties profileProperties){
		List<String> properties = profileProperties.getProperties();

		if(properties.contains("FCM"))
			scope = Scope.FCM;
		else if(properties.contains("BUC"))
			scope = Scope.BUC;
		else
			scope = Scope.COMMON;
	}
}
