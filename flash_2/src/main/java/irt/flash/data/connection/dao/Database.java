package irt.flash.data.connection.dao;

import irt.flash.data.Profile;
import irt.flash.data.ProfileVariable;
import irt.flash.data.SerialNumber;
import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class Database {

	private static final Logger logger = (Logger) LogManager.getLogger();

	private static Database database;

	private Properties sqlProperties = new Properties();
	private DatabaseSerialNumbers databaseSerialNumber;
	private DatabaseSerialNumberProfile databaseSerialNumberProfile;

	public static Database getInstance() throws IOException{
		return database!= null ? database : (database = new Database());
	}

	private Database() throws IOException{
		sqlProperties.load(getClass().getResourceAsStream("sql.properties"));
		databaseSerialNumber = new DatabaseSerialNumbers(sqlProperties);
		databaseSerialNumberProfile = new DatabaseSerialNumberProfile(sqlProperties);
	}

	public long setProfile(Profile profile) throws ClassNotFoundException, SQLException, IOException{
		logger.entry(profile);
		long serialNumberId = 0;

		try(Connection connection = MySQLConnector.getConnection()){
			String serialNumberStr = profile.getProperty(ProfileProperties.SERIAL_NUMBER.toString());
			if(serialNumberStr!=null){
				serialNumberId = getSerialNumberId(connection, serialNumberStr);
				setProperties(connection, serialNumberId, profile);
			}else
				logger.warn("Serial Number = NULL");
		}
		return logger.exit(serialNumberId);
	}

	private void setProperties(Connection connection, long serialNumberId, Profile profile) throws SQLException {
		Properties properties = profile.getProperties();
		Set<Object> keySet = properties.keySet();
		for(Object o:keySet){
			try {
				databaseSerialNumberProfile.set(connection, serialNumberId, databaseSerialNumberProfile.getProfileVariableId(connection, (String) o), profile.getProperty((String) o));
			} catch (SQLException e) {
				logger.catching(e);
			}
		}
		databaseSerialNumberProfile.setActive(connection, serialNumberId, keySet);
	}

	private long getSerialNumberId(Connection connection, String serialNumberStr) throws SQLException {
		long serialNumberId;

		SerialNumber serialNumber = databaseSerialNumber.get(connection, serialNumberStr);

		if(serialNumber!=null){
			serialNumberId = serialNumber.getId();
			if(!serialNumber.getSerialNumber().equals(serialNumberStr)){
				serialNumber.setSerialNumber(serialNumberStr);
				databaseSerialNumber.update(connection, serialNumber);
			}
		}else
			serialNumberId = databaseSerialNumber.add(connection, serialNumberStr);
		return serialNumberId;
	}

	public List<ProfileVariable> getAllProfileVariables() throws ClassNotFoundException, SQLException, IOException {
		return databaseSerialNumberProfile.getAllProfileVariables();
	}

	public Long updateProfile(Profile oldProfile, Profile newProfile) throws SQLException, ClassNotFoundException, IOException {
		logger.entry(oldProfile, newProfile);
		long serialNumberId = 0;

		try(Connection connection = MySQLConnector.getConnection()){
			String serialNumberStr = oldProfile.getProperty(ProfileProperties.SERIAL_NUMBER.toString());

			if(serialNumberStr==null)
				serialNumberStr = newProfile.getProperty(ProfileProperties.SERIAL_NUMBER.toString());

			if(serialNumberStr!=null){
				serialNumberId = getSerialNumberId(connection, serialNumberStr);
				setProperties(connection, serialNumberId, newProfile);
			}else
				logger.warn("Serial Number = NULL");
		}
		return logger.exit(serialNumberId);
	}
}
