package irt.flash.presentation.panel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

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
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import irt.flash.data.ToHex;
import irt.flash.data.connection.FlashConnector;
import irt.flash.data.connection.FlashConnector.ConnectionStatus;
import irt.flash.data.connection.FlashSerialPort;
import irt.flash.data.connection.MicrocontrollerSTM32;
import irt.flash.data.connection.MicrocontrollerSTM32.Address;
import irt.flash.data.connection.MicrocontrollerSTM32.Answer;
import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;
import irt.flash.data.connection.MicrocontrollerSTM32.Status;
import irt.flash.presentation.dialog.MessageDialog;

public class ConnectionPanel extends JPanel implements Observer {

	private static final long serialVersionUID = -79571598522363841L;

	private final static Logger logger = LogManager.getLogger();

	private static final String FCM_UPGRADE = "FCM upgrade";
	private static final String CAN_NOT_CONNECT = "Can Not Connect";
	private static final String DISCONNECT = "Disconnect";
	private static final String CONNECT = "Connect";
	private static final String PRESS_CONNECT_BUTTON = "Press Connect Button";
	public static final String SELECT_SERIAL_PORT = "Select Serial Port";
	public static final String SERIAL_PORT = "flashSerialPort";

	protected static final Preferences prefs = Preferences.userRoot().node("IRT Technologies inc.");

	private JComboBox<String> comboBoxComPort;
	private JComboBox<String> comboBoxUnitType;

	private JLabel lblConnection;
	private JLabel lblUnitType;

	private JButton btnConnect;
	private JButton btnRead;

	private JTextPane textPane;
	private JPopupMenu popupMenu;
	private JMenuItem mntmUploadProfileFromFile;
	private JMenuItem mntmUploadProgram;
	private JMenuItem mntmUploadProgramFrom;
	private JMenuItem mntmEraseProfileMemory;

	private JMenuItem mntmSaveProfileTo;
	private JMenuItem mntmCheckProgram;
	private JMenuItem mntmCheckProgram_1;

	private MessageDialog dialog;

	private Selector selector = new Selector();

	private volatile static byte[] buffer;

