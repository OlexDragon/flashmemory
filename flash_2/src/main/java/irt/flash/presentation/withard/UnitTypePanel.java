package irt.flash.presentation.withard;

import irt.flash.data.connection.MicrocontrollerSTM32.Address;

import java.awt.Font;
import java.util.prefs.Preferences;

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JComboBox;
import javax.swing.JPanel;

public class UnitTypePanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private static final Preferences prefs = Preferences.userRoot().node("IRT Technologies inc.");

	public UnitTypePanel() {

		DefaultComboBoxModel<String> defaultComboBoxModel = new DefaultComboBoxModel<String>();
		for(Address v:Address.values())
			if(v!=Address.PROGRAM)
				defaultComboBoxModel.addElement(v.toString());
		JComboBox<String> comboBox = new JComboBox<String>(defaultComboBoxModel);
		comboBox.setFont(new Font("Tahoma", Font.BOLD, 14));
		comboBox.setSelectedItem(prefs.get("Unit Type", "Select Unit Type"));

		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addComponent(comboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addContainerGap(412, Short.MAX_VALUE))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addComponent(comboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addContainerGap(269, Short.MAX_VALUE))
		);
		setLayout(groupLayout);
	}
}
