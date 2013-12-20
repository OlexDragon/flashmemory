package irt.flash.presentation.dialog;

import irt.flash.data.connection.MicrocontrollerSTM32.Status;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JProgressBar;
import javax.swing.JLabel;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import java.awt.Dimension;

public class MessageDialog extends JDialog implements Runnable{
	private static final long serialVersionUID = -2685615105998432650L;

	private final Logger logger = (Logger) LogManager.getLogger();

	private final JPanel contentPanel = new JPanel();
	private JPanel buttonPane;
	private JButton button;

	private static MessageDialog messageDialog;
	private JTextArea textArea;
	private JPanel panelProgressBar;

	private JProgressBar progressBar;

	private JLabel lblProgress;

	private List<Object> message = new ArrayList<>();

	public static MessageDialog getInstance(Window owner){
		if(messageDialog==null)
			messageDialog = new MessageDialog(owner);
		return messageDialog;
	}

	private MessageDialog(Window owner) {
		super(owner, Dialog.DEFAULT_MODALITY_TYPE);
		setResizable(false);
		setUndecorated(true);
		getContentPane().setLayout(new BorderLayout(4, 4));
		{
			JPanel mainPanel = new JPanel();
			mainPanel.setBorder(new LineBorder(new Color(0, 0, 0), 2, true));
			getContentPane().add(mainPanel, BorderLayout.CENTER);
			contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
			contentPanel.setLayout(new BorderLayout());
			{
				buttonPane = new JPanel();
				buttonPane.setLayout(new FlowLayout(FlowLayout.CENTER));
				{
					button = new JButton("Cancel");
					button.setActionCommand("Cancel");
					buttonPane.add(button);
				}
			}
			mainPanel.setLayout(new BorderLayout(0, 0));
			mainPanel.add(contentPanel, BorderLayout.CENTER);
			{
				textArea = new JTextArea();
				textArea.setEditable(false);
				textArea.setFont(new Font("Monospaced", Font.BOLD, 16));
				contentPanel.add(textArea, BorderLayout.CENTER);
			}
			{
				panelProgressBar = new JPanel();
				panelProgressBar.setVisible(false);
				contentPanel.add(panelProgressBar, BorderLayout.NORTH);
				
				progressBar = new JProgressBar();
				progressBar.setPreferredSize(new Dimension(14, 14));
				progressBar.setMaximum(10000);
				lblProgress = new JLabel("");
				lblProgress.setHorizontalAlignment(SwingConstants.CENTER);

				GroupLayout gl_panelProgressBar = new GroupLayout(panelProgressBar);
				gl_panelProgressBar.setHorizontalGroup(
					gl_panelProgressBar.createParallelGroup(Alignment.LEADING)
						.addComponent(progressBar, 0, 507, Short.MAX_VALUE)
						.addGroup(gl_panelProgressBar.createSequentialGroup()
							.addGap(164)
							.addComponent(lblProgress, GroupLayout.DEFAULT_SIZE, 178, Short.MAX_VALUE)
							.addGap(165))
				);
				gl_panelProgressBar.setVerticalGroup(
					gl_panelProgressBar.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_panelProgressBar.createSequentialGroup()
							.addComponent(progressBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(lblProgress, GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
							.addContainerGap())
				);
				panelProgressBar.setLayout(gl_panelProgressBar);
			}
			mainPanel.add(buttonPane, BorderLayout.SOUTH);
		}
		Thread t = new Thread(this);
		int priority = t.getPriority();
		if(priority>Thread.MIN_PRIORITY)
			t.setPriority(priority-1);
		t.setDaemon(true);
		t.start();
	}

	private synchronized void resize() {
		logger.entry();
		pack();
		Rectangle bounds = getOwner().getBounds();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		double appX = bounds.getX();
		double appWidth = bounds.getWidth();
		double tmp = screenSize.getWidth()-(appX+appWidth);
		if(tmp<0)
			appWidth = appWidth + tmp;
		if(appX<0)
			appX = 0;
		int x = (int) (appX+(appWidth-getWidth())/2);

		double appY = bounds.getY();
		double appHeight = bounds.getHeight();
		tmp = screenSize.getHeight()-appY-appHeight;
		if(tmp<0)
			appHeight = appHeight + tmp;
		int y = (int) (appY+(appHeight-getHeight())/2);

		logger.trace("Location: x={}, y={}", x, y); 
		setLocation(x, y);

		if(!isVisible())//Set Visible
			SwingUtilities.invokeLater(new Runnable() { @Override public void run() { logger.trace("setVisible(true)"); setVisible(true); dispose(); }
		});

		logger.exit();
	}

	@Override
	public void run() {
		while(true){
			if(message.isEmpty()){
				logger.trace("wait();");
				synchronized (this) { try { wait(); } catch (InterruptedException e) { logger.catching(e); } }
			}
			logger.trace("SET MESSAGE: {}", message);
			set(message.get(0));
			message.remove(0);
		}
	}

	public String getButtonText(){
		return button.getText();
	}

	public synchronized void addButtonActionListener(ActionListener actionListener){
		button.addActionListener(actionListener);
	}

	public String getMessage(){
		return textArea.getText();
	}

	public synchronized void setMessage(Object message){
		logger.entry(message);
		this.message.add(message);
		notify();
		logger.exit();
	}

	private synchronized <T> void set(T message) {
		logger.entry(message);
		if(message!=null){
			if(message instanceof Status[])
				setMessage((Status[])message);
			else if(message instanceof Status)
				setMessage((Status)message);
			else if(message instanceof BigDecimal)
				setProgressBar((BigDecimal)message);
			else if(message instanceof String)
				setMessage((String)message);
		}else if(isVisible()){
			logger.trace("setVisible(false); Text={}", textArea.getText());
			panelProgressBar.setVisible(false);
			setVisible(false);
		}
		logger.exit();
	}

	private void setProgressBar(BigDecimal bigDecimal) {
		logger.entry(bigDecimal);
		int progress = bigDecimal.multiply(new BigDecimal(100)).intValueExact();
		progressBar.setValue(progress);
		lblProgress.setText(bigDecimal.toString()+" %");
		if(!panelProgressBar.isVisible()){
			panelProgressBar.setVisible(true);
			resize();
		}
		logger.exit();
	}

	private void setMessage(String text) {
		logger.entry(text);
		textArea.setText(text);
		button.setText("Ok");
		resize();
		logger.exit();
	}

	private void setMessage(Status[] statuses) {
		logger.entry((Object)statuses);

		for (Status s : statuses)
				setMessage(s);
		logger.exit();
	}

	private void setMessage(Status status) {
		logger.entry(status);
		switch (status) {
		case BUTTON:
			button.setText(status.getMessage());
			break;
		case ERROR:
			String message = status.getMessage();
			logger.trace(message);
			textArea.setText(message);
			button.setText("Ok");
			resize();
			break;
		default:
			message = status.getMessage();
			if (message != null) {
				textArea.setText(message);
				resize();
			} else if (isVisible())
				setVisible(false);
		}

		logger.exit();
	}
}
