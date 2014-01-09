package irt.flash.presentation.panel.edit_profile;
import irt.flash.data.Informer;
import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;
import irt.flash.data.connection.dao.Database;
import irt.flash.presentation.panel.edit_profile.extendable.EditTextFieldPanel;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.SwingWorker;

public class SystemNamePanel extends EditTextFieldPanel implements Observer{
	private static final long serialVersionUID = -5617821531967992312L;

	public SystemNamePanel() throws ClassNotFoundException, SQLException, IOException {
		super("System Name", ProfileProperties.SYSTEM_NAME);
	}

	@Override
	public void update(Observable observable, Object obj) {
		logger.trace("{}, {}", observable, obj);

		if(observable instanceof Informer)
			new UpdateWorker((Informer)observable).execute();
	}

	//****************************************************************************************
	private class UpdateWorker extends SwingWorker<Void, Void>{

		private Informer informer;

		public UpdateWorker(Informer informer) {
			this.informer = informer;
		}

		@Override
		protected Void doInBackground() throws Exception {
			logger.trace(informer);
				try {
					List<String> systemNames = null;
					switch(informer.getName()){
					case "part number":
						systemNames = Database.getSystemNameByPartNumber((String)informer.getValue());
						break;
					case "description":
						systemNames = Database.getSystemNameByDescription((String)informer.getValue());
					}
					setValue(systemNames!=null ? systemNames.get(0) : null);
				} catch (Exception e) {
					logger.catching(e);
				}
			return null;
		}
		
	}
}
