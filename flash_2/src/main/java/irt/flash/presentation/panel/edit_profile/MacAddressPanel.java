package irt.flash.presentation.panel.edit_profile;

import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;
import irt.flash.data.connection.dao.Database;
import irt.flash.presentation.panel.edit_profile.extendable.EditTextFieldPanel;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.sql.SQLException;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class MacAddressPanel extends EditTextFieldPanel {
	private static final long serialVersionUID = 7165890958430180057L;

	public MacAddressPanel() throws ClassNotFoundException, SQLException, IOException {
		super("Mac Address", ProfileProperties.MAC_ADDRESS);
		
		JPopupMenu popupMenu = new JPopupMenu();
		addPopup(textField, popupMenu);
		
		JMenuItem mntmNewMacAddress = new JMenuItem("Generate New Mac Address");
		mntmNewMacAddress.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					action(Database.getInstance().getNextMacAddress());
				} catch (Exception e1) {
					logger.catching(e1);
				}
			}
		});
		popupMenu.add(mntmNewMacAddress);
	}
	private static void addPopup(Component component, final JPopupMenu popup) {
		component.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showMenu(e);
				}
			}
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showMenu(e);
				}
			}
			private void showMenu(MouseEvent e) {
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		});
	}
}
