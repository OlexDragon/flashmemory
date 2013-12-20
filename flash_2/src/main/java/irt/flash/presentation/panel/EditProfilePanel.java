package irt.flash.presentation.panel;

import javax.swing.JPanel;
import java.awt.FlowLayout;

public class EditProfilePanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private EditSerialNumberPanel editSerialNumber;

	public EditProfilePanel() {
		setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		editSerialNumber = new EditSerialNumberPanel();
		add(editSerialNumber);

	}

}
