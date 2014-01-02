package irt.flash.presentation.panel;

import irt.flash.data.Profile;
import irt.flash.data.UnitType;
import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;
import irt.flash.data.connection.dao.Database;
import irt.flash.presentation.panel.edit_profile.DevicePartNumberPanel;
import irt.flash.presentation.panel.edit_profile.DeviceRevisionPanel;
import irt.flash.presentation.panel.edit_profile.DeviceSubtypePanel;
import irt.flash.presentation.panel.edit_profile.DeviceTypePanel;
import irt.flash.presentation.panel.edit_profile.EditPanel;
import irt.flash.presentation.panel.edit_profile.FrequencySetPanel;
import irt.flash.presentation.panel.edit_profile.MacAddressPanel;
import irt.flash.presentation.panel.edit_profile.PowerDetectorSourcePanel;
import irt.flash.presentation.panel.edit_profile.ProductDescriptionPanel;
import irt.flash.presentation.panel.edit_profile.SerialNumberPanel;
import irt.flash.presentation.panel.edit_profile.SystemNamePanel;

import java.awt.FlowLayout;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JPanel;
import javax.swing.SwingWorker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class EditProfilePanel extends JPanel implements Observer{
	private static final long serialVersionUID = 1L;

	private static final Logger logger = (Logger) LogManager.getLogger();

	private List<EditPanel> panels = new ArrayList<>();
	private SerialNumberPanel serialNumber;
	private DeviceTypePanel deviceType;
	private MacAddressPanel macAddress;
	private ProductDescriptionPanel productDescription;
	private DevicePartNumberPanel devicePartNumber;
	private DeviceSubtypePanel deviceSubtype;
	private DeviceRevisionPanel deviceRevision;
	private PowerDetectorSourcePanel powerDetectorSource;
	private SystemNamePanel systemNamePanel;
	private FrequencySetPanel frequencySetPanel;

	public EditProfilePanel() throws ClassNotFoundException, SQLException, IOException {
		setLayout(new FlowLayout(FlowLayout.LEADING, 5, 5));
		
		devicePartNumber = new DevicePartNumberPanel();
		panels.add(devicePartNumber);
		productDescription = new ProductDescriptionPanel();
		panels.add(productDescription);
		productDescription.addObserver(devicePartNumber);
		devicePartNumber.addObserver(productDescription);

		deviceType = new DeviceTypePanel();
		panels.add(deviceType);
		deviceType.addObserver(productDescription);
		deviceType.addObserver(devicePartNumber);
		add(deviceType);

		deviceSubtype = new DeviceSubtypePanel();
		panels.add(deviceSubtype);
		deviceSubtype.addObserver(productDescription);
		deviceSubtype.addObserver(devicePartNumber);
		add(deviceSubtype);
		
		deviceRevision = new DeviceRevisionPanel();
		panels.add(deviceRevision);
		add(deviceRevision);
		
		systemNamePanel = new SystemNamePanel();
		panels.add(systemNamePanel);
		devicePartNumber.addObserver(systemNamePanel);
		productDescription.addObserver(systemNamePanel);
		add(systemNamePanel);
		
		add(productDescription);
		
		serialNumber = new SerialNumberPanel();
		panels.add(serialNumber);
		add(serialNumber);
		
		add(devicePartNumber);
		
		macAddress = new MacAddressPanel();
		panels.add(macAddress);
		add(macAddress);
		
		powerDetectorSource = new PowerDetectorSourcePanel();
		add(powerDetectorSource);
		
		frequencySetPanel = new FrequencySetPanel();
		panels.add(frequencySetPanel);
		add(frequencySetPanel);

	}

	@Override
	public void update(Observable o, Object obj) {
		logger.trace("{}, {}",o, obj);
		new UpdateWorker(obj).execute();
	}

	public void setUnitType(String unitType) throws ClassNotFoundException, SQLException, IOException {
		deviceType.setUnitType(unitType);
		new UnitTypeWorker(unitType).execute();;
	}

	//********************************************************************************************
	private class UpdateWorker extends SwingWorker<Observable, Object>{

		private Object object;

		public UpdateWorker(Object object) {
			this.object = object;
		}

		@Override
		protected Observable doInBackground() throws Exception {			

			if(object instanceof Profile){
				Profile p = (Profile) object;

				String property = p.getProperty(ProfileProperties.SERIAL_NUMBER.toString());
				serialNumber.setSerialNumber	(property);

				deviceType.setDeviceType			(p.getProperty(ProfileProperties.DEVICE_TYPE.toString()));
				deviceRevision.setDeviceRevision	(p.getProperty(ProfileProperties.DEVICE_REVISION.toString()));
				deviceSubtype.setDeviceSubtype		(p.getProperty(ProfileProperties.DEVICE_SUBTYPE.toString()));
				devicePartNumber.setDevicePartNumber(p.getProperty(ProfileProperties.DEVICE_PART_NUMBER.toString()));
				productDescription.setDescription	(p.getProperty(ProfileProperties.PRODUCT_DESCRIPTION.toString()));
				macAddress.setMacAddress			(p.getProperty(ProfileProperties.MAC_ADDRESS.toString()));
				frequencySetPanel.setFrequencySet	(p.getProperty(ProfileProperties.FREQUENCY_SET.toString()));
			}
			return null;
		}
	}

	//************************************************************************************
	private class UnitTypeWorker extends SwingWorker<UnitType, String>{

		private final Logger logger = (Logger) LogManager.getLogger();

		private String unitTypeStr;

		public UnitTypeWorker(String unitTypeStr) {
			this.unitTypeStr = unitTypeStr;
		}

		@Override
		protected UnitType doInBackground() throws Exception {
			return Database.getUnitType(unitTypeStr);
		}

		@Override
		protected void done() {
			try {
				UnitType unitType = get();

				for(EditPanel p:panels){

					switch(p.getScope()){
					case COMMON:
						break;
					case BUC:
						switch(unitType.getId()){
						case 1:
							p.setVisible(false);
							break;
						default:
							if(!p.isVisible())
								p.setVisible(true);
						}
						break;
					case FCM:
						switch(unitType.getId()){
						case 2:
							p.setVisible(false);
							break;
						default:
							if(!p.isVisible())
								p.setVisible(true);
						}
					}
				}
				
			} catch (Exception e) {
				logger.catching(e);
			}
		}
	}
}
