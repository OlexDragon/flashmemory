package irt.flash.data.connection.dao;

import irt.flash.data.DeviceType;
import irt.flash.data.Profile;
import irt.flash.data.ProfileVariable;
import irt.flash.data.SerialNumber;
import irt.flash.data.Table;
import irt.flash.data.UnitType;
import irt.flash.data.ValueDescription;
import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Observable;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class Database extends Observable {

	private static final Logger logger = (Logger) LogManager.getLogger();

	private static Database database;

	private final Properties defaultProperties = new Properties();
	private final Properties sqlProperties = new Properties();

	private DatabaseSerialNumbers		databaseSerialNumber;
	private DatabaseSerialNumberProfile databaseSerialNumberProfile;
	private DatabaseSerialNumberTable	databaseSerialNumberTable;
	private DatabaseDeviceTypes			databaseDeviceTypes;

	public static Database getInstance() throws IOException{
		return database!= null ? database : (database = new Database());
	}

	private Database() throws IOException{
		defaultProperties.load(getClass().getResourceAsStream("default.properties"));
		sqlProperties.load(getClass().getResourceAsStream("sql.properties"));
		databaseSerialNumber = new DatabaseSerialNumbers(sqlProperties);
		databaseSerialNumberProfile = new DatabaseSerialNumberProfile(sqlProperties);
		databaseSerialNumberTable = new DatabaseSerialNumberTable(sqlProperties);
		databaseDeviceTypes = new DatabaseDeviceTypes(sqlProperties);
	}

	public long setProfile(String profileStr) throws ClassNotFoundException, SQLException, IOException{
		logger.entry(profileStr);
		long serialNumberId = 0;

		try(Connection connection = MySQLConnector.getConnection()){

			Profile profile = Profile.parseProfile(profileStr);

			notifyObservers(profile);

			String serialNumberStr = profile.getProperty(ProfileProperties.SERIAL_NUMBER.toString());

			if(serialNumberStr!=null && !serialNumberStr.toUpperCase().contains("X")){
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

	private void notifyObservers(Profile profile) {
		setChanged();
		super.notifyObservers(profile);
	}

	public Long updateProfile(String oldProfileStr, String newProfileStr) throws SQLException, ClassNotFoundException, IOException {
		logger.entry(oldProfileStr, newProfileStr);
		long serialNumberId = 0;

		try(Connection connection = MySQLConnector.getConnection()){

			Profile oldProfile = Profile.parseProfile(oldProfileStr);
			String serialNumberStr = oldProfile.getProperty(ProfileProperties.SERIAL_NUMBER.toString());

			Profile newProfile = Profile.parseProfile(newProfileStr);

			notifyObservers(newProfile);

			if(serialNumberStr==null)
				serialNumberStr = newProfile.getProperty(ProfileProperties.SERIAL_NUMBER.toString());
			else if(oldProfile.equals(newProfile)){
				logger.trace("Profiles are the same. serialNumberStr set to 'null'");
				serialNumberStr = null;
			}

			if (serialNumberStr != null && !serialNumberStr.toUpperCase().contains("X")){
				serialNumberId = getSerialNumberId(connection, serialNumberStr);
				databaseSerialNumber.setProfile(connection, serialNumberId, newProfileStr);
				setProfile(connection, (serialNumberId = getSerialNumberId(connection, serialNumberStr)), newProfile);
				setTables(connection, serialNumberId, newProfile.getTables());
			
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

	public String getNextSerialNumber(String yerWeek) throws ClassNotFoundException, SQLException, IOException {
		return databaseSerialNumber.getNextSerialNumber(yerWeek);
	}

	public String getNextMacAddress() throws ClassNotFoundException, SQLException, IOException {
		return databaseSerialNumberProfile.getNextMacAddress();
	}

	public static Properties getDefaultProperties() throws IOException {
		return getInstance().defaultProperties;
	}

	public static List<DeviceType> getDeviceTypes(String unitType) throws ClassNotFoundException, SQLException, IOException {
		return getInstance().databaseDeviceTypes.getTypes(unitType);
	}

	public static List<DeviceType> getDeviceSubtypes() throws ClassNotFoundException, SQLException, IOException {
		return getInstance().databaseDeviceTypes.getSubtypes();
	}

	public static List<String> getProductDescriptions(int deviceType, int subtype) throws IOException, ClassNotFoundException, SQLException {
		return getInstance().databaseSerialNumberProfile.getValues(deviceType, subtype, 3);
	}

	public static List<String> getPartNumbers(int deviceType, int subtype) throws ClassNotFoundException, SQLException, IOException {
		return getInstance().databaseSerialNumberProfile.getValues(deviceType, subtype, 4);
	}

	public static List<String> getPartNumbers(String description) throws IOException, ClassNotFoundException, SQLException {
		return getInstance().databaseSerialNumberProfile.getValues(4, 3, description);
	}

	public static List<String> getDescriptions(String partNumber) throws IOException, ClassNotFoundException, SQLException {
		return getInstance().databaseSerialNumberProfile.getValues(3, 4, partNumber);
	}

	public static List<ValueDescription> getPowerDetectorSources() throws ClassNotFoundException, SQLException, IOException {
		return getInstance().databaseDeviceTypes.getProfileVariablesPossibleValues(9);
	}

	public static List<String> getSystemNameByPartNumber(String partNumber) throws ClassNotFoundException, SQLException, IOException {
		return getInstance().databaseSerialNumberProfile.getValues(15, 4, partNumber);
	}

	public static List<String> getSystemNameByDescription(String description) throws ClassNotFoundException, SQLException, IOException {
		return getInstance().databaseSerialNumberProfile.getValues(15, 3, description);
	}

	public static UnitType getUnitType(String unitTypeStr) throws ClassNotFoundException, SQLException, IOException {
		return getInstance().databaseDeviceTypes.getUnitType(unitTypeStr);
	}

	public static ProfileVariable getProfileVariavle(String profileVariableStr) throws ClassNotFoundException, SQLException, IOException {
		return getInstance().databaseDeviceTypes.getProfileVariavle(profileVariableStr);
	}
}
