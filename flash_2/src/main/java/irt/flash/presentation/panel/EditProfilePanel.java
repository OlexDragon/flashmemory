package irt.flash.presentation.panel;

import irt.flash.data.Profile;
import irt.flash.data.Table;
import irt.flash.data.UnitType;
import irt.flash.data.connection.dao.Database;
import irt.flash.presentation.panel.edit_profile.DevicePartNumberPanel;
import irt.flash.presentation.panel.edit_profile.DeviceRevisionPanel;
import irt.flash.presentation.panel.edit_profile.DeviceSubtypePanel;
import irt.flash.presentation.panel.edit_profile.DeviceTypePanel;
import irt.flash.presentation.panel.edit_profile.FrequencySetPanel;
import irt.flash.presentation.panel.edit_profile.MacAddressPanel;
import irt.flash.presentation.panel.edit_profile.PowerDetectorSourcePanel;
import irt.flash.presentation.panel.edit_profile.ProductDescriptionPanel;
import irt.flash.presentation.panel.edit_profile.SerialNumberPanel;
import irt.flash.presentation.panel.edit_profile.SystemNamePanel;
import irt.flash.presentation.panel.edit_profile.TableDeviceThresholdCurrentPanel;
import irt.flash.presentation.panel.edit_profile.TableDeviceThresholdTemperatyrePanel;
import irt.flash.presentation.panel.edit_profile.TablePowerPanel;
import irt.flash.presentation.panel.edit_profile.TableTemperaturePanel;
import irt.flash.presentation.panel.edit_profile.extendable.EditPanel;
import irt.flash.presentation.panel.edit_profile.extendable.EditTablePanel;
import irt.flash.presentation.panel.edit_profile.extendable.ScrollablePanel;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class EditProfilePanel extends JScrollPane implements Observer {
	private static final long serialVersionUID = 1L;

	private static final Logger logger = (Logger) LogManager.getLogger();

	private List<EditPanel<?>> panels = new ArrayList<>();
	private List<EditTablePanel> tablePanels = new ArrayList<>();
	private DeviceTypePanel deviceTypePanel;

	public EditProfilePanel() throws Exception {
		logger.info("* Start *");

		deviceTypePanel = new DeviceTypePanel();
		ProductDescriptionPanel productDescriptionPanel = new ProductDescriptionPanel();
		DevicePartNumberPanel devicePartNumberPanel = new DevicePartNumberPanel();
		DeviceSubtypePanel deviceSubtypePanel = new DeviceSubtypePanel();

		deviceTypePanel.addObserver(productDescriptionPanel);
		deviceSubtypePanel.addObserver(productDescriptionPanel);
		devicePartNumberPanel.addObserver(productDescriptionPanel);
		deviceSubtypePanel.addObserver(devicePartNumberPanel);
		deviceTypePanel.addObserver(devicePartNumberPanel);
		productDescriptionPanel.addObserver(devicePartNumberPanel);

		JPanel panel = new ScrollablePanel();
		setViewportView(panel);

		panel.add(deviceTypePanel);
		panels.add(deviceTypePanel);

		panel.add(deviceSubtypePanel);
		panels.add(deviceSubtypePanel);

		SerialNumberPanel serialNumberPanel = new SerialNumberPanel();
		panel.add(serialNumberPanel);
		panels.add(serialNumberPanel);

		SystemNamePanel systemNamePanel = new SystemNamePanel();
		panel.add(systemNamePanel);
		panels.add(systemNamePanel);

		panel.add(devicePartNumberPanel);
		panels.add(devicePartNumberPanel);

		DeviceRevisionPanel deviceRevisionPanel = new DeviceRevisionPanel();
		panel.add(deviceRevisionPanel);
		panels.add(deviceRevisionPanel);

		MacAddressPanel macAddressPanel = new MacAddressPanel();
		panel.add(macAddressPanel);
		panels.add(macAddressPanel);
		
		panel.add(productDescriptionPanel);
		panels.add(productDescriptionPanel);
		
		PowerDetectorSourcePanel powerDetectorSourcePanel = new PowerDetectorSourcePanel();
		panel.add(powerDetectorSourcePanel);
		panels.add(powerDetectorSourcePanel);
		
		FrequencySetPanel frequencySetPanel = new FrequencySetPanel();
		panel.add(frequencySetPanel);
		panels.add(frequencySetPanel);
		
		TableDeviceThresholdTemperatyrePanel tableDeviceThresholdTemperatyrePanel = new TableDeviceThresholdTemperatyrePanel();
		panel.add(tableDeviceThresholdTemperatyrePanel);
		tablePanels.add(tableDeviceThresholdTemperatyrePanel);
		
		TableDeviceThresholdCurrentPanel tableDeviceThresholdCurrentPanel = new TableDeviceThresholdCurrentPanel();
		panel.add(tableDeviceThresholdCurrentPanel);
		panels.add(tableDeviceThresholdCurrentPanel);
		tablePanels.add(tableDeviceThresholdCurrentPanel);
		
		TableTemperaturePanel tableTemperaturePanel = new TableTemperaturePanel();
		panel.add(tableTemperaturePanel);
		tablePanels.add(tableTemperaturePanel);
		
		TablePowerPanel tablePowerPanel = new TablePowerPanel();
		panel.add(tablePowerPanel);
		tablePanels.add(tablePowerPanel);
	}

	public void resetProfileVariables() {
		for (EditPanel<?> ep : panels)
			ep.reset();
	}

	@Override
	public void update(Observable o, Object obj) {
		logger.trace("{}, {}", o, obj);
		new UpdateWorker(obj).execute();
	}

	public void setUnitType(String unitType) throws Exception {
		deviceTypePanel.setUnitType(unitType);
		new UnitTypeWorker(unitType).execute();
	}

	// ********************************************************************************************
	private class UpdateWorker extends SwingWorker<Observable, Object> {

		private Object object;

		public UpdateWorker(Object object) {
			this.object = object;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected Observable doInBackground() throws Exception {

			if (object instanceof Profile) {
				Profile p = (Profile) object;

				for (EditPanel<?> ep : panels) {
					String property = ep.getProfileProperties().toString();
					String value = p.getProperty(property);
					logger.debug("property={}, value={}", property, value);
					((EditPanel<String>) ep).setValue(value);
				}

				for (EditTablePanel etp : tablePanels) {
					String property = etp.getProfileProperties().toString();
					Table value = p.getTable(property);
					etp.setValue(value);
				}
			}
			return null;
		}
	}

	// ************************************************************************************
	private class UnitTypeWorker extends SwingWorker<UnitType, String> {

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

				if(unitType!=null)
				for (EditPanel<?> p : panels) {

					switch (p.getScope()) {
					case COMMON:
						break;
					case BUC:
						switch (unitType.getId()) {
						case 1:
							p.setVisible(false);
							break;
						default:
							if (!p.isVisible())
								p.setVisible(true);
						}
						break;
					case FCM:
						switch (unitType.getId()) {
						case 2:
							p.setVisible(false);
							break;
						default:
							if (!p.isVisible())
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
