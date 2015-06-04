package irt.flash.presentation.panel;

import irt.flash.data.connection.dao.Database;
import irt.flash.presentation.panel.edit_profile.extendable.EditPanel;
import irt.flash.presentation.panel.edit_profile.extendable.EditTablePanel;
import irt.flash.presentation.withard.ProfileWithard;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class ProfileWorkerPanel extends JPanel implements Observer {
	private static final long serialVersionUID = 863089977016844508L;

	private static final Logger logger = (Logger) LogManager.getLogger();

	private EditProfilePanel editProfilePanel;

	public ProfileWorkerPanel(final Window owner) throws Exception {

		JPanel buttonsPanel = new JPanel();
		setLayout(new BorderLayout(0, 0));
		add(buttonsPanel, BorderLayout.NORTH);
		
		JButton btnGetProfile = new JButton("Get Profile");
		btnGetProfile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new SwingWorker<Void, Void>() {
					@Override
					protected Void doInBackground() throws Exception {
						String profileStr = null;
						try{
							profileStr = Database.getProfileHeader(editProfilePanel.getDeviceType());
						}catch(Exception e){
							logger.catching(e);
						}
						logger.trace("profileStr={}", profileStr);

						if(profileStr!=null){
							for(EditPanel<?> ep:editProfilePanel.getPanels()){
								Object value = ep.getValue();
								if(value!=null)
									profileStr += ep.getProfileProperties()+" "+value+"\n";
							}
							logger.error(profileStr);
							List<EditTablePanel> tablePanels = editProfilePanel.getTablePanels();
						}
						return null;
					}
				}.execute();
			}
		});
		
		JButton btnRunWithard = new JButton("Run Profile Wizard");
		btnRunWithard.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				logger.entry();
				new ProfileWithard(owner).setVisible(true);
				logger.exit();
			}
		});
		buttonsPanel.add(btnRunWithard);
		buttonsPanel.add(btnGetProfile);

		editProfilePanel = new EditProfilePanel();
		add(editProfilePanel, BorderLayout.CENTER);
	}
	public void setUnitType(String unitType) throws Exception {
		editProfilePanel.setUnitType(unitType);
	}
	public void resetProfileVariables() {
		editProfilePanel.resetProfileVariables();
	}
	@Override
	public void update(Observable o, Object obj) {
		editProfilePanel.update(o, obj);
	}
}
