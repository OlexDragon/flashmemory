package irt.flash.presentation.panel.edit_profile;
import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;

import javax.swing.GroupLayout;
import javax.swing.SwingWorker;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JTextField;

import java.awt.Font;
import java.io.IOException;
import java.sql.SQLException;

public class DeviceRevisionPanel extends EditPanel {
	private static final long serialVersionUID = -1234658465043904517L;
	private JTextField textField;

	public DeviceRevisionPanel() throws ClassNotFoundException, SQLException, IOException {
		super("Device Revision", ProfileProperties.DEVICE_REVISION);
		
		textField = new JTextField();
		textField.setFont(new Font("Tahoma", Font.BOLD, 16));
		textField.setColumns(10);
		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addComponent(textField, 110, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addComponent(textField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
		);
		setLayout(groupLayout);
	}

	public void setDeviceRevision(final String deviceRevision) {
		new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				textField.setText(deviceRevision!=null ? deviceRevision : "");
				return null;
			}
		};
	}
}
