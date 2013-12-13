package irt.flash.data.connection.dao;

import irt.flash.data.Profile;
import irt.flash.data.ProfileVariable;

import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class DatabaseController extends Thread {

	private static final Logger logger = (Logger) LogManager.getLogger();

	public enum Action{ SET_PROFILE, UPDATE}

	private Action action;
	private Profile profile;
	private String profileStr;
	private static List<ProfileVariable> profileVariables;

	public DatabaseController() {
		setDaemon(true);
		start();
	}

	@Override
	public void run() {

		try {
			Database database = Database.getInstance();
			profileVariables = database.getAllProfileVariables();

			while (true) {
				synchronized (this) { try { wait(); } catch (InterruptedException e) { logger.catching(e); } }

				switch (action) {
				case SET_PROFILE:
					if (profile != null) { database.setProfile(profile); }
					break;
				case UPDATE:
					if(profileStr!=null){
						Profile p = Profile.parseProfile(profileStr);
						database.updateProfile(profile, p);
					}
				}
			}
		} catch (Exception e) {
			logger.catching(e);
		}
	}

	public synchronized void setProfile(Profile profile) {
		action = Action.SET_PROFILE;
		this.profile = profile;
		notify();
	}

	public static List<ProfileVariable> getProfileVariables() {
		return Collections.unmodifiableList(profileVariables);
	}

	public synchronized void update(String profileStr) {
		action = Action.UPDATE;
		this.profileStr = profileStr;
		notify();
	}
}
