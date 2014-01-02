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
import javax.swing.SwingWorker;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JComboBox;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;


public class ProductDescriptionPanel extends EditPanel implements Observer{
	private static final long serialVersionUID = 7558824145840843702L;

	private static final Logger logger = (Logger) LogManager.getLogger();

	private Informer informer = new Informer("description");

	private JComboBox<String> comboBox;

	private int deviceType;

	private int subtype;

	private ItemListener itemListener = new ItemListener() {
		public void itemStateChanged(ItemEvent itemEvent) {
			if(itemEvent.getStateChange()==ItemEvent.SELECTED)
				new SwingWorker<Void, Void>() {

					@Override
					protected Void doInBackground() throws Exception {
						informer.setValue(comboBox.getSelectedItem());
						return null;
					}
				};
		}
	};

	public ProductDescriptionPanel() throws IOException, ClassNotFoundException, SQLException {
		super("Product Description", ProfileProperties.PRODUCT_DESCRIPTION);

		comboBox = new JComboBox<>();
		comboBox.addItemListener(itemListener);
		comboBox.setEditable(true);
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

	@Override
	public void update(Observable informer, Object o) {
		logger.trace("{}, {}", informer, o);

		if(informer instanceof Informer)
			new UpdateWorker((Informer)informer).execute();
	}

	public void addObserver(Observer observer) {
		informer.addObserver(observer);
	}

	public void setDescription(String description) {
		comboBox.setSelectedItem(description);
	}

	//****************************************************************************************
	private class UpdateWorker extends SwingWorker<Observable, Object>{

		private Informer informer;

		public UpdateWorker(Informer informer) {
			this.informer = informer;
		}

		@Override
		protected Observable doInBackground() throws Exception {
			try {
				switch(informer.getName()){
				case "type":
					DeviceType value = (DeviceType) informer.getValue();
					deviceType = value!=null ? value.getDeviceType() : 0;
					setComboBoxModel();
					break;
				case "subtype":
					value = (DeviceType) informer.getValue();
					subtype = value!=null ? value.getDeviceType() : 0;
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

		private void setComboBoxModel() throws IOException, ClassNotFoundException, SQLException {
			List<String> productDescription = Database.getProductDescriptions(deviceType, subtype);
			DefaultComboBoxModel<String> model = productDescription!=null ?
												new DefaultComboBoxModel<>(productDescription.toArray(new String[productDescription.size()])) :
												new DefaultComboBoxModel<String>();
			comboBox.setModel(model);
		}

		private void setComboBoxSelectedItem(String partNumber) throws ClassNotFoundException, IOException, SQLException {
			logger.entry(partNumber);
			List<String> productPartNumbers = Database.getDescriptions(partNumber);
			if(productPartNumbers!=null && !productPartNumbers.contains(comboBox.getSelectedItem())){
				comboBox.removeItemListener(itemListener);
				comboBox.setSelectedItem(productPartNumbers.get(0));
				comboBox.addItemListener(itemListener);
			}
		}
	}
}
