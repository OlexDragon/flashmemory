package irt.flash.presentation.panel.edit_profile.extendable;

import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.IOException;
import java.sql.SQLException;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

public abstract class EditTextFieldPanel extends EditPanel<String> {
	private static final long serialVersionUID = 5558092895694728856L;

	protected JTextField textField;

	public EditTextFieldPanel(String title, ProfileProperties profileProperties) throws ClassNotFoundException, SQLException, IOException {
		super(title, profileProperties);

		textField = new JTextField();
		textField.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				action(textField.getText());
			}
		});
		textField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				action(textField.getText());
			}
		});
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

	protected void action(String text) {
		if(oldValues.size()==0)
			oldValues.add("");
		setValue(text);
	}

	@Override
	public void setValue(final Object value) {
		new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {

				logger.trace(value);
				String text = textField.getText();

				String str = (String) value;

				if(oldValues.add(str))
					textField.setToolTipText(oldValues.toString());

				setBackground(str);

				if(text!=null ? !text.equals(value) : !(value==null || str.isEmpty()))
					textField.setText(str);

				return null;
			}
		}.execute();
	}
}
