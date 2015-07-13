package irt.flash.data.connection.dao;

import irt.flash.data.DeviceType;
import irt.flash.data.Profile;
import irt.flash.data.ProfileVariable;
import irt.flash.data.SerialNumber;
import irt.flash.data.Table;
import irt.flash.data.UnitType;
import irt.flash.data.ValueDescription;
import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;
import irt.flash.presentation.panel.ConnectionPanel;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Observable;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
		logger.info("* Start *");
		defaultProperties.load(getClass().getResourceAsStream("default.properties"));
		sqlProperties.load(getClass().getResourceAsStream("sql.properties"));
		databaseSerialNumber = new DatabaseSerialNumbers(sqlProperties);
		databaseSerialNumberProfile = new DatabaseSerialNumberProfile(sqlProperties);
		databaseSerialNumberTable = new DatabaseSerialNumberTable(sqlProperties);
		databaseDeviceTypes = new DatabaseDeviceTypes(sqlProperties);
	}

	public SerialNumber setProfile(String profileStr) throws ClassNotFoundException, SQLException, IOException, InterruptedException{
		logger.entry(profileStr);
		SerialNumber serialNumber = null;

		try(Connection connection = MySQLConnector.getConnection()){

			if(connection!=null){
			Profile profile = Profile.parseProfile(profileStr);

			notifyObservers(profile);

			String serialNumberStr = profile.getProperty(ProfileProperties.SERIAL_NUMBER.toString());
			Timestamp unitProfileChangeDate = getUploadDate(profileStr);

			if(serialNumberStr!=null && !serialNumberStr.toUpperCase().contains("X")){
				serialNumber = getSerialNumber(connection, serialNumberStr);
				Timestamp dbProfileChangeDate = serialNumber.getProfileChangeDate();
				logger.debug("dbProfileChangeDate={}, unitUploadDate={}", dbProfileChangeDate, unitProfileChangeDate);

				if(isCorrectDate(unitProfileChangeDate, dbProfileChangeDate)){//10*1000 = 10 minutes
					long id = serialNumber.getId();
					logger.debug("Serial Number ID={}", id);
					if(!profilesAreEqual(connection, serialNumber.getProfile(), profile)){//profile have been changed
						logger.info("Storing the Profile in the Database.");
						databaseSerialNumber.setProfile(connection, id, profileStr);
						setProfile(connection, id, profile.getProperties());
						setTables(connection, id, profile.getTables());
					}
				}
			}else
				logger.warn("Serial Number = NULL");
			}
		}
		return logger.exit(serialNumber);
	}

	// true - if unitProfileChangeDate < dbProfileChangeDate+10min
	private boolean isCorrectDate(Timestamp unitProfileChangeDate, Timestamp dbProfileChangeDate) {
		return unitProfileChangeDate==null || dbProfileChangeDate==null || dbProfileChangeDate.getTime()<unitProfileChangeDate.getTime()+10*1000;
	}

	private boolean profilesAreEqual(Connection connection, String profile1, Profile profile2) {

		Profile parseProfile1 = Profile.parseProfile(profile1);
		boolean result;

		if(parseProfile1!=null){
			parseProfile1.setCompareByName(false);
			result = parseProfile1.equals(profile2);
		}else
			result =  profile2==null;

		return result;
	}

	private Timestamp getUploadDate(String profileStr) {
		final String KEY = ConnectionPanel.UPLOAD_DATE;

		int lastIndexOf = profileStr.lastIndexOf(KEY);
		if(lastIndexOf>=0)
			profileStr = profileStr.substring(lastIndexOf+KEY.length());
		else
			profileStr = "1978-01-01 00:00:00";

		logger.debug("lastIndexOf={}, Date={}", lastIndexOf, profileStr);

		return Timestamp.valueOf(profileStr);
	}

	private void notifyObservers(Profile profile) {
		setChanged();
		super.notifyObservers(profile);
	}

	public SerialNumber updateProfile(final String oldProfileStr, final String newProfileStr) throws SQLException, ClassNotFoundException, IOException, InterruptedException, ExecutionException {
		logger.entry(oldProfileStr, newProfileStr);
						ExecutorService executorService = Executors.newFixedThreadPool(2);
						Future<Profile> submitOld = executorService.submit(Profile.getCallable(oldProfileStr));
						Future<Profile> submitNew = executorService.submit(Profile.getCallable(newProfileStr));
		Profile oldProfile = submitOld.get();
		Profile newProfile =submitNew.get();
						executorService.shutdown();

		SerialNumber serialNumber = null;

		if(newProfile!=null)
		try(Connection connection = MySQLConnector.getConnection()){

			if(connection!=null){
			String serialNumberStr = null;
			String profileProperty_SERIAL_NUMBER = ProfileProperties.SERIAL_NUMBER.toString();

			if(oldProfile!=null)
				serialNumberStr = oldProfile.getProperty(profileProperty_SERIAL_NUMBER);

			String newSerialNumberStr = null;
			if(serialNumberStr==null)
				serialNumberStr = newProfile.getProperty(profileProperty_SERIAL_NUMBER);
			else
				newSerialNumberStr = newProfile.getProperty(profileProperty_SERIAL_NUMBER);

			if (serialNumberStr != null && !serialNumberStr.replaceAll("\\D", "").isEmpty()){

				if (!serialNumberStr.toUpperCase().contains("X")) {

					serialNumber = getSerialNumber(connection, serialNumberStr, newSerialNumberStr, getUploadDate(oldProfileStr));

					if(serialNumber!=null){
						long id = serialNumber.getId();

						if(id>0){
							databaseSerialNumber.setProfile(connection, id, newProfileStr);
							setProfile(connection, id, newProfile.getProperties());
							setTables(connection, id, newProfile.getTables());
						}
					}else
						logger.warn("Serial Number is not correct({})", serialNumberStr);
				notifyObservers(newProfile);
				}
			}else{
				logger.warn("Serial Number is not correct({})", serialNumberStr);
			}
			}
		}
		return logger.exit(serialNumber);
	}

	private void setTables(Connection connection, long serialNumberId, List<Table> tables) {
		logger.entry(serialNumberId, tables);

		for(Table t:tables)
			try {
				setTable(connection, serialNumberId, t);

			} catch (Exception e) { logger.catching(e); }

		logger.exit();
	}

	private void setTable(Connection connection, long serialNumberId, Table table) throws SQLException, ClassNotFoundException, IOException, InterruptedException {
		logger.entry(serialNumberId, table);

		long profileVariableId = databaseSerialNumberProfile.getProfileVariableId(connection, table.getName());
		TreeMap<BigDecimal, BigDecimal> tableMap = table.getTableMap();
		Set<BigDecimal> keySet = tableMap.keySet();

		if(profileVariableId>0){
			for(BigDecimal bd:keySet)
				try {

					databaseSerialNumberTable.setTableRow(connection, serialNumberId, profileVariableId, bd, tableMap.get(bd));

				} catch (SQLException e) { logger.catching(e); }

			databaseSerialNumberTable.setNotActive(connection, serialNumberId, profileVariableId, tableMap);
		}

		logger.exit();
	}

	private void setProfile(Connection connection, long serialNumberId, Properties properties) throws SQLException, ClassNotFoundException, IOException, InterruptedException {
		Set<Object> keySet = properties.keySet();

		for(Object o:keySet)
			try {
				String key = (String) o;
				databaseSerialNumberProfile.set(connection,
												serialNumberId,
												databaseSerialNumberProfile.getProfileVariableId(connection, key),
												properties.getProperty(key)
						);
			} catch (SQLException e) { logger.catching(e); }

		databaseSerialNumberProfile.setActive(connection, serialNumberId, keySet);
	}

	private SerialNumber getSerialNumber(Connection connection, String serialNumberStr) throws SQLException{
		return getSerialNumber(connection, serialNumberStr, null, null);
		
	}

	private SerialNumber getSerialNumber(Connection connection, String serialNumberStr, String newSerialNumberStr, Timestamp profileUploadDate) throws SQLException {
		logger.entry(serialNumberStr, newSerialNumberStr);

		SerialNumber serialNumber = null;
		if(serialNumberStr!=null){
			serialNumber = databaseSerialNumber.get(connection, serialNumberStr);
			if(serialNumber==null)
				return databaseSerialNumber.add(connection, serialNumberStr);
		}

		SerialNumber newSerialNumber = null;
		if(newSerialNumberStr!=null){
			newSerialNumber = databaseSerialNumber.get(connection, newSerialNumberStr);
			if(serialNumber==null)
				if(newSerialNumber==null)
					return databaseSerialNumber.add(connection, newSerialNumberStr);
				else
					return newSerialNumber;
			else{
				if(profileUploadDate!=null && serialNumber.getProfileChangeDate().getTime()-10*1000 > profileUploadDate.getTime())//if PCB profile is older then datadase
					if(newSerialNumber==null)
						return databaseSerialNumber.add(connection, newSerialNumberStr);
					else{
						if(!serialNumber.getSerialNumber().equals(newSerialNumberStr))
							databaseSerialNumber.update(connection, serialNumber.setSerialNumber(newSerialNumberStr));
						return newSerialNumber;
					}

				if (databaseSerialNumber.update(connection, newSerialNumber != null
						? newSerialNumber
								: new SerialNumber().setId(serialNumber.getId()).setSerialNumber(newSerialNumberStr))
					> 0)
					return newSerialNumber;
			}
		}

		if(!serialNumber.getSerialNumber().equals(serialNumberStr))
			databaseSerialNumber.update(connection, serialNumber.setSerialNumber(serialNumberStr));

		return serialNumber;
	}

	public List<ProfileVariable> getAllProfileVariables() throws ClassNotFoundException, SQLException, IOException, InterruptedException {
		return databaseSerialNumberProfile.getAllProfileVariables();
	}

	public String getNextSerialNumber(String yerWeek) throws ClassNotFoundException, SQLException, IOException, InterruptedException {
		return databaseSerialNumber.getNextSerialNumber(yerWeek);
	}

	public String getNextMacAddress() throws ClassNotFoundException, SQLException, IOException, InterruptedException {
		return databaseSerialNumberProfile.getNextMacAddress();
	}

	public static Properties getDefaultProperties() throws IOException {
		return getInstance().defaultProperties;
	}

	public static List<DeviceType> getDeviceTypes(String unitType) throws ClassNotFoundException, SQLException, IOException, InterruptedException {
		return getInstance().databaseDeviceTypes.getTypes(unitType);
	}

	public static List<DeviceType> getDeviceSubtypes() throws ClassNotFoundException, SQLException, IOException, InterruptedException {
		return getInstance().databaseDeviceTypes.getSubtypes();
	}

	public static List<String> getProductDescriptions(int deviceType, int subtype) throws IOException, ClassNotFoundException, SQLException, InterruptedException {
		return getInstance().databaseSerialNumberProfile.getValues(deviceType, subtype, 3);
	}

	public static List<String> getPartNumbers(int deviceType, int subtype) throws ClassNotFoundException, SQLException, IOException, InterruptedException {
		return getInstance().databaseSerialNumberProfile.getValues(deviceType, subtype, 4);
	}

	public static List<String> getPartNumbers(String description) throws IOException, ClassNotFoundException, SQLException, InterruptedException {
		return getInstance().databaseSerialNumberProfile.getValues(4, 3, description);
	}

	public static List<String> getDescriptions(String partNumber) throws IOException, ClassNotFoundException, SQLException, InterruptedException {
		return getInstance().databaseSerialNumberProfile.getValues(3, 4, partNumber);
	}

	public static List<ValueDescription> getPowerDetectorSources() throws ClassNotFoundException, SQLException, IOException, InterruptedException {
		return getInstance().databaseDeviceTypes.getProfileVariablesPossibleValues(9);
	}

	public static List<String> getSystemNameByPartNumber(String partNumber) throws ClassNotFoundException, SQLException, IOException, InterruptedException {
		return getInstance().databaseSerialNumberProfile.getValues(15, 4, partNumber);
	}

	public static List<String> getSystemNameByDescription(String description) throws ClassNotFoundException, SQLException, IOException, InterruptedException {
		return getInstance().databaseSerialNumberProfile.getValues(15, 3, description);
	}

	public static UnitType getUnitType(String unitTypeStr) throws ClassNotFoundException, SQLException, IOException, InterruptedException {
		return getInstance().databaseDeviceTypes.getUnitType(unitTypeStr);
	}

	public static ProfileVariable getProfileVariavle(String profileVariableStr) throws ClassNotFoundException, SQLException, IOException, InterruptedException {
		return getInstance().databaseDeviceTypes.getProfileVariavle(profileVariableStr);
	}

	public static String getProfileHeader(DeviceType deviceType) throws Exception {
		return getInstance().getProfileHeader(deviceType.getDeviceType());
	}

	private String getProfileHeader(int deviceType) throws Exception {
		logger.entry(deviceType);
		String result = null;

		String sql = sqlProperties.getProperty("select_profile_header");
		logger.entry(sql);

		try(Connection connection = MySQLConnector.getConnection()){
			if(connection!=null){
			try(PreparedStatement statement = connection.prepareStatement(sql)){
				statement.setInt(1, deviceType);
				try(ResultSet resultSet = statement.executeQuery()){
					if(resultSet.next()){
						result = resultSet.getString(1);
						do{
							result += resultSet.getString(1);
						}while(resultSet.next());
					}
				}
			}
			}
		}
		return logger.exit(result);
	}
}
