package irt.flash.data.connection.dao;

import irt.flash.data.ProfileVariable;
import irt.flash.data.SerialNumberProfileVariable;
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
		this.sqlProperties = sqlProperties;
	}

	public void set(long serialNumberId, String variableName, String value) throws ClassNotFoundException, SQLException, IOException {
		if(serialNumberId>0 && variableName!=null && !variableName.isEmpty() && value!=null && !value.isEmpty()){
			try(Connection connection = MySQLConnector.getConnection()){
				long variableId = getProfileVariableId(connection, variableName);
				if(variableId>0)
					set(connection, serialNumberId, variableId, value);
			}
		}
	}

	public void set(Connection connection, Long serialNumberId, Long variableId, String value) throws SQLException {

		String sql = sqlProperties.getProperty("active_serial_number_profile_variable");
		logger.entry(sql);

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

		boolean equals = false; 
		for(SerialNumberProfileVariable v:snpv)
			if(!(equals=v.getVariableValue().equals(value)))
				setActive(connection, v.getId(), false);

		if(!equals)
			add(connection, serialNumberId, variableId, value, true);
		logger.exit();
	}

	public void add(Connection connection, Long serialNumberId, Long profileVariableId, String value, boolean active) throws SQLException {

		String sql = sqlProperties.getProperty("insert_serial_number_profile_variable");
		logger.trace(sql);
		
		try(PreparedStatement statement = connection.prepareStatement(sql)){
			statement.setLong(1, serialNumberId);
			statement.setLong(2, profileVariableId);
			statement.setString(3, value);
			statement.setInt(4, active ? 1 : 0);
			statement.executeUpdate();
		}
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

	public void setActive(Connection connection, long serialNumberId, Set<Object> activeProfileVariables) throws SQLException {
		logger.entry(serialNumberId, activeProfileVariables);

		//get not used variables
		List<UnitProfileVariable> vs = getActiveVariables(connection, serialNumberId);

		long profileVariableId;

		for(Object o:activeProfileVariables){
			profileVariableId = getProfileVariableId(connection, (String)o);
			vs.remove(new ProfileVariable(profileVariableId, (String) o, null));
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
		snpv.setStatusChangeDate(resultSet.getTimestamp("status_change_date"));
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

	public long getProfileVariableId(Connection connection, String variableName) throws SQLException {

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
		}
		return variableId;
	}

	public List<ProfileVariable> getAllProfileVariables() throws ClassNotFoundException, SQLException, IOException {

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
																	resultSet.getString("description")));
						}while(resultSet.next());
					}
				}
			}
		}
		return profileVariables;
	}
}
