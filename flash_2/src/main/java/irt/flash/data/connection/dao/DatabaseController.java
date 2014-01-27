package irt.flash.data.connection.dao;

import irt.flash.data.Profile;
import irt.flash.data.ProfileVariable;
import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;

import java.awt.Component;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.JOptionPane;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class DatabaseController extends Thread {

	private static final Logger logger = (Logger) LogManager.getLogger();

	public enum Action{ SET_PROFILE, UPDATE}

	private Action action;
	private String profileStr;
	private String newProfileStr;
	private Component owner;
	private static List<ProfileVariable> profileVariables;
	private static Map<String, ProfileVariable> profileVariableMap;

	private boolean profileIsNotTheSame;

	public DatabaseController() {
		logger.info("* Start *");
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
					if (profileStr != null) 
						database.setProfile(profileStr);
					break;
				case UPDATE:
					if(profileIsNotTheSame)
						if(profileStr!=null)
							database.updateProfile(profileStr, newProfileStr);
						else
							database.setProfile(newProfileStr);

					profileStr = newProfileStr;
					newProfileStr = null;
				}
			}
		} catch (Exception e) {
			logger.catching(e);
		}
	}

	public static Map<String, ProfileVariable> getProfileVariablesMap() {
		return profileVariableMap!=null ? profileVariableMap : setProfileVariableMap();
	}

	private static Map<String, ProfileVariable> setProfileVariableMap() {
		profileVariableMap = new HashMap<String, ProfileVariable>();

		for(ProfileVariable p:getProfileVariables())
			profileVariableMap.put(p.getName(), p);
			
		return profileVariableMap;
	}

	public static List<ProfileVariable> getProfileVariables() {
		return profileVariables!=null ? Collections.unmodifiableList(profileVariables) : null;
	}

	public static Database setProfileVariables() throws IOException, ClassNotFoundException, SQLException, InterruptedException {
		Database database = Database.getInstance();
		profileVariables = database.getAllProfileVariables();
		return database;
	}

	public synchronized void setProfile(byte[] readBytes) {
		logger.entry(readBytes);

		if(readBytes!=null){
			action = Action.SET_PROFILE;
			profileStr = new String(readBytes);
			notify();
		}

		logger.exit(profileStr);
	}

	public synchronized boolean update(String profileStr) throws InterruptedException, ExecutionException {
		logger.entry(profileStr);

		boolean update = haveToUpdate(profileStr);

		if (profileIsNotTheSame && profileStr != null) {
			action = Action.UPDATE;
			newProfileStr = profileStr;
			notify();
			logger.trace("Action - {}", action);
		}

		return logger.exit(update);
	}

	private boolean haveToUpdate(String profileStr) throws InterruptedException, ExecutionException {
		logger.entry();

		ExecutorService executorService = Executors.newFixedThreadPool(2);

		Future<Profile> submitOld = executorService.submit(Profile.getCallable(this.profileStr));
		Future<Profile> submitNew = executorService.submit(Profile.getCallable(profileStr));

		Profile oldProfile = submitOld.get();
		Profile newProfile =submitNew.get();

		executorService.shutdown();

		boolean haveToUpdate = profileIsNotTheSame = newProfile!=null ? !newProfile.equals(oldProfile) : false;
		String serialNumber = newProfile.getProperty(ProfileProperties.SERIAL_NUMBER.toString());
		if(haveToUpdate){
			if(serialNumber==null || serialNumber.replaceAll("\\D", "").isEmpty() )
				haveToUpdate = JOptionPane.showConfirmDialog(
						owner,
						"The Profile must contains valid serial number.\n"
						+ "\nPress 'OK' to continue\n",
						"Warning",
						JOptionPane.OK_CANCEL_OPTION)
					== JOptionPane.OK_OPTION;
		}else
			haveToUpdate = JOptionPane.showConfirmDialog(
					owner,
					"The profile has not been changed.\nTo continue press 'OK' button.",
					"Warning",
					JOptionPane.OK_CANCEL_OPTION)
				== JOptionPane.OK_OPTION;

		return logger.exit(haveToUpdate);
	}

	public void addObserver(Observer observer) throws IOException {
		Database.getInstance().addObserver(observer);
	}

	public void setOwner(Component owner){
		this.owner = owner;
	}

	public void reset() {
		profileStr = newProfileStr = null;
	}
}
