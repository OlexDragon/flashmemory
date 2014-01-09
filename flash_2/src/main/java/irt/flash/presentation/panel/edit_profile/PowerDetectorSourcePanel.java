package irt.flash.presentation.panel.edit_profile;

import irt.flash.data.ValueDescription;
import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;
import irt.flash.data.connection.dao.Database;
import irt.flash.presentation.panel.edit_profile.extendable.EditComboBoxPanel;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.SwingWorker;

public class PowerDetectorSourcePanel extends EditComboBoxPanel<ValueDescription> {
	private static final long serialVersionUID = 4533710621923096958L;

	public PowerDetectorSourcePanel() throws Exception {
		super("Power Detector Source", ProfileProperties.POWER_DETECTOR_SOURCE);
		comboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent itemEvent) {
				if(itemEvent.getStateChange()==ItemEvent.SELECTED)
					new SwingWorker<Void, Void>() {

						@Override
						protected Void doInBackground() throws Exception {
							ValueDescription selectedItem = (ValueDescription) comboBox.getSelectedItem();
							oldValues.add(selectedItem);
							setBackground(selectedItem);
							return null;
						}
					}.execute();
			}
		});

	}

	@Override
	protected void setComboBoxModel() throws Exception {
		new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				List<ValueDescription> powerDetectorSources =  Database.getPowerDetectorSources();
				DefaultComboBoxModel<ValueDescription> model = powerDetectorSources!=null ?
																	new DefaultComboBoxModel<>(powerDetectorSources.toArray(new ValueDescription[powerDetectorSources.size()])) :
																		new DefaultComboBoxModel<ValueDescription>();
				comboBox.setModel(model);
				ValueDescription selectedItem = (ValueDescription)comboBox.getSelectedItem();
				oldValues.add(selectedItem);
				setBackground(selectedItem);
				return null;
			}
		}.execute();
	}
}
