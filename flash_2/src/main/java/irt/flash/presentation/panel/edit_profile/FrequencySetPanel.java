package irt.flash.presentation.panel.edit_profile;
import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;
import irt.flash.data.value.ValueFrequency;

import java.awt.Font;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class FrequencySetPanel extends EditPanel {
	private static final long serialVersionUID = -713461044198711633L;

	private static final Logger logger = (Logger) LogManager.getLogger();

	private JList<Object> list;

	public FrequencySetPanel() throws ClassNotFoundException, SQLException, IOException {
		super("Frequency Set", ProfileProperties.FREQUENCY_SET);
		
		list = new JList<>();
		list.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		list.setFont(new Font("Tahoma", Font.BOLD, 16));

		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addComponent(list, 110, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addComponent(list, 18, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
		);
		setLayout(groupLayout);
	}

	public void setFrequencySet(String frequencies) {
		logger.trace(frequencies);
		if(frequencies!=null){
			String[] split = frequencies.split(" ");
			ValueFrequency[] vFrequencies = new ValueFrequency[split.length];
			for(int i=0; i<split.length; i++){
				vFrequencies[i] = new ValueFrequency(split[i], "1MHz", "20GHz");
			}
			Arrays.sort(vFrequencies);
			for(ValueFrequency vf:vFrequencies)
				if(!vf.isError())
					list.add(new JTextField(vf.toString()));
		}else
			list.removeAll();
	}
}
