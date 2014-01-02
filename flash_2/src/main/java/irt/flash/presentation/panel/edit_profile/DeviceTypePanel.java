package irt.flash.presentation.panel.edit_profile;
import irt.flash.data.DeviceType;
import irt.flash.data.Informer;
import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;
import irt.flash.data.connection.dao.Database;

import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JComboBox;
import javax.swing.SwingWorker;

public class DeviceTypePanel extends EditPanel implements Observer{
	private static final long serialVersionUID = 1823377270737022009L;

	private Informer informer = new Informer("type");
	private JComboBox<DeviceType> comboBox;

	public DeviceTypePanel() throws ClassNotFoundException, SQLException, IOException {
		super("Devile Type", ProfileProperties.DEVICE_TYPE);
		comboBox = new JComboBox<>();
		comboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent itemEvent) {
				if(itemEvent.getStateChange()==ItemEvent.SELECTED){
					informer.setValue((DeviceType) comboBox.getSelectedItem());
				}
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

	public void addObserver(final Observer observer){
		new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				informer.addObserver(observer);
				return null;
			}
		}.execute();
	}

	@Override
	public void update(Observable informer, Object o) {
	}

	public void setDeviceType(String deviceType) {
		new DeviceTypeWorker(deviceType).execute();
	}

	public void setUnitType(String unitType) throws ClassNotFoundException, SQLException, IOException {
		new UnitTypeWorker(unitType).execute();
	}

	//******************************************************************************
	private class DeviceTypeWorker extends SwingWorker<String, Void>{

		private String deviceType;

		public DeviceTypeWorker(String deviceType) {
			this.deviceType = deviceType;
		}

		@Override
		protected String doInBackground() throws Exception {
			if(deviceType!=null && !(deviceType=deviceType.replaceAll("\\D", "")).isEmpty())
				comboBox.setSelectedItem(new DeviceType(Integer.parseInt(deviceType), null));
			return null;
		}
	}

	private class UnitTypeWorker extends SwingWorker<Void, Void>{

		private String unitType;

		public UnitTypeWorker(String unitType) {
			this.unitType = unitType;
		}

		@Override
		protected Void doInBackground() throws Exception {
			List<DeviceType> deviceTypes =  Database.getDeviceTypes(unitType);
			DefaultComboBoxModel<DeviceType> model = deviceTypes!=null ?
																new DefaultComboBoxModel<>(deviceTypes.toArray(new DeviceType[deviceTypes.size()])) :
																	new DefaultComboBoxModel<DeviceType>();
			comboBox.setModel(model);
			informer.setValue((DeviceType) comboBox.getSelectedItem());
			return null;
		}
	}
}
