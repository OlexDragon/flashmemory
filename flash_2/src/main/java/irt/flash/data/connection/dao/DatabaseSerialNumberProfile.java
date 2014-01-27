package irt.flash.data.connection.dao;

import irt.flash.data.ProfileVariable;
import irt.flash.data.SerialNumberProfileVariable;
import irt.flash.data.ToHex;
import irt.flash.data.UnitProfileVariable;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class DatabaseSerialNumberProfile {

	private static final Logger logger = (Logger) LogManager.getLogger();

	private Properties sqlProperties;

	public DatabaseSerialNumberProfile(Properties sqlProperties){
		logger.info("* Start *");
		this.sqlProperties = sqlProperties;
	}

	public void set(long serialNumberId, String variableName, String value) throws ClassNotFoundException, SQLException, IOException, InterruptedException {
		if(serialNumberId>0 && variableName!=null && !variableName.isEmpty() && value!=null && !value.isEmpty()){
			try(Connection connection = MySQLConnector.getConnection()){
				long variableId = getProfileVariableId(connection, variableName);
				if(variableId>0)
					set(connection, serialNumberId, variableId, value);
			}
		}
	}

	public void set(Connection connection, Long serialNumberId, Long variableId, String value) throws SQLException {

		String sql = sqlProperties.getProperty("select_serial_number_profile_variable");
		logger.entry(serialNumberId, variableId, value, sql);

		List<SerialNumberProfileVariable> snpv = new ArrayList<>();

		try(PreparedStatement statement = connection.prepareStatement(sql)){
			statement.setLong(1, serialNumberId);
			statement.setLong(2, variableId);
			try(ResultSet resultSet = statement.executeQuery()){

				while(resultSet.next())
					snpv.add(get(resultSet));
			}
		}

		logger.trace("{}", snpv);

		boolean haveToAdd = true; 
		for(SerialNumberProfileVariable v:snpv)
			if(v.getVariableValue().equals(value)){
				if(!v.isStatus())
					setActive(connection, v.getId(), true);
				haveToAdd = false;
			}else
				setActive(connection, v.getId(), false);

		if(haveToAdd)
			addValue(connection, serialNumberId, variableId, value, true);
		logger.exit();
	}

	public int addValue(Connection connection, Long serialNumberId, Long profileVariableId, String value, boolean active) throws SQLException {
		logger.entry(serialNumberId, profileVariableId, value);

		String sql = sqlProperties.getProperty("insert_serial_number_profile_variable");
		logger.trace(sql);

		int executeUpdate = 0;
		try(PreparedStatement statement = connection.prepareStatement(sql)){
			statement.setLong(1, serialNumberId);
			statement.setLong(2, profileVariableId);
			statement.setString(3, value);
			statement.setInt(4, active ? 1 : 0);
			executeUpdate = statement.executeUpdate();
		}
		return logger.exit(executeUpdate);
	}

	public void setActive(Connection connection, long id, boolean active) throws SQLException {
		logger.entry(id, active);

		String sql = sqlProperties.getProperty("update_active_serial_number_profile_variable");
		logger.trace(sql);

		try(PreparedStatement statement = connection.prepareStatement(sql)){
			statement.setInt(1, active ? 1 : 0);
			statement.setLong(2, id);
			statement.executeUpdate();
		}
		logger.exit();
	}

	public void setActive(Connection connection, long serialNumberId, Set<Object> activeProfileVariables) throws SQLException, ClassNotFoundException, IOException, InterruptedException {
		logger.entry(serialNumberId, activeProfileVariables);

		//get not used variables
		List<UnitProfileVariable> vs = getActiveVariables(connection, serialNumberId);

		long profileVariableId;

		for(Object o:activeProfileVariables){
			profileVariableId = getProfileVariableId(connection, (String)o);
			vs.remove(new ProfileVariable(profileVariableId, (String) o, 0, null));
			logger.trace("for loop: {}, {}", profileVariableId, (String) o);
		}
		logger.trace("left vs={}", vs);
		//set not used variable to 'Not Active'
		for(UnitProfileVariable v:vs)
			setActive(connection, v.getRowId(), false);

		logger.exit();
	}

	private List<UnitProfileVariable> getActiveVariables(Connection connection, long serialNumberId) throws SQLException {

		String sql = sqlProperties.getProperty("active_serial_number_profile_variables");
		logger.entry(serialNumberId, sql);
		
		List<UnitProfileVariable> profileVariables = null;
		try(PreparedStatement statement = connection.prepareStatement(sql)){
			statement.setLong(1, serialNumberId);
			try(ResultSet resultSet = statement.executeQuery()){
				if(resultSet.next()){
					profileVariables = new ArrayList<>();
					do{
						profileVariables.add(new UnitProfileVariable(resultSet.getLong("row_id"),
																	resultSet.getLong("id"),
																	resultSet.getString("variable_name"),
																	resultSet.getInt("scope"),
																	resultSet.getString("description")));
					}while(resultSet.next());
				}
			}
		}
		return logger.exit(profileVariables);
	}

	private SerialNumberProfileVariable get(ResultSet resultSet) throws SQLException {
		SerialNumberProfileVariable snpv = new SerialNumberProfileVariable();
		snpv.setId(resultSet.getLong("id"));
		snpv.setSerialNumberId(resultSet.getLong("id_serial_numbers"));
		snpv.setVariableId(resultSet.getLong("id_profile_variables"));
		snpv.setVariableValue(resultSet.getString("value"));
		snpv.setStatus(resultSet.getBoolean("status"));
		snpv.setDate(resultSet.getTimestamp("date"));
		snpv.setStatusChangeDate(resultSet.getTimestamp("last_status_update"));
		return snpv;
	}

	private long setProfileVariable(Connection connection, String variableName) throws SQLException {

		String sql = sqlProperties.getProperty("insert_profile_variable");
		logger.trace(sql);

		long variableId = 0;
		try(PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)){
			statement.setString(1, variableName);
			if(statement.executeUpdate()>0){
				ResultSet generatedKeys = statement.getGeneratedKeys();
				if(generatedKeys.next())
					variableId = generatedKeys.getLong(1);
			}
		}
		return variableId;
	}

	public long getProfileVariableId(Connection connection, String variableName) throws SQLException, ClassNotFoundException, IOException, InterruptedException {
		logger.entry(variableName);

		if(variableName==null || variableName.isEmpty()){
			logger.warn("variableName = NULL or empty");
			return 0;
		}

		long variableId = 0;
		List<ProfileVariable> profileVariables = DatabaseController.getProfileVariables();

		if (profileVariables == null) {
			String sql = sqlProperties.getProperty("select_profile_variable_id");
			logger.trace(sql);

			try (PreparedStatement statement = connection.prepareStatement(sql)) {
				statement.setString(1, variableName);
				try (ResultSet resultSet = statement.executeQuery()) {
					if (resultSet.next())
						variableId = resultSet.getLong(1);
					else
						variableId = setProfileVariable(connection, variableName);
				}
			}
		}else{
			for(ProfileVariable v:profileVariables)
				if(v.getName().equals(variableName)){
					variableId = v.getId();
				}
			if(variableId<=0){
				variableId = setProfileVariable(connection, variableName);
				DatabaseController.setProfileVariables();
			}
		}
		return logger.exit(variableId);
	}

	public List<ProfileVariable> getAllProfileVariables() throws ClassNotFoundException, SQLException, IOException, InterruptedException {

		String sql = sqlProperties.getProperty("profile_variables");
		logger.trace(sql);

		List<ProfileVariable> profileVariables = null;
		try (Connection connection = MySQLConnector.getConnection()) {
			try (PreparedStatement statement = connection.prepareStatement(sql)) {
				try(ResultSet resultSet = statement.executeQuery()){
					if(resultSet.next()){
						profileVariables = new ArrayList<>();
						do{
							profileVariables.add(new ProfileVariable(resultSet.getLong("id"),
																	resultSet.getString("variable_name"),
																	resultSet.getInt("scope"),
																	resultSet.getString("description")));
						}while(resultSet.next());
					}
				}
			}
		}
		return profileVariables;
	}

	public String getNextMacAddress() throws ClassNotFoundException, SQLException, IOException, InterruptedException {

		String sql = sqlProperties.getProperty("all_mac_addresses");
		logger.entry(sql);

		String macAddress;
		try (Connection connection = MySQLConnector.getConnection()) {
			try (PreparedStatement statement = connection.prepareStatement(sql)) {
				try(ResultSet resultSet = statement.executeQuery()){
					List<Integer> macAddresses = new ArrayList<>();
					while(resultSet.next()){
						String string = resultSet.getString("mac_address");
						logger.trace(string);
						int lastIndexOf = string.lastIndexOf(':');
						macAddresses.add(new Integer(ToHex.parseToByte(string.substring(++lastIndexOf))));
					}
					int tmp = 0;
					if(!macAddresses.isEmpty()){
						int size = macAddresses.size();
						if (size < 256) {
							int ma = macAddresses.get(0);
							for (int i = 1; i < size; i++) {
								tmp = ma + i;
								logger.trace("{}+{}={}", ma, i, tmp);
								if (!macAddresses.contains(tmp)) {
									break;
								}
							}
						}else
							tmp = macAddresses.get(255);
					}
					macAddress = (String) Database.getDefaultProperties().getProperty("mac_address");
					macAddress += ToHex.byteToHex((byte)tmp);
				}
			}
		}
		return logger.exit(macAddress);
	}

	//SELECT DISTINCT`values`.`value`FROM`lab`.`serial_number_profile`AS`values`
	//JOIN
	//`lab`.`serial_number_profile` AS `device_types`
	//ON
	//`device_types`.`id_serial_numbers` = `values`.`id_serial_numbers`
	//AND
	//`device_types`.`id_profile_variables` = ?
	//AND
	//`device_types`.`value` = ?
	//JOIN
	//`lab`.`serial_number_profile`AS`device_subtypes`
	//ON`device_subtypes`.`id_serial_numbers`=`values`.`id_serial_numbers`
	//AND
	//`device_subtypes`.`id_profile_variables` = 5
	//AND `device_subtypes`.`value` = ?
	//WHERE
	//`values`.`status`=TRUE
	//AND
	//`values`.`id_profile_variables` = 3
	//ORDER BY `values`.`value`
	public List<String> getValues(int deviceType, int deviceSubtype, int profileVariableId) throws ClassNotFoundException, SQLException, IOException, InterruptedException {
		List<String> values = null;

		if (deviceType != 0 && deviceSubtype != 0 && profileVariableId != 0) {

			String sql = sqlProperties.getProperty("select_variables_for_device_type");
			logger.entry(deviceType, deviceSubtype, profileVariableId, sql);

			try (Connection connection = MySQLConnector.getConnection()) {
				try (PreparedStatement statement = connection.prepareStatement(sql)) {
					statement.setInt(1, deviceType);
					statement.setInt(2, deviceSubtype);
					statement.setInt(3, profileVariableId);
					try (ResultSet resultSet = statement.executeQuery()) {
						if (resultSet.next()) {
							values = new ArrayList<>();
							do {
								values.add(resultSet.getString("value"));
							} while (resultSet.next());
						}
					}
				}
			}
		}
		return logger.exit(values);
	}

	public List<String> getValues(int profileVariableIdToGet, int valueProfileVariableId, String value) throws ClassNotFoundException, SQLException, IOException, InterruptedException {
		List<String> values = null;

		String sql = sqlProperties.getProperty("select_value_where_value");
		logger.entry(profileVariableIdToGet, valueProfileVariableId, value, sql);

		try(Connection connection = MySQLConnector.getConnection()){
			try(PreparedStatement statement = connection.prepareStatement(sql)){
				statement.setInt(1, profileVariableIdToGet);
				statement.setInt(2, valueProfileVariableId);
				statement.setString(3, value);
				try(ResultSet resultSet = statement.executeQuery()){
					if(resultSet.next()){
						values = new ArrayList<>();
						do{
							values.add(resultSet.getString("value"));
						}while(resultSet.next());
					}
				}
			}
		}
		return logger.exit(values);
	}
}
