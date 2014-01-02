package irt.flash.presentation.panel.edit_profile;
import irt.flash.data.DeviceType;
import irt.flash.data.Informer;
import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;
import irt.flash.data.connection.dao.Database;

import java.awt.Font;
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

import java.awt.event.ItemEvent;

public class DevicePartNumberPanel extends EditPanel implements Observer{
	private static final long serialVersionUID = -6807620162853088371L;

	private static final Logger logger = (Logger) LogManager.getLogger();

	private Informer informer = new Informer("part number");

	private JComboBox<String> comboBox;

	private int type;
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

	public DevicePartNumberPanel() throws ClassNotFoundException, SQLException, IOException {
		super("Device Part Number", ProfileProperties.DEVICE_PART_NUMBER);
		
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

	private void setComboBoxSelectedItem(final String description) throws ClassNotFoundException, SQLException, IOException {
		logger.trace(description);
		new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				List<String> productPartNumbers = Database.getPartNumbers(description);
				if(productPartNumbers!=null && !productPartNumbers.contains(comboBox.getSelectedItem())){
					comboBox.removeItemListener(itemListener);
					comboBox.setSelectedItem(productPartNumbers.get(0));
					comboBox.addItemListener(itemListener);
				}
				return null;
			}
		}.execute();
	}

	private void setComboBoxModel() throws ClassNotFoundException, SQLException, IOException {
		new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				List<String> productDescription = Database.getPartNumbers(type, subtype);
				DefaultComboBoxModel<String> model = productDescription!=null ?
													new DefaultComboBoxModel<>(productDescription.toArray(new String[productDescription.size()])) :
													new DefaultComboBoxModel<String>();
				comboBox.setModel(model);
				informer.setValue(comboBox.getSelectedItem());
				return null;
			}
		}.execute();
	}

	public void addObserver(Observer observer) {
		informer.addObserver(observer);
	}

	public void setDevicePartNumber(final String devicePartNumber) {
		new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				comboBox.setSelectedItem(devicePartNumber);
				return null;
			}
		}.execute();;
	}

	//*****************************************************************************************
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
					DeviceType value = (DeviceType) informer.getValue();
					type = value!=null ? value.getDeviceType() : 0;
					setComboBoxModel();
					break;
				case "subtype":
					value = (DeviceType) informer.getValue();
					subtype = value!=null ? value.getDeviceType() : 0;
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
		
	}
}
