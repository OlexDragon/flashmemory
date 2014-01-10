package irt.flash.presentation.panel.edit_profile.extendable;

import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;

import java.awt.Color;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public abstract class EditPanel<T> extends JPanel {
	private static final long serialVersionUID = -5075735149795277563L;

	protected final Logger logger = (Logger) LogManager.getLogger(getClass().getName());

	public enum Scope{
		COMMON,
		FCM,
		BUC
	}

	protected Color color;
	protected volatile Set<T> oldValues = new LinkedHashSet<>();
	private final ProfileProperties profileProperties;
	private Scope scope;

	public EditPanel(String title, ProfileProperties profileProperties) throws ClassNotFoundException, SQLException, IOException {
		logger.info("* Start * {}", this);

		color = getBackground();
		this.profileProperties = profileProperties;
		setScope(profileProperties);
		setBorder(new TitledBorder(new LineBorder(new Color(0, 0, 0), 1, true), title, TitledBorder.LEFT, TitledBorder.TOP, getFont().deriveFont(16F), Color.BLUE));
	}

	public ProfileProperties getProfileProperties() {
		return profileProperties;
	}

	public Scope getScope() {
		return scope;
	}

	private void setScope(ProfileProperties profileProperties){
		List<String> properties = profileProperties!=null ? profileProperties.getProperties() : new ArrayList<String>();

		if(properties.contains("FCM"))
			scope = Scope.FCM;
		else if(properties.contains("BUC"))
			scope = Scope.BUC;
		else
			scope = Scope.COMMON;
	}

	protected void setBackground(T value){
		logger.trace("new value={}, old value={}", value, oldValues);
		Object[] array = oldValues.toArray();

		if(array.length==0 || (array[0]!=null ? array[0].equals(value) : value==null))
			setBackground(color);
		else
			setBackground(Color.CYAN);
	}

	public void reset(){
		new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				oldValues.clear();
				setBackground(color);
				return null;
			}
		}.execute();
	}

	public boolean isChanged(){
		return getBackground()!=color;
	}
	public abstract void setValue(Object value);
}
