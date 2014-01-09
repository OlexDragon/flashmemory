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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;


public class SerialNumberPanel extends EditTextFieldPanel {
	private static final long serialVersionUID = -724267452308108357L;

	public SerialNumberPanel() throws ClassNotFoundException, SQLException, IOException {
		super("Serial Number", ProfileProperties.SERIAL_NUMBER);
		
		JPopupMenu popupMenu = new JPopupMenu();
		addPopup(textField, popupMenu);
		
		JMenuItem mntmNewSerialnumber = new JMenuItem("Generate New Serial Number");
		mntmNewSerialnumber.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new SwingWorker<Void, Void>() {

					@Override
					protected Void doInBackground() throws Exception {
						Calendar calendar = Calendar.getInstance();
						DateFormat dateFormat = new SimpleDateFormat("YYww");
						String nextSerialNumber = dateFormat.format(calendar.getTime());
						try {
							nextSerialNumber = Database.getInstance().getNextSerialNumber(nextSerialNumber);
						} catch (ClassNotFoundException | SQLException | IOException e1) {
							logger.catching(e1);
						}
						action(nextSerialNumber);
						return null;
					}
				}.execute();
		}
		});
		popupMenu.add(mntmNewSerialnumber);
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
