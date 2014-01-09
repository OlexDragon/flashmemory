package irt.flash.presentation.panel.edit_profile.extendable;

import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;

import java.awt.Font;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JComboBox;
import javax.swing.SwingWorker;

public abstract class EditComboBoxPanel<T> extends EditPanel<T> {
	private static final long serialVersionUID = 4560512863663869360L;

	protected JComboBox<T> comboBox;

	public EditComboBoxPanel(String title, ProfileProperties profileProperties) throws Exception {
		super(title, profileProperties);
		
		comboBox = new JComboBox<>();
		comboBox.setFont(new Font("Tahoma", Font.BOLD, 16));
		setComboBoxModel();

		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addComponent(comboBox, 110, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addComponent(comboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
		);
		setLayout(groupLayout);
	}

	@Override
	public void setValue(final Object value) {
		new SwingWorker<Void, Void>() {

			@SuppressWarnings("unchecked")
			@Override
			protected Void doInBackground() throws Exception {
				logger.trace(value);
				T v = (T) value;
				oldValues.add(v);
				comboBox.setSelectedItem(v);
				setBackground(v);
				return null;
			}
		}.execute();
	}

	protected abstract void setComboBoxModel() throws Exception;
}
