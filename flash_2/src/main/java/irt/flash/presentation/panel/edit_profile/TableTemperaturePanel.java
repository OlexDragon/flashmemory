package irt.flash.presentation.panel.edit_profile;

import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;
import irt.flash.presentation.panel.edit_profile.extendable.EditTablePanel;
import irt.flash.presentation.panel.edit_profile.table.TableModelInterface;
import irt.flash.presentation.panel.edit_profile.table.TemperatureTableModel;

import java.io.IOException;
import java.sql.SQLException;

public class TableTemperaturePanel extends EditTablePanel {
	private static final long serialVersionUID = -2573984522025656905L;

	public TableTemperaturePanel() throws ClassNotFoundException, SQLException, IOException {
		super("Temperature Table", ProfileProperties.TEMPERATURE_LUT_ENTRY);
	}

	@Override
	protected TableModelInterface getTableModel() {
		return (TableModelInterface) new TemperatureTableModel();
	}
}
