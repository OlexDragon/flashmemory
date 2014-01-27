package irt.flash.presentation.panel;

import java.awt.BorderLayout;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JPanel;

public class ProfileWorkerPanel extends JPanel implements Observer {
	private static final long serialVersionUID = 863089977016844508L;

	private EditProfilePanel editProfilePanel;

	public ProfileWorkerPanel() throws Exception {
		
		JPanel buttonsPanel = new JPanel();
		setLayout(new BorderLayout(0, 0));
		add(buttonsPanel, BorderLayout.NORTH);
		
		JButton btnGetProfile = new JButton("Get Profile");
		buttonsPanel.add(btnGetProfile);

		editProfilePanel = new EditProfilePanel();
		add(editProfilePanel, BorderLayout.CENTER);
	}
	public void setUnitType(String unitType) throws Exception {
		editProfilePanel.setUnitType(unitType);
	}
	public void resetProfileVariables() {
		editProfilePanel.resetProfileVariables();
	}
	@Override
	public void update(Observable o, Object obj) {
		editProfilePanel.update(o, obj);
	}
}
