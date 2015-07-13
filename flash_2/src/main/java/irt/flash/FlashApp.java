package irt.flash;

import irt.flash.presentation.panel.ConnectionPanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class FlashApp extends JFrame {
	private static final long serialVersionUID = 3813033753839141111L;

	private static final Logger logger = (Logger) LogManager.getLogger();

	protected static final Preferences prefs = Preferences.userRoot().node("IRT Technologies inc.");
	private JPanel contentPane;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					FlashApp frame = new FlashApp();
					frame.setVisible(true);
				} catch (Exception e) {
					logger.throwing(e);
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public FlashApp() {
		logger.info("*** Application Starts ***");

		Runtime.getRuntime().addShutdownHook(new Thread()
		{
		    @Override
		    public void run()
		    {
		        logger.info("ShutdownHook");
		    }
		});

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent arg0) {
				int extendedState = getExtendedState();
				prefs.putInt("ExtendedState", extendedState);
				if(extendedState==Frame.NORMAL){
					Rectangle bounds = getBounds();
					prefs.putInt("x", bounds.x);
					prefs.putInt("y", bounds.y);
					prefs.putInt("width", bounds.width);
					prefs.putInt("height", bounds.height);
				}
			}
		});

		setMinimumSize(new Dimension(470, 250));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 638, 840);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		ConnectionPanel connectionPanel = new ConnectionPanel(this);
		connectionPanel.setMinimumSize(new Dimension(0, 0));
		contentPane.add(connectionPanel, BorderLayout.CENTER);

		int extendedState = prefs.getInt("ExtendedState", Frame.NORMAL);
		setExtendedState(extendedState);
		setBounds(prefs.getInt("x", 100), prefs.getInt("y", 100), prefs.getInt("width", 400), prefs.getInt("height", 100));
	}

}
