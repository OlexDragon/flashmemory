package irt.flash.presentation.dialog;

import irt.flash.data.connection.MicrocontrollerSTM32.Status;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.math.BigDecimal;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class MessageDialog extends JDialog{
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

	private DialogWorker dialogWorker;

	public static MessageDialog getInstance(Window owner){
		if(messageDialog==null)
			messageDialog = new MessageDialog(owner);
		return messageDialog;
	}

	private MessageDialog(Window owner) {
		super(owner, Dialog.DEFAULT_MODALITY_TYPE);

		logger.info("* Start *");

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
				textArea.setFocusable(false);
				textArea.setFocusTraversalKeysEnabled(false);
				textArea.setEditable(false);
				textArea.setFont(new Font("Monospaced", Font.BOLD, 16));
				contentPanel.add(textArea, BorderLayout.CENTER);
			}
			{
				panelProgressBar = new JPanel();
				panelProgressBar.setVisible(false);
				contentPanel.add(panelProgressBar, BorderLayout.NORTH);
				
				progressBar = new JProgressBar();
				progressBar.setFocusTraversalKeysEnabled(false);
				progressBar.setFocusable(false);
				progressBar.setPreferredSize(new Dimension(14, 14));
				progressBar.setMaximum(10000);
				lblProgress = new JLabel("");
				lblProgress.setFocusTraversalKeysEnabled(false);
				lblProgress.setFocusable(false);
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

		if(dialogWorker!=null)
			dialogWorker.cancel(true);

		dialogWorker = new DialogWorker(message);
		dialogWorker.execute();
	}

	//********************************************************************
	private class DialogWorker extends SwingWorker<Object, Void>{

		private Object message;

		public DialogWorker(Object message){
			logger.trace("DialogWorker(Object message={})", message);
			this.message = message;
		}

		@Override
		protected Object doInBackground() throws Exception {

			if(message!=null){
				if(message instanceof Status[]){
					logger.trace("(Status[])message={}", (Object[])message);
					setMessage((Status[])message);
				}else if(message instanceof Status){
					logger.trace("(Status)message={}", message);
					new StatusWorker((Status)message).execute();
				}else if(message instanceof BigDecimal){
					logger.trace("(BigDecimal)message={}", message);
					setProgressBar((BigDecimal)message);
				}else if(message instanceof String){
					logger.trace("(String)message={}", message);
					setMessage((String)message);
				}else
					logger.warn("Not use message={}", message);
			}else if(isVisible()){
				logger.trace("setVisible(false); Text={}", textArea.getText());
				panelProgressBar.setVisible(false);
				setVisible(false);
			}
			return null;
		}

		private void setMessage(String text) throws InterruptedException {
			logger.entry(text);

			textArea.setText(text);
			button.setText("Ok");
			Thread.sleep(1);
			resize();
		}

		private void setMessage(Status[] statuses) throws InterruptedException {
			logger.entry((Object[])statuses);

			for (Status s : statuses){
				if(isCancelled()){
					logger.trace("Break;");
					break;
				}else
					new StatusWorker(s).execute();
			}
		}

		private void setProgressBar(BigDecimal bigDecimal) throws InterruptedException {
			logger.entry(bigDecimal);
			int progress = bigDecimal.multiply(new BigDecimal(100)).intValueExact();
			progressBar.setValue(progress);
			lblProgress.setText(bigDecimal.toString()+" %");
			if(!panelProgressBar.isVisible()){
				panelProgressBar.setVisible(true);
				resize();
			}
		}

		private synchronized void resize() throws InterruptedException {
			Thread.sleep(1);
			pack();

				Rectangle bounds = getOwner().getBounds();
				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

				double appX = bounds.getX();
				double appWidth = bounds.getWidth();
				double tmp = screenSize.getWidth() - (appX + appWidth);
				if (tmp < 0)
					appWidth = appWidth + tmp;
				if (appX < 0)
					appX = 0;
				int x = (int) (appX + (appWidth - getWidth()) / 2);

				double appY = bounds.getY();
				double appHeight = bounds.getHeight();
				tmp = screenSize.getHeight() - appY - appHeight;
				if (tmp < 0)
					appHeight = appHeight + tmp;
				int y = (int) (appY + (appHeight - getHeight()) / 2);

				Thread.sleep(1);
					logger.trace("Location: x={}, y={}", x, y);
					setLocation(x, y);

					if (!isVisible())// Set Visible
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								if (!isCancelled()) {
									logger.trace("setVisible(true)");
									setVisible(true);
									dispose();
								}
							}
						});
		}

		private class StatusWorker extends SwingWorker<Void, Void>{
			Status status;

			public StatusWorker(Status status) {
				this.status = status;
			}

			@Override
			protected Void doInBackground() throws Exception {
				logger.entry(status);

				String text = status.getMessage();
				switch (status) {
				case BUTTON:
					logger.trace("button.setText({});", text);
					button.setText(text);
					break;
				case ERROR:
					logger.trace("textArea.setText({})", text);
					textArea.setText(text);
					button.setText("Ok");
					resize();
					break;
				default:
					if (text != null) {
						logger.trace("textArea.setText({})", text);
						textArea.setText(text);
						resize();
					} else if (isVisible())
						setVisible(false);
				}

				return null;
			}
		}
	}
}
