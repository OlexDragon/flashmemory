package irt.flash.presentation.panel.edit_profile;

import irt.flash.data.DeviceType;
import irt.flash.data.Informer;
import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;
import irt.flash.data.connection.dao.Database;
import irt.flash.presentation.panel.edit_profile.extendable.EditComboBoxPanel;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Observer;

import javax.swing.DefaultComboBoxModel;
import javax.swing.SwingWorker;

public class DeviceSubtypePanel extends EditComboBoxPanel<DeviceType> {
	private static final long serialVersionUID = 5529580366809575305L;

	private Informer informer = new Informer("subtype");

	public DeviceSubtypePanel() throws Exception {
		super("Device Subtype", ProfileProperties.DEVICE_SUBTYPE);
	}

	public void addObserver(final Observer observer) {
		new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				informer.addObserver(observer);
				return null;
			}
		}.execute();
	}

	@Override
	public void setValue(Object deviceSubtype) {
		String str = (String) deviceSubtype;
		if(str!=null && !(str=str.replaceAll("\\D", "")).isEmpty())
			super.setValue(new DeviceType(Integer.parseInt(str), null));
	}

	@Override
	protected void setComboBoxModel() throws Exception {
		List<DeviceType> deviceTypes =  Database.getDeviceSubtypes();
		DefaultComboBoxModel<DeviceType> model = new DefaultComboBoxModel<>(deviceTypes.toArray(new DeviceType[deviceTypes.size()]));
		comboBox.setModel(model);
		comboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent itemEvent) {
				if(itemEvent.getStateChange()==ItemEvent.SELECTED)
					informer.setValue(comboBox.getSelectedItem());
			}
		});
	}
}
