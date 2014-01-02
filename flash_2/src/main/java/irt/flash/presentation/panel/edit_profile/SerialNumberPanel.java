package irt.flash.presentation.panel.edit_profile;

import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;
import irt.flash.data.connection.dao.Database;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;


public class SerialNumberPanel extends EditPanel {
	private static final long serialVersionUID = -724267452308108357L;

	private static final Logger logger = (Logger) LogManager.getLogger();

	private JTextField textField;

	public SerialNumberPanel() throws ClassNotFoundException, SQLException, IOException {
		super("Serial Number", ProfileProperties.SERIAL_NUMBER);

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
						textField.setText(nextSerialNumber);
									return null;
					}
				}.execute();
		}
		});
		popupMenu.add(mntmNewSerialnumber);
		setLayout(groupLayout);
		
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

	public void setSerialNumber(final String serialNumber) {
		new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				textField.setText(serialNumber!=null ? serialNumber : "");
				return null;
			}
		}.execute();
	}
}
