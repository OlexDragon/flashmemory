package irt.flash.data.connection.dao;

import irt.flash.data.Profile;
import irt.flash.data.ProfileVariable;
import irt.flash.data.SerialNumber;
import irt.flash.data.Table;
import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class Database {

	private static final Logger logger = (Logger) LogManager.getLogger();

	private static Database database;

	private Properties sqlProperties = new Properties();
	private DatabaseSerialNumbers databaseSerialNumber;
	private DatabaseSerialNumberProfile databaseSerialNumberProfile;

	private DatabaseSerialNumberTable databaseSerialNumberTable;

	public static Database getInstance() throws IOException{
		return database!= null ? database : (database = new Database());
	}

	private Database() throws IOException{
		sqlProperties.load(getClass().getResourceAsStream("sql.properties"));
		databaseSerialNumber = new DatabaseSerialNumbers(sqlProperties);
		databaseSerialNumberProfile = new DatabaseSerialNumberProfile(sqlProperties);
		databaseSerialNumberTable = new DatabaseSerialNumberTable(sqlProperties);
	}

	public long setProfile(String profileStr) throws ClassNotFoundException, SQLException, IOException{
		logger.entry(profileStr);
		long serialNumberId = 0;

		try(Connection connection = MySQLConnector.getConnection()){

			Profile profile = Profile.parseProfile(profileStr);
			String serialNumberStr = profile .getProperty(ProfileProperties.SERIAL_NUMBER.toString());

			if(serialNumberStr!=null){
				serialNumberId = getSerialNumberId(connection, serialNumberStr);
				if(databaseSerialNumber.profileIsChanged(connection, serialNumberId, profileStr)){//profile have been changed
					databaseSerialNumber.setProfile(connection, serialNumberId, profileStr);
					setProfile(connection, serialNumberId, profile);
					setTables(connection, serialNumberId, profile.getTables());
				}
			}else
				logger.warn("Serial Number = NULL");
		}
		return logger.exit(serialNumberId);
	}

	private void setTables(Connection connection, long serialNumberId, List<Table> tables) {
		logger.entry(serialNumberId, tables);

		for(Table t:tables)
			try {

				setTable(connection, serialNumberId, t);

			} catch (Exception e) { logger.catching(e); }

		logger.exit();
	}

	private void setTable(Connection connection, long serialNumberId, Table table) throws SQLException, ClassNotFoundException, IOException {
		logger.entry(serialNumberId, table);

		long profileVariableId = databaseSerialNumberProfile.getProfileVariableId(connection, table.getName());
		TreeMap<BigDecimal, BigDecimal> tableMap = table.getTableMap();
		Set<BigDecimal> keySet = tableMap.keySet();

		for(BigDecimal bd:keySet)
			try {

				databaseSerialNumberTable.set(connection, serialNumberId, profileVariableId, bd, tableMap.get(bd));

			} catch (SQLException e) { logger.catching(e); }
		databaseSerialNumberTable.setActive(connection, serialNumberId, profileVariableId, tableMap);

		logger.exit();
	}

	private void setProfile(Connection connection, long serialNumberId, Profile profile) throws SQLException, ClassNotFoundException, IOException {
		Properties properties = profile.getProperties();
		Set<Object> keySet = properties.keySet();

		for(Object o:keySet)
			try {

				databaseSerialNumberProfile.set(connection, serialNumberId, databaseSerialNumberProfile.getProfileVariableId(connection, (String) o), profile.getProperty((String) o));

			} catch (SQLException e) { logger.catching(e); }

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

	public Long updateProfile(String oldProfileStr, String newProfileStr) throws SQLException, ClassNotFoundException, IOException {
		logger.entry(oldProfileStr, newProfileStr);
		long serialNumberId = 0;

		try(Connection connection = MySQLConnector.getConnection()){

			Profile oldProfile = Profile.parseProfile(oldProfileStr);
			String serialNumberStr = oldProfile.getProperty(ProfileProperties.SERIAL_NUMBER.toString());

			Profile newProfile = Profile.parseProfile(newProfileStr);
			if(serialNumberStr==null) serialNumberStr = newProfile.getProperty(ProfileProperties.SERIAL_NUMBER.toString());

			if (serialNumberStr != null)
				setProfile(connection, (serialNumberId = getSerialNumberId(connection, serialNumberStr)), newProfile);
			else
				logger.warn("Serial Number = NULL");
		}
		return logger.exit(serialNumberId);
	}

	public String getNextSerialNumber(String format) throws ClassNotFoundException, SQLException, IOException {
		return databaseSerialNumber.getSerialNumbersStartWith(format);
	}
}
