package irt.flash.data.connection.dao;

import irt.flash.data.DeviceType;
import irt.flash.data.ProfileVariable;
import irt.flash.data.UnitType;
import irt.flash.data.ValueDescription;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class DatabaseDeviceTypes {

	private static final Logger logger = (Logger) LogManager.getLogger();

	private Properties sqlProperties;

	public DatabaseDeviceTypes(Properties sqlProperties){
		logger.info("* Start *");
		this.sqlProperties = sqlProperties;
	}

	public List<DeviceType> getTypes(String unitType) throws ClassNotFoundException, SQLException, IOException, InterruptedException {
		List<DeviceType> deviceTypes = null;

		String sql = sqlProperties.getProperty("select_device_types");
		logger.entry(unitType, sql);

		try(Connection connection = MySQLConnector.getConnection()){
			if(connection!=null)
			try(PreparedStatement statement = connection.prepareStatement(sql)){
				statement.setString(1, unitType);
				try(ResultSet resultSet = statement.executeQuery()){
					if(resultSet.next()){
						deviceTypes = new ArrayList<>();
						do{
							deviceTypes.add(new DeviceType(resultSet.getInt("id"), resultSet.getString("description")));
						}while(resultSet.next());
					}
				}
			}
		}
		return logger.exit(deviceTypes);
	}

	public List<DeviceType> getSubtypes() throws SQLException, ClassNotFoundException, IOException, InterruptedException {
		List<DeviceType> deviceTypes = null;

		String sql = sqlProperties.getProperty("select_device_subtypes");
		logger.entry(sql);

		try(Connection connection = MySQLConnector.getConnection()){
			if(connection!=null)
			try(PreparedStatement statement = connection.prepareStatement(sql)){
				try(ResultSet resultSet = statement.executeQuery()){
					if(resultSet.next()){
						deviceTypes = new ArrayList<>();
						do{
							deviceTypes.add(new DeviceType(resultSet.getInt("id"), resultSet.getString("description")));
						}while(resultSet.next());
					}
				}
			}
		}
		return logger.exit(deviceTypes);
	}

	public List<ValueDescription> getProfileVariablesPossibleValues(long profileVariableId) throws ClassNotFoundException, SQLException, IOException, InterruptedException {
		List<ValueDescription> valueDescription = null;

		String sql = sqlProperties.getProperty("select_profile_variables_possible_values");
		logger.entry(sql);

		try(Connection connection = MySQLConnector.getConnection()){
			if(connection!=null)
			try(PreparedStatement statement = connection.prepareStatement(sql)){
				statement.setLong(1, profileVariableId);
				try(ResultSet resultSet = statement.executeQuery()){
					if(resultSet.next()){
						valueDescription = new ArrayList<>();
						do{
							valueDescription.add(new ValueDescription(resultSet.getString("value"), resultSet.getString("description")));
						}while(resultSet.next());
					}
				}
			}
		}
		return logger.exit(valueDescription);
	}

	public UnitType getUnitType(String unitTypeStr) throws SQLException, ClassNotFoundException, IOException, InterruptedException {
		UnitType unitType = null;

		String sql = sqlProperties.getProperty("select_unit_type");
		logger.entry(sql);

		try(Connection connection = MySQLConnector.getConnection()){
			if(connection!=null)
			try(PreparedStatement statement = connection.prepareStatement(sql)){
				statement.setString(1, unitTypeStr);
				try(ResultSet resultSet = statement.executeQuery()){
					if(resultSet.next())
						unitType = new UnitType(resultSet.getInt("id"), unitTypeStr);
				}
			}
		}
		return logger.exit(unitType);
	}

	public ProfileVariable getProfileVariavle(String profileVariableStr) throws ClassNotFoundException, SQLException, IOException, InterruptedException {
		ProfileVariable profileVariable = null;

		String sql = sqlProperties.getProperty("select_profile_variable");
		logger.entry(sql);

		try(Connection connection = MySQLConnector.getConnection()){
			if(connection!=null)
			try(PreparedStatement statement = connection.prepareStatement(sql)){
				statement.setString(1, profileVariableStr);
				try(ResultSet resultSet = statement.executeQuery()){
					if(resultSet.next())
						profileVariable = new ProfileVariable(resultSet.getLong("id"), profileVariableStr, resultSet.getInt("scope"), resultSet.getString("description"));
				}
			}
		}
		return logger.exit(profileVariable);
	}
}
