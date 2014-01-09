package irt.flash.presentation.panel.edit_profile;

import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;
import irt.flash.presentation.panel.edit_profile.extendable.EditTablePanel;
import irt.flash.presentation.panel.edit_profile.table.PowerTableModel;
import irt.flash.presentation.panel.edit_profile.table.TableModelInterface;

import java.io.IOException;
import java.sql.SQLException;

public class TablePowerPanel extends EditTablePanel {
	private static final long serialVersionUID = -2573984522025656905L;

	public TablePowerPanel() throws ClassNotFoundException, SQLException, IOException {
		super("Power Table", ProfileProperties.POWER_LUT_ENTRY);
	}

	@Override
	protected TableModelInterface getTableModel() {
		return (TableModelInterface) new PowerTableModel();
	}
}
