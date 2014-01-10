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


public class ProductDescriptionPanel extends EditComboBoxPanel<String> implements Observer{
	private static final long serialVersionUID = 7558824145840843702L;

	private Informer informer = new Informer("description");

	private int deviceType;
	private int deviceSubtype;
	private ItemListener itemListener = new ItemListener() {
		public void itemStateChanged(ItemEvent itemEvent) {
			if(itemEvent.getStateChange()==ItemEvent.SELECTED)
				new SwingWorker<Void, Void>() {

					@Override
					protected Void doInBackground() throws Exception {
						String selectedItem = (String) comboBox.getSelectedItem();
						informer.setValue(selectedItem);
						oldValues.add(selectedItem);
						setBackground(selectedItem);
						logger.debug("itemStateChanged = {}", selectedItem);
						return null;
					}
				}.execute();
		}
	};

	public ProductDescriptionPanel() throws Exception {
		super("Product Description", ProfileProperties.PRODUCT_DESCRIPTION);
		comboBox.addItemListener(itemListener);
		comboBox.setEditable(true);
	}

	@Override
	public void update(Observable informer, Object o) {
		logger.debug("{}, {}", informer, o);

		if(informer instanceof Informer)
			new UpdateWorker((Informer)informer).execute();
	}

	public void addObserver(Observer observer) {
		informer.addObserver(observer);
	}

	@Override
	protected void setComboBoxModel() throws Exception {
	}

	//****************************************************************************************
	private class UpdateWorker extends SwingWorker<Informer, Void>{

		private Informer informer;

		public UpdateWorker(Informer informer) {
			this.informer = informer;
		}

		@Override
		protected Informer doInBackground() throws Exception {
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
				case "part number":
					setComboBoxSelectedItem((String)informer.getValue());
				}
			} catch (Exception e) {
				logger.catching(e);
			}
			return null;
		}

		private boolean getDeviceType() {
			int dt = getDeviceTypeInt();
			logger.entry(dt);

			boolean changed;
			if(deviceType!=dt){
				deviceType = dt;
				changed = true;
			}else
				changed = false;

			return logger.exit(changed);
		}

		private boolean getDeviceSubtype() {
			int dt = getDeviceTypeInt();
			logger.entry(dt);

			boolean changed;
			if(deviceSubtype!=dt){
				deviceSubtype = dt;
				changed = true;
			}else
				changed = false;

			return logger.exit(changed);
		}

		private int getDeviceTypeInt() {
			DeviceType value = (DeviceType) informer.getValue();
			int dt = value!=null ? value.getDeviceType() : 0;
			return dt;
		}

		private void setComboBoxModel() throws IOException, ClassNotFoundException, SQLException, InterruptedException {
			if (deviceType != 0 && deviceSubtype != 0){
				logger.entry(deviceType, deviceSubtype);

				new SwingWorker<Void, Void>() {

					@Override
					protected Void doInBackground() throws Exception {
						List<String> productDescription = Database.getProductDescriptions(deviceType, deviceSubtype);
						logger.trace(productDescription);

						DefaultComboBoxModel<String> model = productDescription!=null ?
															new DefaultComboBoxModel<>(productDescription.toArray(new String[productDescription.size()])) :
															new DefaultComboBoxModel<String>();

						if(!comboBox.getModel().equals(model))
							comboBox.setModel(model);
						logger.exit("setComboBoxModel();");
						return null;
					}
					
				};
			}
		}

		private void setComboBoxSelectedItem(String partNumber) throws ClassNotFoundException, IOException, SQLException, InterruptedException {
			List<String> productPartNumbers = Database.getDescriptions(partNumber);
			logger.entry(partNumber, productPartNumbers);
			Object selectedItem = comboBox.getSelectedItem();
			if (productPartNumbers != null && !productPartNumbers.contains(selectedItem)) {
					comboBox.removeItemListener(itemListener);
					setValue(selectedItem = productPartNumbers.get(0));
					comboBox.addItemListener(itemListener);
				}
			logger.exit(selectedItem);
		}
	}
}