	public ConnectionPanel(Window owner) {
		logger.info("* Start *");
		dialog = MessageDialog.getInstance(owner);
		dialog.addButtonActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				new SwingWorker<Void, Void>() {

					@Override
					protected Void doInBackground() throws Exception {
						String buttonText = dialog.getButtonText();
						logger.entry(buttonText);
						switch (buttonText) {
						case "Ok":
							dialog.setMessage((Status) null);
							break;
						default:
							System.exit(0);
						}
						return null;
					}
				}.execute();
			}
		});

		addAncestorListener(new AncestorListener() {
			public void ancestorAdded(AncestorEvent ancestorEvent) {
				final List<String> portNames = FlashSerialPort.getPortNames();
				DefaultComboBoxModel<String> defaultComboBoxModel = new DefaultComboBoxModel<String>(
						portNames.toArray(new String[portNames.size()]));
				defaultComboBoxModel.insertElementAt(SELECT_SERIAL_PORT, 0);
				comboBoxComPort.setModel(defaultComboBoxModel);
				comboBoxComPort.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(ItemEvent itemEvent) {
						if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
							new SwingWorker<Void, Void>() {

								@Override
								protected Void doInBackground() throws Exception {
									disconnect();
									prefs.put(SERIAL_PORT, comboBoxComPort.getSelectedItem().toString());
									setButtons();
									return null;
								}
							}.execute();
						}
					}
				});
				comboBoxComPort.setSelectedItem(prefs.get(SERIAL_PORT, SELECT_SERIAL_PORT));
				setButtons();
			}

			public void ancestorMoved(AncestorEvent ancestorEvent) {
			}

			public void ancestorRemoved(AncestorEvent ancestorEvent) {
			}
		});

		btnConnect = new JButton(CONNECT);
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				SwingUtilities.invokeLater(connectionWorker);
			}
		});
		btnConnect.setFont(new Font("Tahoma", Font.BOLD, 14));
		btnConnect.setMargin(new Insets(0, 0, 0, 0));

		btnRead = new JButton("Read");
		btnRead.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (btnRead.getText().equals(FCM_UPGRADE))
					connectToConverter();
				else
					new ReaderWorker().execute();
			}

			private void connectToConverter() {
				new SwingWorker<Void, Void>() {
					@Override
					protected Void doInBackground() throws Exception {

						String text = Address.CONVERTER.toString();

						comboBoxUnitType.setSelectedItem(text);

						try {

							FlashConnector.write((String) comboBoxComPort.getSelectedItem(),
									new byte[] { 0x7E, (byte) 0xFE, 0x00, 0x00, 0x00, 0x03, 0x00, 0x78, 0x64, 0x00,
											0x00, 0x00, 0x02, 0x00, 0x00, 0x5A, 0x51, 0x7E });
							dialog.setMessage("Wait please.");
							synchronized (this) {
								wait(2000);
							}
							SwingUtilities.invokeLater(connectionWorker);

						} catch (Exception e) {
							logger.catching(e);
							JOptionPane.showMessageDialog(null, e.getLocalizedMessage());
						}
						return null;
					}

				}.execute();
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
		for (Address v : Address.values())
			if (v != Address.PROGRAM)
				defaultComboBoxModel.addElement(v.toString());
		comboBoxUnitType = new JComboBox<String>(defaultComboBoxModel);
		comboBoxUnitType.setSelectedItem(prefs.get("Unit Type", "Select Unit Type"));
		comboBoxUnitType.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent itemEvent) {
				if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
					new SwingWorker<Void, Void>() {

						@Override
						protected Void doInBackground() throws Exception {
							String selectedItem = (String) comboBoxUnitType.getSelectedItem();
							prefs.put("Unit Type", selectedItem);
							setButtons();
							boolean isSelectes = comboBoxUnitType.getSelectedIndex() > 0;
							setLabel(lblUnitType, isSelectes ? "Unit Type" : "Select The Unit Type",
									isSelectes ? Color.GREEN : Color.YELLOW);
							return null;
						}

					}.execute();
				}
			}
		});
		comboBoxUnitType.setFont(new Font("Tahoma", Font.BOLD, 14));

		boolean selectedIndex = comboBoxUnitType.getSelectedIndex() > 0;
		lblUnitType = new JLabel(selectedIndex ? "Unit Type" : "Select The Unit Type");
		lblUnitType.setBackground(selectedIndex ? Color.GREEN : Color.YELLOW);
		lblUnitType.setOpaque(true);
		lblUnitType.setHorizontalAlignment(SwingConstants.CENTER);
		lblUnitType.setFont(new Font("Tahoma", Font.BOLD, 14));

		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);

		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout
				.setHorizontalGroup(
						groupLayout.createParallelGroup(Alignment.LEADING).addGroup(
								groupLayout.createSequentialGroup().addContainerGap().addGroup(groupLayout
										.createParallelGroup(Alignment.LEADING).addGroup(groupLayout
												.createSequentialGroup()
												.addComponent(tabbedPane, GroupLayout.PREFERRED_SIZE, 430,
														Short.MAX_VALUE)
												.addContainerGap())
										.addGroup(groupLayout.createSequentialGroup()
												.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
														.addComponent(lblUnitType, GroupLayout.DEFAULT_SIZE, 343,
																Short.MAX_VALUE)
														.addComponent(lblConnection, GroupLayout.DEFAULT_SIZE, 343,
																Short.MAX_VALUE))
												.addPreferredGap(ComponentPlacement.RELATED)
												.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
														.addGroup(groupLayout.createSequentialGroup()
																.addComponent(comboBoxComPort,
																		GroupLayout.PREFERRED_SIZE, 117,
																		GroupLayout.PREFERRED_SIZE)
																.addPreferredGap(ComponentPlacement.RELATED)
																.addComponent(btnConnect, GroupLayout.PREFERRED_SIZE,
																		83, GroupLayout.PREFERRED_SIZE))
														.addGroup(groupLayout.createSequentialGroup()
																.addComponent(comboBoxUnitType,
																		GroupLayout.PREFERRED_SIZE,
																		GroupLayout.DEFAULT_SIZE,
																		GroupLayout.PREFERRED_SIZE)
																.addPreferredGap(ComponentPlacement.RELATED)
																.addComponent(btnRead, GroupLayout.PREFERRED_SIZE, 83,
																		GroupLayout.PREFERRED_SIZE)))
												.addGap(10)))));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup().addContainerGap()
						.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
								.addComponent(btnConnect, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE)
								.addComponent(comboBoxComPort, GroupLayout.PREFERRED_SIZE,
										GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addComponent(lblConnection))
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
								.addComponent(btnRead, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE)
								.addComponent(comboBoxUnitType, GroupLayout.PREFERRED_SIZE, 28,
										GroupLayout.PREFERRED_SIZE)
								.addComponent(lblUnitType, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE, 205, Short.MAX_VALUE).addContainerGap()));
		groupLayout.linkSize(SwingConstants.VERTICAL, new Component[] { btnConnect, comboBoxComPort, lblConnection });
		groupLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] { comboBoxComPort, comboBoxUnitType });

		JScrollPane scrollPane = new JScrollPane();
		tabbedPane.addTab("Profile", null, scrollPane, null);

		textPane = new JTextPane();
		textPane.setEditable(false);
		scrollPane.setViewportView(textPane);

		popupMenu = new JPopupMenu();
		addPopup(textPane, popupMenu);

		mntmUploadProfileFromFile = new JMenuItem("Upload Profile From File...");
		mntmUploadProfileFromFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				uploadProfile();
			}
		});

		mntmEraseProfileMemory = new JMenuItem("Erase Profile Memory");
		mntmEraseProfileMemory.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				new SwingWorker<Void, Void>() {

					@Override
					protected Void doInBackground() throws Exception {
						if (FlashConnector.getConnectionStatus()==ConnectionStatus.CONNECTED) {
							if (btnRead.isEnabled()) {
								try {
									dialog.setMessage(Status.ERASE.setMessage("Memory Erasing"));
									MicrocontrollerSTM32.erase((String) comboBoxUnitType.getSelectedItem()).get(5, TimeUnit.SECONDS);
								} catch (Exception e) {
									logger.catching(e);
								}
							} else
								JOptionPane.showMessageDialog(ConnectionPanel.this, "The Unit Type Is Not Selected.");
						} else
							JOptionPane.showMessageDialog(ConnectionPanel.this, "The Unit Is Not Connected.");
						return null;
					}
				}.execute();
			}
		});
		popupMenu.add(mntmEraseProfileMemory);
		popupMenu.add(mntmUploadProfileFromFile);

		mntmUploadProgram = new JMenuItem("Upload Program");
		mntmUploadProgram.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				uploadProgram();
			}
		});

		mntmSaveProfileTo = new JMenuItem("Save Profile to File...");
		mntmSaveProfileTo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				new SwingWorker<Void, Void>() {

					@Override
					protected Void doInBackground() throws Exception {
						if (!textPane.getText().isEmpty()) {

							@SuppressWarnings("serial")
							JFileChooser fc = new JFileChooser() {
								@Override
								public void approveSelection() {
									File f = getSelectedFile();
									if (f.exists() && getDialogType() == SAVE_DIALOG) {
										switch (JOptionPane.showConfirmDialog(this,
												"The File '" + f.getName()
														+ "' already exists.\nDo you want to replace it?",
												"Existing file", JOptionPane.YES_NO_OPTION)) {
										case JOptionPane.YES_OPTION:
											super.approveSelection();
										}
									} else
										super.approveSelection();
								}
							};
							fc.setDialogTitle("Save Profile");
							fc.setMultiSelectionEnabled(false);

							FileNameExtensionFilter fileNameExtensionFilter = new FileNameExtensionFilter(
									"Bin files(bin)", "bin");
							fc.addChoosableFileFilter(fileNameExtensionFilter);
							fc.setFileFilter(fileNameExtensionFilter);

							String pathStr = prefs.get("profile_path", null);
							if (pathStr != null)
								fc.setSelectedFile(Paths.get(pathStr).getParent().resolve("profile.bin").toFile());

							if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {

								File selectedFile = fc.getSelectedFile();
								String absolutePath = selectedFile.getAbsolutePath();
								if (pathStr == null || !pathStr.equals(absolutePath))
									prefs.put("profile_path", absolutePath);

								try (BufferedWriter out = new BufferedWriter(new FileWriter(selectedFile))) {
									textPane.write(out);
									out.flush();
								} catch (IOException e) {
									logger.catching(e);
									dialog.setMessage("There was an error saving your file.");
								}
							}
						} else
							dialog.setMessage("The Profile was not read from the Unit.");
						return null;
					}
				}.execute();
			}
		});
		popupMenu.add(mntmSaveProfileTo);
		popupMenu.add(mntmUploadProgram);

		mntmUploadProgramFrom = new JMenuItem("Upload Program From File...");
		mntmUploadProgramFrom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				new SwingWorker<Void, Void>() {

					@Override
					protected Void doInBackground() throws Exception {
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
									uploadProgram(file);
								}
							}
						}
						return null;
					}
				}.execute();
			}
		});

		mntmCheckProgram = new JMenuItem("Check Program");
		mntmCheckProgram.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				new SwingWorker<Void, Void>() {

					@Override
					protected Void doInBackground() throws Exception {
						if (isConnected()) {
							String path = getProgramPath();

							logger.trace("path={}", path);

							File file = new File(path);
							if (file.exists()) {
								compare(file);
							} else
								dialog.setMessage("The File do not exist.");
						}
						return null;
					}
				}.execute();
			}
		});
		popupMenu.add(mntmCheckProgram);
		popupMenu.add(mntmUploadProgramFrom);

		mntmCheckProgram_1 = new JMenuItem("Check Program with File...");
		mntmCheckProgram_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new SwingWorker<Void, Void>() {

					@Override
					protected Void doInBackground() throws Exception {
						if (isConnected()) {

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
								compare(file);
							}
						}
						return null;
					}
				}.execute();
			}
		});
		popupMenu.add(mntmCheckProgram_1);
		setLayout(groupLayout);

		MicrocontrollerSTM32.getInstance().addObserver(this);

		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {

			private boolean ctrl;
//			private boolean shift;

			@Override
			public boolean dispatchKeyEvent(KeyEvent e) {
				final int id = e.getID();
				final int keyCode = e.getKeyCode();

				switch (id) {
				case KeyEvent.KEY_PRESSED:
//					logger.error(e);
					keyPressed(keyCode);
					break;

				case KeyEvent.KEY_RELEASED:
//					logger.error(e);
					keyReleased(keyCode);
				}
				return false;
			}

			private void keyReleased(final int keyCode) {

				switch (keyCode) {

				case KeyEvent.VK_CONTROL:
					ctrl = false;
					break;

				case KeyEvent.VK_EQUALS:
					if(ctrl) selector.change(comboBoxUnitType, true);
					break;
				case KeyEvent.VK_ADD:
					if (ctrl) selector.change(comboBoxComPort, true);
					break;

				case KeyEvent.VK_MINUS:
					if(ctrl) selector.change(comboBoxUnitType, false);
					break;
				case KeyEvent.VK_SUBTRACT:
					if (ctrl) selector.change(comboBoxComPort, false);//TODO
					break;

				case KeyEvent.VK_C:
					if(!ctrl)
						return;
					boolean isSelected = Optional.ofNullable(textPane.getSelectedText()).filter(t->!t.isEmpty()).isPresent();
					if(!isSelected)	// if text is selected do nothing
						SwingUtilities.invokeLater(connectionWorker);
					break;

				case KeyEvent.VK_R:
					if(ctrl) new ReaderWorker().execute();
					break;

				case KeyEvent.VK_P:
					if(ctrl) uploadProgram();
					break;

				case KeyEvent.VK_F:
					if(ctrl) uploadProfile();
					break;

				case KeyEvent.VK_ENTER:
				case KeyEvent.VK_ESCAPE:
					if(dialog.isVisible()) dialog.setVisible(false);
					break;
				default:
					if(ctrl)
						if(keyCode>=KeyEvent.VK_0 && keyCode<=KeyEvent.VK_9)
							selector.select(comboBoxUnitType, keyCode-48);
						else if(keyCode>=KeyEvent.VK_NUMPAD0 && keyCode<=KeyEvent.VK_NUMPAD9)
							selector.select(comboBoxComPort, keyCode-96);
						
							
				}
			}

			private void keyPressed(final int keyCode) {

				switch (keyCode) {
				case KeyEvent.VK_CONTROL:
					ctrl = true;
					break;
//				case KeyEvent.VK_SHIFT:
//					shift = true;
				}
			}
		});
	}

	private void setLabel(JLabel label, String text, Color color) {
		label.setText(text);
		label.setBackground(color);
	}

	private void setButtons() {
		boolean selected = comboBoxComPort.getSelectedIndex() != 0;
		btnConnect.setEnabled(selected);

		logger.trace("CON Port is Selected = {}", selected);

		if (selected) {
			if (FlashConnector.getConnectionStatus()==ConnectionStatus.CONNECTED)
				setLabel(lblConnection, "Connected", Color.GREEN);
			else
				setLabel(lblConnection, PRESS_CONNECT_BUTTON, Color.YELLOW);
		}
		setReadButton();
	}

	private void disconnect() {
		new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				if (FlashConnector.getConnectionStatus()==ConnectionStatus.CONNECTED) {
					try {
						FlashConnector.disconnect();
					} catch (Exception e) {
						logger.catching(e);
					}
				}
				btnConnect.setText(CONNECT);
				setReadButton();
				return null;
			}
		}.execute();
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
		new UpdateWorker(o, obj).execute();
	}

	private void uploadProgram(File file) {
		byte fileContents[] = new byte[(int) file.length()];

		try (FileInputStream fileInputStream = new FileInputStream(file)) {

			fileInputStream.read(fileContents);
			MicrocontrollerSTM32.writeProgram(fileContents);

		} catch (IOException | InterruptedException e) {
			logger.catching(e);
		}
	}

	private boolean isConnected() {
		boolean connected = true;
		if (FlashConnector.getConnectionStatus()!=ConnectionStatus.CONNECTED) {
			connected = false;
			JOptionPane.showMessageDialog(ConnectionPanel.this, "The Unit Is Not Connected.");
		} else if (!btnRead.isEnabled()) {
			connected = false;
			JOptionPane.showMessageDialog(ConnectionPanel.this, "The Unit Type Is Not Selected.");
		}
		return connected;
	}

	private String getProgramPath() {
		String path = "\\\\192.168.2.250\\Share\\4alex\\boards\\SW release\\latest\\";
		Object selectedItem = comboBoxUnitType.getSelectedItem();

		if (selectedItem.equals(Address.BIAS.toString()))
			path += "picobuc.bin";
		else
			path += "fcm.bin";

		return path;
	}

	private void compare(File file) {
		buffer = new byte[(int) file.length()];

		try (FileInputStream fileInputStream = new FileInputStream(file)) {
			fileInputStream.read(buffer);
			logger.trace("buffer length={}", buffer.length);
			MicrocontrollerSTM32.read(Address.PROGRAM);
		} catch (IOException | InterruptedException e) {
			logger.catching(e);
		}
	}

	private void uploadProgram() {
		new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				if (isConnected()) {
					if (textPane.getText().isEmpty())
						dialog.setMessage("First Have to Read Profile.");
					else {
						String path = getProgramPath();

						logger.trace("path={}", path);

						File file = new File(path);
						if (file.exists()) {
							uploadProgram(file);
						} else
							dialog.setMessage("The File do not exist.");
					}
				}
				return null;
			}
		}.execute();
	}

	private void uploadProfile() {
		new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				if (isConnected()) {
					if (textPane.getText().isEmpty())
						dialog.setMessage("First Have to Read Profile.");
					else
						try {
							uploadProfileFromFile();
						} catch (Exception e) {
							logger.catching(e);
						}
				}
				return null;
			}

			private void uploadProfileFromFile() throws FileNotFoundException, InterruptedException, UnknownHostException {

				JFileChooser fc = new JFileChooser();
				fc.setDialogTitle("Upload Profile...");
				fc.setApproveButtonToolTipText("Upload selected profile.");
				fc.setMultiSelectionEnabled(false);

				FileNameExtensionFilter fileNameExtensionFilter = new FileNameExtensionFilter("Bin files(bin)", "bin");
				fc.addChoosableFileFilter(fileNameExtensionFilter);
				fc.setFileFilter(fileNameExtensionFilter);

				Path p = null;
				Object selectedUnitType = comboBoxUnitType.getSelectedItem();
				String key = selectedUnitType + " profilePath";
				String pathStr = prefs.get(key, null);

				if (pathStr != null) {
					p = Paths.get(pathStr);
					fc.setSelectedFile(p.toFile());
				}

				if (fc.showDialog(ConnectionPanel.this, "Upload") == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();

					StringBuffer fileContents = new StringBuffer();
					final ProfileParser profileParser = new ProfileParser();

					try (Scanner scanner = new Scanner(file)) {
						while (scanner.hasNextLine()){
							final String trim = scanner.nextLine().trim();
							fileContents.append(trim).append("\n");
							profileParser.append(trim);
						}
					}

					if (fileContents.length()>0) {

						int deviceType = profileParser.getDeviceType();

						if (deviceType >= 0) {

							boolean bais = deviceType < 1000;

							logger.debug("deviceType={}, bais={}", deviceType, bais);

							boolean upload = true;

							if ((selectedUnitType.equals(Address.CONVERTER.toString()) && bais) || (selectedUnitType.equals(Address.BIAS.toString()) && !bais)) {

								String message = "Selected Profile is created for "
										+ (bais ? Address.BIAS.toString() : Address.CONVERTER.toString())
										+ "\nbut selected 'Unit Type' is " + selectedUnitType
										+ ".\n\nTo continue press 'OK' button.\n";

								upload = JOptionPane.showConfirmDialog(ConnectionPanel.this, message, "Warning", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
							}

							if(upload && profileParser.hasError()){
								
								String message = "The profile has error(s):\n"
										+ profileParser.getReports()
										+ ".\n\nTo continue press 'OK' button.\n";

								upload = JOptionPane.showConfirmDialog(ConnectionPanel.this, message, "Profile ERROR", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
							}

							if (upload) {

								String path = file.getAbsolutePath();
								if (p == null || !path.equals(pathStr)) {
									prefs.put(key, path);
								}

// Signature
								fileContents
								.append("\n#Uploaded by STM32 on ")
								.append(new Timestamp(new Date().getTime()))
								.append(" from ")
								.append(InetAddress.getLocalHost().getHostName())
								.append(" computer.")
								.append('\0');

								MicrocontrollerSTM32.writeProfile((String) selectedUnitType, fileContents.toString());
							}
						} else
							JOptionPane.showMessageDialog(ConnectionPanel.this, "Profile is missing 'DEVICE TYPE'");
					}
				}
			}
		}.execute();
	}

	//	*************************************************************************
	//	*							Connection Worker							*
	//	*************************************************************************
	private final Runnable connectionWorker = ()->{
		String text = btnConnect.getText();
		logger.entry(text);
		try {
			switch (text) {
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
	};

	// ******************************************* ReaderWorker *************************************************************
	private class ReaderWorker extends SwingWorker<Void, Void> {

		@Override
		protected Void doInBackground() throws Exception {
			try {

				MicrocontrollerSTM32.read((String) comboBoxUnitType.getSelectedItem()).get(1, TimeUnit.SECONDS);

			} catch (InterruptedException e) {
				logger.catching(e);
			}
			return null;
		}
	}

	// ******************************************* UpdateWorker *************************************************************
	private class UpdateWorker extends SwingWorker<Observable, Object> {

		private Observable observable;
		private Object object;

		public UpdateWorker(Observable observable, Object object) {
			this.observable = observable;
			this.object = object;
		}

		@Override
		protected Observable doInBackground() throws Exception {
			logger.trace("observable={}, object={}", observable, object);
			try {

				if (object == null) {
					if (observable instanceof MicrocontrollerSTM32) {
						MicrocontrollerSTM32 stm32 = (MicrocontrollerSTM32) observable;
						switch (stm32.getCommand()) {
						case CONNECT:
							logger.trace("CONNECT");
							setConnected(stm32.getReadBytes());
							break;
						case ERASE:
							logger.trace("ERASE");
							dialog.setMessage(
									new Status[] { Status.ERASE.setMessage("Erased"), Status.BUTTON.setMessage("Ok") });
							break;
						case EXTENDED_ERASE:
							logger.trace("EXTENDED_ERASE");
							break;
						case GET:
							logger.trace("GET");
							break;
						case READ_MEMORY:
							readMemory(stm32.getReadBytes());
							break;
						case WRITE_MEMORY:
							logger.trace("switch('WRITE_MEMORY')");
							break;
						case USER_COMMAND:
							logger.trace("USER_COMMAND");
						}
					}
				} else {
					logger.trace("dialog.setMessage({})", object);
					dialog.setMessage(object);
				}

			} catch (Exception e) {
				logger.catching(e);
			}
			return null;
		}

		private void setConnected(byte[] bytes) {
			if (bytes != null && bytes.length == 1) {
				boolean isConnected = bytes[0] == Answer.ACK.getAnswer();
				if (isConnected) {
					setLabel(lblConnection, "Connected", Color.GREEN);
					btnConnect.setText(DISCONNECT);
					setReadButton();
					new ReaderWorker().execute();
				} else {
					setLabel(lblConnection, CAN_NOT_CONNECT, Color.RED);
					disconnect();
					dialog.setMessage(Status.ERROR.setMessage("Can Not Connect.(" + (Answer.NACK.getAnswer() == bytes[0] ? Answer.NACK : (ToHex.bytesToHex(bytes))) + ")"));
				}
			} else {
				setLabel(lblConnection, CAN_NOT_CONNECT, Color.RED);
				disconnect();
				dialog.setMessage(Status.ERROR.setMessage("Can Not Connect.(NULL)"));
			}
		}

		private void readMemory(byte[] readBytes) throws IOException {
			if (buffer != null) { // buffer contains data to compare

				boolean equals = readBytes != null && Arrays.equals(buffer, Arrays.copyOf(readBytes, buffer.length));

				logger.trace("Setted '{}'", equals ? "Equal" : "Not Equal");
				dialog.setMessage(equals ? "Equal" : "Not Equal");
				buffer = null;
			} else {
				logger.debug("readBytes = {}", readBytes);
				if (readBytes != null) {
					readBytes = removeEnd(readBytes);
					setTextPaneText(readBytes);
				} else {
					setLabel(lblConnection, "Can Not read Memory", Color.RED);
				}
			}
		}

		private void setTextPaneText(final byte[] readBytes) {

			new SwingWorker<Void, Void>() {

				@Override
				protected Void doInBackground() throws Exception {
					setName("setTextPaneText(final byte[] readBytes)");
					logger.debug(readBytes);
					if (readBytes != null) {
						String string = new String(readBytes);
						textPane.setText(string);
						textPane.setForeground(Color.BLACK);
					} else {
						textPane.setText(comboBoxUnitType.getSelectedItem() + " does not have a Profile.");
						textPane.setForeground(Color.RED);
					}
					return null;
				}
			}.execute();

			dialog.setMessage(null);
		}

		private byte[] removeEnd(byte[] readBytes) {
			int ffCounter = 0;
			int offset = -1;

			for (int i = 0; i < readBytes.length; i++) {
				logger.trace("readBytes[{}]={}", i, readBytes[i]);
				if (readBytes[i] == 0) {
					offset = i;
					break;
				} else if (readBytes[i] == (byte) 0xFF) {

					if (ffCounter == 0)
						offset = i;

					else if (ffCounter >= 2)
						break;

					ffCounter++;

				} else
					ffCounter = 0;
			}
			logger.trace("ffCounter={}, offset={}", ffCounter, offset);

			return offset > 0 ? Arrays.copyOfRange(readBytes, 0, offset) : null;
		}
	}

	private void setReadButton() {
		ConnectionStatus connected = FlashConnector.getConnectionStatus();
		if (!comboBoxUnitType.getSelectedItem().equals(Address.CONVERTER.toString()) && connected!=ConnectionStatus.CONNECTED)
			setEnableReadButton(FCM_UPGRADE, FCM_UPGRADE, true);
		else
			setEnableReadButton("Read", "Read Profile", comboBoxUnitType.getSelectedIndex() > 0 && connected==ConnectionStatus.CONNECTED);
	}

	private void setEnableReadButton(String text, String toolTipText, boolean enable) {
		btnRead.setText(text);
		btnRead.setToolTipText(toolTipText);
		btnRead.setEnabled(enable);
	}

	// **********************************************************************************************
	// *									Selector												*
	// **********************************************************************************************
	private boolean busy;
	private class Selector{

		public void change(JComboBox<String> comboBox, boolean increase) {

			if(busy) return; busy = true;

			final int itemCount = comboBox.getItemCount();

			if(itemCount==0){
				busy = false;
				return;
			}

			int selectedIndex = increase ? comboBox.getSelectedIndex()+1 : comboBox.getSelectedIndex()-1;

			if (itemCount <= selectedIndex)
					selectedIndex = 0;

			else if(selectedIndex < 0)
				selectedIndex = itemCount -1;

			final int si = selectedIndex;
			new SwingWorker<Void, Void>(){

				@Override
				protected Void doInBackground() throws Exception {

					comboBox.setSelectedIndex(si);

					busy = false;
					return null;
				}
				
			}.execute();
		}

		public void select(JComboBox<String> comboBox, int index) {

			if(busy) return; busy = true;

			final int itemCount = comboBox.getItemCount();
//			logger.error("index= {}, itemCount={}", index, itemCount);

			if(itemCount==0){
				busy = false;
				return;
			}

			if(index<0 || itemCount<=index){
				busy = false;
				change(comboBox,true);
				return;
			}

			new SwingWorker<Void, Void>(){

				@Override
				protected Void doInBackground() throws Exception {

					comboBox.setSelectedIndex(index);

					busy = false;
					return null;
				}
				
			}.execute();
		}
	}

	/**
	 * This class finds errors in the profile
	 */
	public static class ProfileParser{

		public static final String LUT = "-lut-";
		private List<String> lines = new ArrayList<>();
		private Map<String, TestResult> report;

		public void append(String line) {

			report = null;

			Optional
			.of(line)
			.filter(l->!l.isEmpty())
			.filter(l->l.charAt(0)!='#')
			.filter(l->l.contains(LUT) || l.startsWith(ProfileProperties.DEVICE_TYPE.toString()))
			.map(l->l.split("#")[0])
			.ifPresent(lines::add);
		}

		public boolean hasError() {

			final Map<String, TestResult> collect = getReports();
			return !collect.isEmpty();
		}

		public Map<String, TestResult> getReports() {
			logger.traceEntry();

			if(report==null)
				report = lines
							.stream()
							.filter(l->l.contains(LUT))
							.collect(Collectors.groupingBy(l->l.split(LUT)[0]))
							.entrySet().stream()
							.map(test())
							.filter(e->e.getValue()!=TestResult.NO_ERROR)//collect errors only
							.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

			return report;
		}

		//test for errors
		private Function< Map.Entry<String,List<String>>, Map.Entry<String, TestResult>> test() {
			return table->{
				logger.entry(table);
				String tableName = table.getKey();
				Map<Boolean, List<String>> t = table.getValue().stream().collect(Collectors.partitioningBy(l->l.contains("-size")));

				final int tableSize = getTableSize(t);

				// Can not get table size
				if(tableSize<0)
					return new AbstractMap.SimpleEntry<String, TestResult>(tableName, TestResult.WRONG_SIZE_VALUE);

				final List<String> content = t.get(false);

				// Table size does not match to line count
				if(content.size()!=tableSize)
					return new AbstractMap.SimpleEntry<String, TestResult>(tableName, TestResult.WRONG_TABLE_SIZE);

				List<SimpleEntry<Double, Double>> tableContent = content.stream().map(l->l.split("\\s+"))
														.filter(split->split.length>2)
														.map(split->new AbstractMap.SimpleEntry<String, String>(split[1], split[2]))
														.map(entry->new AbstractMap.SimpleEntry<Double, Double>(Double.parseDouble(entry.getKey()), Double.parseDouble(entry.getValue())))
														.collect(Collectors.toList());

				// Table has wrong structure
				if(tableContent.size()!=tableSize)
					return new AbstractMap.SimpleEntry<String, TestResult>(tableName, TestResult.WRONG_STRUCTURE);

				final SequenceChecker seq1 = new SequenceChecker();
				final SequenceChecker seq2 = new SequenceChecker();
				final boolean ignoreValues = tableName.equals("frequency") || tableName.equals("rf-gain");

				List<SimpleEntry<Double, Double>> sequence = tableContent.stream()
																		.filter(e->seq1.add(e.getKey()) && (ignoreValues || seq2.add(e.getValue())))
																		.collect(Collectors.toList());

				// Table has wrong structure
				if(sequence.size()!=tableSize)
					return new AbstractMap.SimpleEntry<String, TestResult>(tableName, TestResult.WRONG_SEQUENCE);

				return new AbstractMap.SimpleEntry<String, TestResult>(tableName, TestResult.NO_ERROR);
			};
		}

		private int getTableSize(Map<Boolean, List<String>> t) {
			//Parse table size
			final int tableSize = t.get(true).stream().findAny()
										.map(sizeLine->sizeLine.split("\\s+"))
										.filter(split->split.length>1)
										.map(split->split[1])
										.map(size->size.replaceAll("\\D", ""))
										.filter(size->!size.isEmpty())
										.map(Integer::parseInt)
										.orElse(-1);
			return tableSize;
		}

		/**
		 * @return Device type number from the profile
		 */
		public Integer getDeviceType() {
			return lines
					.parallelStream()
					.filter(l->l.startsWith(ProfileProperties.DEVICE_TYPE.toString()))
					.findAny()
					.map(l->l.split(" "))
					.filter(arr->arr.length>1)
					.map(arr->arr[1])
					.map(dt->dt.replaceAll("\\D", ""))
					.filter(dt->!dt.isEmpty())
					.map(Integer::parseInt)
					.orElse(-1);
		}
	}

	public enum TestResult{
		NO_ERROR,
		WRONG_SIZE_VALUE,//size value is not readable
		WRONG_TABLE_SIZE,//size does not match \
		WRONG_SEQUENCE,
		WRONG_STRUCTURE
	}

	private static class SequenceChecker{

		private Double value;
		private Boolean sequence; // null - no result,  true - increase, false - decrease. 

		public boolean add(Double value) {

			if(value == null)
				return false;

			// First time call
			if(this.value == null){
				this.value = value;
				return true;
			}

			final boolean seq = this.value.compareTo(value) > 0;
			this.value = value;

			// Define type of sequence (decrease or increase)
			if(sequence == null){
				sequence = seq; 
				return true;
			}

			// Check type of the sequence
			return sequence == seq;
		}
		
	}
}
