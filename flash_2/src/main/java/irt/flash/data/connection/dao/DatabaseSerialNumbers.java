package irt.flash.data.connection.dao;

import irt.flash.data.SerialNumber;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class DatabaseSerialNumbers {

	private static final Logger logger = (Logger) LogManager.getLogger();

	private Properties sqlProperties;

	public DatabaseSerialNumbers(Properties sqlProperties){
		logger.info("* Start *");
		this.sqlProperties = sqlProperties;
	}

	public boolean isExists(String serialNumber) throws ClassNotFoundException, SQLException, IOException, InterruptedException{

		boolean hasNext = false;
		String sql = sqlProperties.getProperty("select_serial_number");

		try(Connection connection = MySQLConnector.getConnection()){
			try(PreparedStatement statement = connection.prepareStatement(sql)){
				statement.setString(1, serialNumber);
				try(ResultSet resultSet = statement.executeQuery()){
					hasNext = resultSet.next();
				}
			}
		}
		return hasNext;
	}

	public int update(Connection connection, SerialNumber serialNumber) throws SQLException {

		String sql = sqlProperties.getProperty("update_serial_number");

		int executeUpdate;
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, serialNumber.getSerialNumber());
			statement.setLong(2, serialNumber.getId());
			executeUpdate = statement.executeUpdate();
		}
		logger.info("serialNumber={}, sql={}, executeUpdate={}", serialNumber, sql);
		return executeUpdate;
	}

	public long add(Connection connection, String serialNumberStr) throws SQLException{

		String sql = sqlProperties.getProperty("insert_serial_number");
		logger.trace(sql);

		long serialNumberId = 0;
		try (PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
			statement.setString(1, serialNumberStr);
			if (statement.executeUpdate() > 0) {
				ResultSet generatedKeys = statement.getGeneratedKeys();
				if (generatedKeys.next())
					serialNumberId = generatedKeys.getLong(1);
			}
		}
		return serialNumberId;
	}

	public SerialNumber get(Connection connection, String serialNumberStr) throws SQLException, NullPointerException {
		logger.entry(serialNumberStr);

		SerialNumber serialNumber = null;
		String sql = sqlProperties.getProperty("select_serial_number");
		logger.trace(sql);

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, serialNumberStr);
			statement.setString(2, serialNumberStr);
			try(ResultSet resultSet = statement.executeQuery()){
				if(resultSet.next()){
					serialNumber = new SerialNumber();
					serialNumber.setId(resultSet.getLong("id"));
					serialNumber.setSerialNumber(resultSet.getString("serial_number"));
					serialNumber.setDate(resultSet.getTimestamp("date"));
				}
			}
		}
		return logger.exit(serialNumber);
	}

	public String getNextSerialNumber(String yerWeek) throws ClassNotFoundException, SQLException, IOException, InterruptedException {
		logger.entry(yerWeek);

		String serialNumber = null;
		String like = "IRT-"+yerWeek+'%';
		logger.trace("LIKE'{}'", like);

		String sql = sqlProperties.getProperty("new_serial_number");
		logger.trace(sql);

		try(Connection connection = MySQLConnector.getConnection()){
			try (PreparedStatement statement = connection.prepareStatement(sql)) {

				statement.setString(1, like);
				statement.setString(2, like);

				try(ResultSet resultSet = statement.executeQuery()){
					List<Integer> l = new ArrayList<>();

					while(resultSet.next()){
						String string = resultSet.getString("serial_number");
						logger.trace("while(resultSet.next())={}", string);
						l.add(Integer.parseInt(string.replaceAll("\\D", "").substring(yerWeek.length())));
					}

					logger.trace(l);
					serialNumber = "IRT-"+getSerialNumber(yerWeek, l);
				}
			}
		}
			return logger.exit(serialNumber);
	}

	private String getSerialNumber(String yerWeek, List<Integer> l) {
		String serialNumber = null;

		if(l.isEmpty())
			serialNumber = yerWeek + new DecimalFormat("000").format(1);
		else
			for(int i=1; i<1000; i++)
				if(!l.contains(i)){
					serialNumber = yerWeek + new DecimalFormat("000").format(i);
					break;
				}

		return serialNumber;
	}

	public boolean profileIsChanged(Connection connection, long serialNumberId, String profileStr) throws SQLException {

		boolean isChanged = false;
		String sql = sqlProperties.getProperty("profile_have_been_changed");

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, serialNumberId);
			statement.setString(2, profileStr);
			try(ResultSet resultSet = statement.executeQuery()){
				isChanged = !resultSet.next();
			}
		}
		logger.info("serialNumberId={}, profileStr={}, isChanged=>{}, sql={}", serialNumberId, profileStr, isChanged, sql);
		return isChanged;
	}

	public int setProfile(Connection connection, long serialNumberId, String profileStr) throws SQLException {
		
		String sql = sqlProperties.getProperty("set_profile");

		int executeUpdate = 0;
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(2, serialNumberId);
			statement.setString(1, profileStr);
			executeUpdate = statement.executeUpdate();
		}
		logger.info("serialNumberId={}, profileStr={}, executeUpdate=>{}, sql={}", serialNumberId, profileStr, executeUpdate, sql);
		return executeUpdate;
	}
}
