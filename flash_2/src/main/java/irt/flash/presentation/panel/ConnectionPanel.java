package irt.flash.presentation.panel;

import irt.flash.data.Profile;
import irt.flash.data.ToHex;
import irt.flash.data.connection.FlashConnector;
import irt.flash.data.connection.MicrocontrollerSTM32;
import irt.flash.data.connection.MicrocontrollerSTM32.Address;
import irt.flash.data.connection.MicrocontrollerSTM32.Answer;
import irt.flash.data.connection.MicrocontrollerSTM32.Status;
import irt.flash.data.connection.dao.DatabaseController;
import irt.flash.presentation.dialog.MessageDialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;
import java.util.Scanner;
import java.util.prefs.Preferences;

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import jssc.SerialPortList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class ConnectionPanel extends JPanel implements Observer {
	private static final long serialVersionUID = -79571598522363841L;

	private final Logger logger = (Logger) LogManager.getLogger();

	private static final String CAN_NOT_CONNECT = "Can Not Connect";
	private static final String DISCONNECT = "Disconnect";
	private static final String CONNECT = "Connect";
	private static final String PRESS_CONNECT_BUTTON = "Press Connect Button";
	public static final String SELECT_SERIAL_PORT = "Select Serial Port";
	public static final String SERIAL_PORT = "serialPort";

	protected static final Preferences prefs = Preferences.userRoot().node("IRT Technologies inc.");

	private JComboBox<String> comboBoxComPort;

	private JButton btnConnect;

	private JLabel lblConnection;

	private JComboBox<String> comboBoxUnitType;

	private JButton btnRead;

	private JLabel lblUnitType;

	private JTextPane textPane;
	private JPopupMenu popupMenu;
	private JMenuItem mntmUploadFromFile;
	private JMenuItem mntmUploadProgram;
	private JMenuItem mntmUploadProgramFrom;
	private JMenuItem mntmEraseProfileMemory;

	private JMenuItem mntmSaveProfileTo;
	private JMenuItem mntmCheckProgram;
	private JMenuItem mntmCheckProgram_1;

	private MessageDialog dialog;
	private volatile static byte[] buffer;
	private volatile static DatabaseController databaseController = new DatabaseController();

	public ConnectionPanel(Window owner) {
		dialog = MessageDialog.getInstance(owner);
		dialog.addButtonActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				String buttonText = dialog.getButtonText();
				logger.entry(buttonText);
				switch(buttonText){
				case "Ok":
					dialog.setMessage((Status)null);
					break;
				default:
					System.exit(0);
				}
			}
		});

		addAncestorListener(new AncestorListener() {
			public void ancestorAdded(AncestorEvent ancestorEvent) {
				logger.entry();
				DefaultComboBoxModel<String> defaultComboBoxModel = new DefaultComboBoxModel<String>(SerialPortList.getPortNames());
				defaultComboBoxModel.insertElementAt(SELECT_SERIAL_PORT, 0);
				comboBoxComPort.setModel(defaultComboBoxModel);
				comboBoxComPort.setSelectedItem(prefs.get(SERIAL_PORT, SELECT_SERIAL_PORT));
				comboBoxComPort.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(ItemEvent itemEvent) {
						if(itemEvent.getStateChange()==ItemEvent.SELECTED){
							disconnect();
							prefs.put(SERIAL_PORT, comboBoxComPort.getSelectedItem().toString());
							setButtons();
						}
					}
				});
				setButtons();
				logger.exit();
			}
			public void ancestorMoved(AncestorEvent ancestorEvent) { }
			public void ancestorRemoved(AncestorEvent ancestorEvent) { }
		});
		
		btnConnect = new JButton(CONNECT);
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				String text = btnConnect.getText();
				logger.entry(text);
				try {
					switch(text){
					case CONNECT:
						logger.trace(CONNECT);
						btnConnect.setText("Cancel");
						FlashConnector.connect();
						textPane.setText("");
						break;
					case DISCONNECT:
						logger.trace("disconnect");
						disconnect();
						setLabel(lblConnection, PRESS_CONNECT_BUTTON, Color.YELLOW);
						break;
					default:
						logger.trace("default");
					}
				} catch (Exception e) {
					logger.catching(e);
					JOptionPane.showMessageDialog(ConnectionPanel.this, e.getLocalizedMessage());
					disconnect();
					setLabel(lblConnection, PRESS_CONNECT_BUTTON, Color.YELLOW);
				}
				logger.exit();
			}
		});
		btnConnect.setFont(new Font("Tahoma", Font.BOLD, 14));
		btnConnect.setMargin(new Insets(0, 0, 0, 0));
		
		btnRead = new JButton("Read");
		btnRead.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					MicrocontrollerSTM32.read((String) comboBoxUnitType.getSelectedItem());
				} catch (InterruptedException e) {
					logger.catching(e);
				}
			}
		});
		btnRead.setMargin(new Insets(0, 0, 0, 0));
		btnRead.setFont(new Font("Tahoma", Font.BOLD, 14));

		comboBoxComPort = new JComboBox<>();
		comboBoxComPort.setFont(new Font("Tahoma", Font.BOLD, 14));
		
		lblConnection = new JLabel("Connection");
		lblConnection.setHorizontalAlignment(SwingConstants.CENTER);
		lblConnection.setOpaque(true);
		lblConnection.setFont(new Font("Tahoma", Font.BOLD, 14));

		DefaultComboBoxModel<String> defaultComboBoxModel = new DefaultComboBoxModel<String>();
		defaultComboBoxModel.addElement("Select Unit Type");
		for(Address v:Address.values())
			if(v!=Address.PROGRAM)
				defaultComboBoxModel.addElement(v.toString());
		comboBoxUnitType = new JComboBox<String>(defaultComboBoxModel);
		comboBoxUnitType.setSelectedItem(prefs.get("Unit Type", "Select Unit Type"));
		comboBoxUnitType.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent itemEvent) {
				if(itemEvent.getStateChange()==ItemEvent.SELECTED){
					prefs.put("Unit Type", comboBoxUnitType.getSelectedItem().toString());
					setButtons();
					boolean isSelectes = comboBoxUnitType.getSelectedIndex()>0;
					setLabel(lblUnitType, isSelectes ? "Unit Type" : "Select The Unit Type", isSelectes ? Color.GREEN : Color.YELLOW);
				}
			}
		});
		comboBoxUnitType.setFont(new Font("Tahoma", Font.BOLD, 14));
		
		boolean selectedIndex = comboBoxUnitType.getSelectedIndex()>0;
		lblUnitType = new JLabel(selectedIndex ? "Unit Type" : "Select The Unit Type");
		lblUnitType.setBackground(selectedIndex ? Color.GREEN : Color.YELLOW);
		lblUnitType.setOpaque(true);
		lblUnitType.setHorizontalAlignment(SwingConstants.CENTER);
		lblUnitType.setFont(new Font("Tahoma", Font.BOLD, 14));
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);

		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.TRAILING)
				.addGroup(Alignment.LEADING, groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE, 440, Short.MAX_VALUE)
							.addContainerGap())
						.addGroup(groupLayout.createSequentialGroup()
							.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
								.addComponent(lblUnitType, GroupLayout.DEFAULT_SIZE, 218, Short.MAX_VALUE)
								.addComponent(lblConnection, GroupLayout.DEFAULT_SIZE, 218, Short.MAX_VALUE))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
								.addGroup(groupLayout.createSequentialGroup()
									.addComponent(comboBoxComPort, GroupLayout.PREFERRED_SIZE, 117, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(btnConnect, GroupLayout.PREFERRED_SIZE, 83, GroupLayout.PREFERRED_SIZE))
								.addGroup(groupLayout.createSequentialGroup()
									.addComponent(comboBoxUnitType, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(btnRead, GroupLayout.PREFERRED_SIZE, 83, GroupLayout.PREFERRED_SIZE)))
							.addGap(10))))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(btnConnect, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE)
						.addComponent(comboBoxComPort, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblConnection))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(btnRead, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE)
						.addComponent(comboBoxUnitType, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblUnitType, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE, 213, Short.MAX_VALUE)
					.addContainerGap())
		);
		groupLayout.linkSize(SwingConstants.VERTICAL, new Component[] {btnConnect, comboBoxComPort, lblConnection});
		groupLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {comboBoxComPort, comboBoxUnitType});
		
		JScrollPane scrollPane = new JScrollPane();
		tabbedPane.addTab("Profile", null, scrollPane, null);
		
		textPane = new JTextPane();
		textPane.setEditable(false);
		scrollPane.setViewportView(textPane);
		
		popupMenu = new JPopupMenu();
		addPopup(textPane, popupMenu);
		
		mntmUploadFromFile = new JMenuItem("Upload Profile From File...");
		mntmUploadFromFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				if (isConnected()) {
					if(textPane.getText().isEmpty())
						dialog.setMessage("First Have to Read Profile.");
					else
					try {
						uploadFromFile();
					} catch (Exception e) {
						logger.catching(e);
					}
				}
			}

			private void uploadFromFile() throws FileNotFoundException, InterruptedException {

				JFileChooser fc = new JFileChooser();
				fc.setMultiSelectionEnabled(false);
				fc.setDialogTitle("Open Profile");

				FileNameExtensionFilter fileNameExtensionFilter = new FileNameExtensionFilter("Bin files(bin)", "bin");
				fc.addChoosableFileFilter(fileNameExtensionFilter);
				fc.setFileFilter(fileNameExtensionFilter);

				String pathStr = prefs.get("profilePath", null);
				Path p = null;
				if(pathStr!=null){
					p = Paths.get(pathStr);
					fc.setSelectedFile(p.toFile());
				}

				if(fc.showSaveDialog(ConnectionPanel.this)==JFileChooser.APPROVE_OPTION ){
					File file = fc.getSelectedFile();
					String path = file.getAbsolutePath();
					if(p==null || !path.equals(pathStr)) {
						prefs.put("profilePath", path);
					}

					String fileContents = new String();
					try(Scanner scanner = new Scanner(file)) {
						while(scanner.hasNextLine())
							fileContents += scanner.nextLine()+"\n";
					}
					databaseController.update(fileContents);

					if(!fileContents.isEmpty()){
						fileContents += '\0';
						MicrocontrollerSTM32.write((String) comboBoxUnitType.getSelectedItem(), fileContents);
					}
				}
			}
		});
		
		mntmEraseProfileMemory = new JMenuItem("Erase Profile Memory");
		mntmEraseProfileMemory.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				if(FlashConnector.isConnected()){
					if(btnRead.isEnabled()){
						try {
							dialog.setMessage(Status.ERASE.setMessage("Memory Erasing"));
							MicrocontrollerSTM32.erase((String) comboBoxUnitType.getSelectedItem());
						} catch (Exception e) {
							logger.catching(e);
						}
					}else
						JOptionPane.showMessageDialog(ConnectionPanel.this, "The Unit Type Is Not Selected.");
				}else
					JOptionPane.showMessageDialog(ConnectionPanel.this, "The Unit Is Not Connected.");
			}
		});
		popupMenu.add(mntmEraseProfileMemory);
		popupMenu.add(mntmUploadFromFile);
		
		mntmUploadProgram = new JMenuItem("Upload Program");
		mntmUploadProgram.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {

				if (isConnected()) {
					if(textPane.getText().isEmpty())
						dialog.setMessage("First Have to Read Profile.");
					else {
						String path = getProgramPath();

						logger.trace("path={}", path);

						File file = new File(path);
						if (file.exists()) {
							writeProgram(file);
						} else
							dialog.setMessage("The File do not exist.");
					}
				}
			}
		});
		
		mntmSaveProfileTo = new JMenuItem("Save Profile to File...");
		mntmSaveProfileTo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				if(!textPane.getText().isEmpty()){

					@SuppressWarnings("serial")
					JFileChooser fc = new JFileChooser(){
					    @Override
					    public void approveSelection(){
					        File f = getSelectedFile();
					        if(f.exists() && getDialogType() == SAVE_DIALOG){
					            switch(JOptionPane.showConfirmDialog(this,"The File '"+f.getName()+"' already exists.\nDo you want to replace it?", "Existing file", JOptionPane.YES_NO_OPTION)){
					                case JOptionPane.YES_OPTION:
					                    super.approveSelection();
					            }
					        }else
			                    super.approveSelection();
					    }
					};
					fc.setDialogTitle("Save Profile");
					fc.setMultiSelectionEnabled(false);

					FileNameExtensionFilter fileNameExtensionFilter = new FileNameExtensionFilter("Bin files(bin)", "bin");
					fc.addChoosableFileFilter(fileNameExtensionFilter);
					fc.setFileFilter(fileNameExtensionFilter);

					String pathStr = prefs.get("profile_path", null);
					if(pathStr!=null)
						fc.setSelectedFile(Paths.get(pathStr).getParent().resolve("profile.bin").toFile());

					if(fc.showSaveDialog(null)==JFileChooser.APPROVE_OPTION ){

						File selectedFile = fc.getSelectedFile();
						String absolutePath = selectedFile.getAbsolutePath();
						if(pathStr==null || !pathStr.equals(absolutePath))
							prefs.put("profile_path", absolutePath);

						try(BufferedWriter out = new BufferedWriter(new FileWriter(selectedFile))) {
							textPane.write(out);
							out.flush();
						} catch(IOException e) {
							logger.catching(e);
							dialog.setMessage("There was an error saving your file.");
}
					}
				}else
					dialog.setMessage("The Profile was not read from the Unit.");
			}
		});
		popupMenu.add(mntmSaveProfileTo);
		popupMenu.add(mntmUploadProgram);
		
		mntmUploadProgramFrom = new JMenuItem("Upload Program From File...");
		mntmUploadProgramFrom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				if (isConnected()) {
					if (textPane.getText().isEmpty())
						dialog.setMessage("First Have to Read Profile.");
					else {

						JFileChooser fc = new JFileChooser();
						fc.setDialogTitle("Open Program");
						fc.setMultiSelectionEnabled(false);
						String pathStr = prefs.get("programPath", null);

						Path p = null;
						if (pathStr != null) {
							p = Paths.get(pathStr);
							fc.setSelectedFile(p.toFile());
						}
						if (fc.showOpenDialog(ConnectionPanel.this) == JFileChooser.APPROVE_OPTION) {
							File file = fc.getSelectedFile();
							String absolutePath = file.getAbsolutePath();
							if (p == null || !absolutePath.equals(pathStr)) {
								prefs.put("programPath", absolutePath);
							}
							writeProgram(file);
						}
					}
				}
			}
		});
		
		mntmCheckProgram = new JMenuItem("Check Program");
		mntmCheckProgram.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {

				if (isConnected()) {
					String path = getProgramPath();

					logger.trace("path={}", path);

					File file = new File(path);
					if (file.exists()) {
						compare(file);
					} else
						dialog.setMessage("The File do not exist.");
				}
			}
		});
		popupMenu.add(mntmCheckProgram);
		popupMenu.add(mntmUploadProgramFrom);

		mntmCheckProgram_1 = new JMenuItem("Check Program with File...");
		mntmCheckProgram_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (isConnected()) {

					JFileChooser fc = new JFileChooser();
					fc.setDialogTitle("Open Program");
					fc.setMultiSelectionEnabled(false);
					String pathStr = prefs.get("programPath", null);

					Path p = null;
					if(pathStr!=null){
						p = Paths.get(pathStr);
						fc.setSelectedFile(p.toFile());
					}
					if (fc.showOpenDialog(ConnectionPanel.this) == JFileChooser.APPROVE_OPTION) {
						File file = fc.getSelectedFile();
						String absolutePath = file.getAbsolutePath();
						if(p==null || !absolutePath.equals(pathStr)) {
							prefs.put("programPath", absolutePath);
						}
						compare(file);
					}
				}
			}
		});
		popupMenu.add(mntmCheckProgram_1);
		setLayout(groupLayout);

		MicrocontrollerSTM32.getInstance().addObserver(this);
	}

	private void setConnected(byte[] bytes) {
		if(bytes!=null && bytes.length==1){
			boolean isConnected = bytes[0]==Answer.ACK.getAnswer();
			if(isConnected){
				setLabel(lblConnection, "Connected", Color.GREEN);
				btnConnect.setText(DISCONNECT);
				btnRead.setEnabled(comboBoxUnitType.getSelectedIndex()>0);
				dialog.setMessage((Status)null);
			}else{
				setLabel(lblConnection, CAN_NOT_CONNECT, Color.RED);
				disconnect();
				dialog.setMessage(Status.ERROR.setMessage("Cen Not Connect.("+(Answer.NACK.getAnswer()==bytes[0] ? Answer.NACK : (ToHex.bytesToHex(bytes)))+")"));
			}
		}else{
			setLabel(lblConnection, CAN_NOT_CONNECT, Color.RED);
			disconnect();
			dialog.setMessage(Status.ERROR.setMessage("Con Not Connect.(NULL)"));
		}
	}

	private void setLabel(JLabel label, String text, Color color) {
		label.setText(text);
		label.setBackground(color);
	}

	private void setButtons() {
		boolean selected = comboBoxComPort.getSelectedIndex()!=0;
		btnConnect.setEnabled(selected);
		if(selected){
			if(FlashConnector.isConnected()){
				setLabel(lblConnection, "Connected", Color.GREEN);
				btnRead.setEnabled(comboBoxUnitType.getSelectedIndex()>0);
			}else{
				setLabel(lblConnection, PRESS_CONNECT_BUTTON, Color.YELLOW);
				btnRead.setEnabled(false);
			}
		}else{
			setLabel(lblConnection, "Select The Serial Port", Color.YELLOW);
			btnRead.setEnabled(false);
		}
	}

	private void disconnect() {
		if(FlashConnector.isConnected()){
			try {
				FlashConnector.disconnect();
			} catch (Exception e) {
				logger.catching(e);
			}
		}
		btnConnect.setText(CONNECT);
		btnRead.setEnabled(false);
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

	@Override
	public void update(Observable o, Object obj) {
		logger.entry(o, obj);

		try{

		if (obj == null) {
			if (o instanceof MicrocontrollerSTM32) {
				MicrocontrollerSTM32 stm32 = (MicrocontrollerSTM32) o;
				switch (stm32.getCommand()) {
				case CONNECT:
					setConnected(stm32.getReadBytes());
					break;
				case ERASE:
					dialog.setMessage(new Status[]{Status.ERASE.setMessage("Erased"), Status.BUTTON.setMessage("Ok")});
					break;
				case EXTENDED_ERASE:
					break;
				case GET:
					break;
				case READ_MEMORY:
					readMemory(stm32.getReadBytes());
					break;
				case WRITE_MEMORY:
					break;
				case USER_COMMAND:
					break;
				default:
				}
			}
		} else 
			dialog.setMessage(obj);

		}catch(Exception e){
			logger.catching(e);
		}
		logger.exit();
	}

	private void readMemory(byte[] readBytes) {
		if (buffer!=null) { //buffer contains data to compare

			boolean equals = readBytes!=null && Arrays.equals(buffer, Arrays.copyOf(readBytes, buffer.length));

			logger.trace("Setted '{}'", equals ? "Equal" : "Not Equal");
			dialog.setMessage(equals ? "Equal" : "Not Equal");
			buffer = null;
		} else {
			if (readBytes != null) {
				setTextPaneText(readBytes);
				databaseController.setProfile(Profile.parseProfile(textPane.getText()));
			} else {
				setLabel(lblConnection, "Can Not read Memory", Color.RED);
			}
		}
	}

	private void setTextPaneText(byte[] readBytes) {
		String string = new String(readBytes);
		int indexOf = string.indexOf('\0');
		if (indexOf > 0)
			string = string.substring(0, indexOf);
		textPane.setText(string);
		dialog.setMessage(null);
	}

	private void writeProgram(File file) {
		byte fileContents[] = new byte[(int) file.length()];

		try (FileInputStream fileInputStream = new FileInputStream(file)) {

			fileInputStream.read(fileContents);
			MicrocontrollerSTM32.writeProgram(fileContents);

		} catch (IOException | InterruptedException e) {
			logger.catching(e);
		}
	}

	private boolean isConnected(){
		boolean connected = true;
		if(!FlashConnector.isConnected()){
			connected = false;
			JOptionPane.showMessageDialog(ConnectionPanel.this, "The Unit Is Not Connected.");	
		}else if(!btnRead.isEnabled()){
			connected = false;
			JOptionPane.showMessageDialog(ConnectionPanel.this, "The Unit Type Is Not Selected.");
		}
		return connected;
	}

	private String getProgramPath() {
		String path = "\\\\192.168.2.250\\Share\\4alex\\boards\\SW release\\latest\\";
		Object selectedItem = comboBoxUnitType.getSelectedItem();

		if(selectedItem.equals(Address.BIAS_R1.toString()))
			path += "picobuc_r1.bin";
		else if(selectedItem.equals(Address.BIAS_R2.toString()))
			path += "picobuc_r2.bin";
		else
			path +=  "fcm.bin";

		return path;
	}

	private void compare(File file) {
		buffer = new byte[(int) file.length()];

		try (FileInputStream fileInputStream = new FileInputStream(file)) {
				fileInputStream.read(buffer);
				logger.trace("buffer length={}", buffer.length);
				MicrocontrollerSTM32.read(Address.PROGRAM);
		}catch (IOException | InterruptedException e) {
			logger.catching(e);
		}
	}
}
