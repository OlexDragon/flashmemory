package irt.flash.presentation.panel.edit_profile;

import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;
import irt.flash.presentation.panel.edit_profile.extendable.EditTablePanel;
import irt.flash.presentation.panel.edit_profile.table.DeviceThresholdCurrentModel;
import irt.flash.presentation.panel.edit_profile.table.TableModelInterface;

import java.io.IOException;
import java.sql.SQLException;

public class TableDeviceThresholdCurrentPanel extends EditTablePanel{
	private static final long serialVersionUID = 7804792470861561921L;

	public TableDeviceThresholdCurrentPanel() throws ClassNotFoundException, SQLException, IOException {
		super("Device Threshold Current", ProfileProperties.DEVICE_THRESHOLD_CURRENT);
	}

	@Override
	protected TableModelInterface getTableModel() {
		return (TableModelInterface) new DeviceThresholdCurrentModel();
	}
}
