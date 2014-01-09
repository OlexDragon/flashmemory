package irt.flash.presentation.panel.edit_profile;
import irt.flash.data.DeviceType;
import irt.flash.data.Informer;
import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;
import irt.flash.data.connection.dao.Database;
import irt.flash.presentation.panel.edit_profile.extendable.EditComboBoxPanel;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.DefaultComboBoxModel;
import javax.swing.SwingWorker;

public class DevicePartNumberPanel extends EditComboBoxPanel<String> implements Observer{
	private static final long serialVersionUID = -6807620162853088371L;

	private Informer informer = new Informer("part number");

	private int deviceType;
	private int deviceSubtype;

	private ItemListener itemListener;

	public DevicePartNumberPanel() throws Exception {
		super("Device Part Number", ProfileProperties.DEVICE_PART_NUMBER);
	}

	public void addObserver(Observer observer) {
		informer.addObserver(observer);
	}

	@Override
	protected void setComboBoxModel() throws Exception {
		new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				List<String> productDescription = Database.getPartNumbers(deviceType, deviceSubtype);
				DefaultComboBoxModel<String> model = productDescription != null ?
																	new DefaultComboBoxModel<>(productDescription.toArray(new String[productDescription.size()])) :
																		new DefaultComboBoxModel<String>();
				comboBox.setModel(model);
				String selectedItem = (String) comboBox.getSelectedItem();
				informer.setValue(selectedItem);
				oldValues.add(selectedItem);
				setBackground(selectedItem);

				itemListener = new ItemListener() {
					public void itemStateChanged(ItemEvent itemEvent) {
						if(itemEvent.getStateChange()==ItemEvent.SELECTED)
							new SwingWorker<Void, Void>() {
								@Override
								protected Void doInBackground() throws Exception {
									logger.trace(informer);
									String selectedItem = (String) comboBox.getSelectedItem();
									informer.setValue(selectedItem);
									oldValues.add(selectedItem);
									setBackground(selectedItem);
									return null;
								}
							}.execute();
					}
				};

				comboBox.addItemListener(itemListener);
				return null;
			}
		}.execute();
	}

	@Override
	public void update(Observable informer, Object o) {
		logger.trace("{}, {}", informer, o);

		if(informer instanceof Informer)
			new UpdateWorker((Informer)informer).execute();
	}

	//*****************************************************************************************
	private class UpdateWorker extends SwingWorker<Informer, Void>{

		private Informer informer;

		public UpdateWorker(Informer informer) {
			this.informer = informer;
		}

		@Override
		protected Informer doInBackground() throws Exception {
			logger.trace(informer);
			try {
				switch(informer.getName()){
				case "type":
					if(getDeviceType())
						setComboBoxModel();
					break;
				case "subtype":
					if(getDeviceSubtype())
						setComboBoxModel();
					break;
				case "description":
					setComboBoxSelectedItem((String)informer.getValue());
				}
			} catch (Exception e) {
				logger.catching(e);
			}
			return null;
		}	

		private boolean getDeviceType() {
			int dt = getDeviceTypeInt();

			boolean changed;
			if(deviceType!=dt){
				deviceType = dt;
				changed = true;
			}else
				changed = false;

			return changed;
		}

		private boolean getDeviceSubtype() {
			int dt = getDeviceTypeInt();

			boolean changed;
			if(deviceSubtype!=dt){
				deviceSubtype = dt;
				changed = true;
			}else
				changed = false;

			return changed;
		}

		private int getDeviceTypeInt() {
			DeviceType value = (DeviceType) informer.getValue();
			int dt = value!=null ? value.getDeviceType() : 0;
			return dt;
		}

		private void setComboBoxSelectedItem(final String description) throws ClassNotFoundException, SQLException, IOException {
			logger.trace(description);
			List<String> productPartNumbers = Database.getPartNumbers(description);
			if (productPartNumbers != null && !productPartNumbers.contains(comboBox.getSelectedItem())) {
				comboBox.removeItemListener(itemListener);
				String anObject = productPartNumbers.get(0);
				comboBox.setSelectedItem(anObject);
				oldValues.add(anObject);
				setBackground(anObject);
				comboBox.addItemListener(itemListener);
			}
		}
	}
}
