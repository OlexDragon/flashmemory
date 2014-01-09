package irt.flash.presentation.panel.edit_profile;
import irt.flash.data.DeviceType;
import irt.flash.data.Informer;
import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;
import irt.flash.data.connection.dao.Database;
import irt.flash.presentation.panel.edit_profile.extendable.EditComboBoxPanel;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.DefaultComboBoxModel;
import javax.swing.SwingWorker;

public class DeviceTypePanel extends EditComboBoxPanel<DeviceType> implements Observer{
	private static final long serialVersionUID = 1823377270737022009L;

	private Informer informer = new Informer("type");

	public DeviceTypePanel() throws Exception {
		super("Devile Type", ProfileProperties.DEVICE_TYPE);
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

	@Override
	public void setValue(Object deviceType) {
		String str = (String)deviceType;
		if(str!=null && !(str=str.replaceAll("\\D", "")).isEmpty())
			super.setValue(new DeviceType(Integer.parseInt(str), null));
	}

	public void setUnitType(String unitType) throws Exception {
		new UnitTypeWorker(unitType).execute();
	}

	@Override
	protected void setComboBoxModel() throws Exception {
		new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				comboBox.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent itemEvent) {
						if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
							logger.trace(informer);
							informer.setValue((DeviceType) comboBox.getSelectedItem());
						}
					}
				});
				return null;
			}
		}.execute();
	}

	//******************************************************************************

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
			logger.trace(informer);
			return null;
		}
	}
}
