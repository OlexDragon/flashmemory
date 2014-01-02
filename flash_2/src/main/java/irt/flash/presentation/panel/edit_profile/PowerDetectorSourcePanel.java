package irt.flash.presentation.panel.edit_profile;

import irt.flash.data.ValueDescription;
import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;
import irt.flash.data.connection.dao.Database;

import java.awt.Font;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JComboBox;

public class PowerDetectorSourcePanel extends EditPanel {
	private static final long serialVersionUID = 4533710621923096958L;
	private JComboBox<ValueDescription> comboBox;

	public PowerDetectorSourcePanel() throws ClassNotFoundException, SQLException, IOException {
		super("Power Detector Source", ProfileProperties.POWER_DETECTOR_SOURCE);

		List<ValueDescription> powerDetectorSources =  Database.getPowerDetectorSources();
		DefaultComboBoxModel<ValueDescription> model = powerDetectorSources!=null ?
															new DefaultComboBoxModel<>(powerDetectorSources.toArray(new ValueDescription[powerDetectorSources.size()])) :
																new DefaultComboBoxModel<ValueDescription>();
		comboBox = new JComboBox<>(model);
		comboBox.setFont(new Font("Tahoma", Font.BOLD, 16));

		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addComponent(comboBox, 110, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addComponent(comboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
		);
		setLayout(groupLayout);
	}

}
