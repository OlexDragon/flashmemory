package irt.flash.presentation.panel.edit_profile;
import irt.flash.data.Informer;
import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;
import irt.flash.data.connection.dao.Database;

import java.awt.Font;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class SystemNamePanel extends EditPanel implements Observer{
	private static final long serialVersionUID = -5617821531967992312L;

	private static final Logger logger = (Logger) LogManager.getLogger();

	private JTextField textField;

	public SystemNamePanel() throws ClassNotFoundException, SQLException, IOException {
		super("System Name", ProfileProperties.SYSTEM_NAME);
		
		textField = new JTextField();
		textField.setFont(new Font("Tahoma", Font.BOLD, 16));
		textField.setColumns(10);

		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addComponent(textField, 110, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addComponent(textField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
		);
		setLayout(groupLayout);
	}

	@Override
	public void update(Observable observable, Object obj) {
		logger.trace("{}, {}", observable, obj);

		new UpdateWorker(observable).execute();
	}

	//****************************************************************************************
	private class UpdateWorker extends SwingWorker<Void, Void>{

		private Observable observable;

		public UpdateWorker(Observable observable) {
			this.observable = observable;
		}

		@Override
		protected Void doInBackground() throws Exception {
			if(observable instanceof Informer)
				try {
					Informer info = (Informer)observable;
					List<String> systemNames = null;
					switch(info.getName()){
					case "part number":
						systemNames = Database.getSystemNameByPartNumber((String)info.getValue());
						break;
					case "description":
						systemNames = Database.getSystemNameByDescription((String)info.getValue());
					}
					textField.setText(systemNames!=null ? systemNames.get(0) : "");
					logger.exit(systemNames);
				} catch (Exception e) {
					logger.catching(e);
				}
			return null;
		}
		
	}
}
