package irt.flash.data.connection.dao;

import irt.flash.data.Profile;
import irt.flash.data.ProfileVariable;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class DatabaseController extends Thread {

	private static final Logger logger = (Logger) LogManager.getLogger();

	public enum Action{ SET_PROFILE, UPDATE}

	private Action action;
	private String profile;
	private String profileStr;
	private static List<ProfileVariable> profileVariables;

	public DatabaseController() {
		setDaemon(true);
		int priority = getPriority();
		if(priority>Thread.MIN_PRIORITY)
			setPriority(priority-1);
		start();
	}

	@Override
	public void run() {

		try {
			Database database = setProfileVariables();

			while (true) {
				synchronized (this) { try { wait(); } catch (InterruptedException e) { logger.catching(e); } }

				switch (action) {
				case SET_PROFILE:
					if (profile != null) 
						database.setProfile(profile);
					break;
				case UPDATE:
					if(profileStr!=null && !profileStr.equals(profile)){
						database.updateProfile(profile, profileStr);
					}
				}
			}
		} catch (Exception e) {
			logger.catching(e);
		}
	}

	public static List<ProfileVariable> getProfileVariables() {
		return profileVariables!=null ? Collections.unmodifiableList(profileVariables) : null;
	}

	public static Database setProfileVariables() throws IOException, ClassNotFoundException, SQLException {
		Database database = Database.getInstance();
		profileVariables = database.getAllProfileVariables();
		return database;
	}

	public synchronized void setProfile(String profileStr) {
		logger.entry();
		action = Action.SET_PROFILE;
		this.profile = profileStr;
		notify();
		logger.exit();
	}

	public synchronized void update(String profileStr) {
		logger.entry();
		action = Action.UPDATE;
		this.profileStr = profileStr;
		notify();
		logger.exit();
	}
}
