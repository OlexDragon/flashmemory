package irt.flash.presentation.panel.edit_profile;

import irt.flash.data.DeviceType;
import irt.flash.data.Informer;
import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;
import irt.flash.data.connection.dao.Database;

import java.awt.Font;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Observer;

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JComboBox;
import javax.swing.SwingWorker;

import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

public class DeviceSubtypePanel extends EditPanel {
	private static final long serialVersionUID = 5529580366809575305L;

	private Informer informer = new Informer("subtype");

	private JComboBox<DeviceType> comboBox;

	public DeviceSubtypePanel() throws ClassNotFoundException, SQLException, IOException {
		super("Device Subtype", ProfileProperties.DEVICE_SUBTYPE);

		List<DeviceType> deviceTypes =  Database.getDeviceSubtypes();
		
		DefaultComboBoxModel<DeviceType> model = new DefaultComboBoxModel<>(deviceTypes.toArray(new DeviceType[deviceTypes.size()]));
		comboBox = new JComboBox<>(model);
		comboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent itemEvent) {
				if(itemEvent.getStateChange()==ItemEvent.SELECTED)
					informer.setValue(comboBox.getSelectedItem());
			}
		});
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

	public void addObserver(final Observer observer) {
		new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				informer.addObserver(observer);
				return null;
			}
		}.execute();
	}

	public void setDeviceSubtype(String deviceSubtype) {
		new DeviceSubtypeWorker(deviceSubtype).execute();
	}

	//******************************************************************************
	private class DeviceSubtypeWorker extends SwingWorker<String, Void>{

		private String deviceSubtype;

		public DeviceSubtypeWorker(String deviceSubtype) {
			this.deviceSubtype = deviceSubtype;
		}

		@Override
		protected String doInBackground() throws Exception {
			if(deviceSubtype!=null && !(deviceSubtype=deviceSubtype.replaceAll("\\D", "")).isEmpty())
				comboBox.setSelectedItem(new DeviceType(Integer.parseInt(deviceSubtype), null));
			return null;
		}
	}
}
