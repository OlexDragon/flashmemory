package irt.flash.presentation.panel.edit_profile.extendable;

import irt.flash.data.Table;
import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;
import irt.flash.presentation.panel.edit_profile.table.TableModelInterface;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.sql.SQLException;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;

public abstract class EditTablePanel extends EditPanel<String> {
	private static final long serialVersionUID = -5833224115643726296L;

	protected JTable table;
	protected TableModelInterface tableModel;

	public EditTablePanel(String title, ProfileProperties profileProperties) throws ClassNotFoundException, SQLException, IOException {
		super(title, profileProperties);

		
		JScrollPane scrollPane = new JScrollPane();
		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addComponent(scrollPane, 110, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addComponent(scrollPane, 14, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
		);

		tableModel = getTableModel();

		table = new JTable(tableModel);
		scrollPane.setViewportView(table);
		table.setFont(new Font("Tahoma", Font.BOLD, 16));
		
		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent keyEvent) {
				switch(keyEvent.getKeyCode()){
				case KeyEvent.VK_ESCAPE:
					tableModel.escape();
				}
			}
		});

		JPopupMenu popupMenu = new JPopupMenu();
		addPopup(table, popupMenu);
		
		JMenuItem mntmIncert = new JMenuItem("Insert Row");
		mntmIncert.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int selectedRow = table.getSelectedRow();
				tableModel.insertRow(selectedRow);
				table.setRowSelectionInterval(selectedRow, selectedRow);
			}
		});
		popupMenu.add(mntmIncert);
		
		JMenuItem mntmDeleteRow = new JMenuItem("Delete Row");
		mntmDeleteRow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tableModel.deleteRow(table.getSelectedRow());
			}
		});
		popupMenu.add(mntmDeleteRow);
		setLayout(groupLayout);
	}

	@Override
	public void setValue(Object value) {
		if(value instanceof Table)
			tableModel.setTable((Table)value);
	}

	private void addPopup(Component component, final JPopupMenu popup) {
		component.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {

				//Select Row whith right click
				int row = table.rowAtPoint(e.getPoint());
		        if (row >= 0 && row < table.getRowCount()) {
		            table.setRowSelectionInterval(row, row);
		        } else {
		            table.clearSelection();
		        }

		        //double click to edit row
				if(e.getClickCount()>1)
					tableModel.setEditableRow(row);

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

	protected abstract TableModelInterface getTableModel();
}
