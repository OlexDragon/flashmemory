package irt.flash.presentation.panel.edit_profile;
import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;
import irt.flash.presentation.panel.edit_profile.extendable.EditTextFieldPanel;

import java.io.IOException;
import java.sql.SQLException;

public class DeviceRevisionPanel extends EditTextFieldPanel {
	private static final long serialVersionUID = -1234658465043904517L;

	public DeviceRevisionPanel() throws ClassNotFoundException, SQLException, IOException {
		super("Device Revision", ProfileProperties.DEVICE_REVISION);
	}
}
