package irt.flash.data.connection.dao;

import irt.flash.data.SerialNumber;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class DatabaseSerialNumbers {

	private static final Logger logger = (Logger) LogManager.getLogger();

	private Properties sqlProperties;

	public DatabaseSerialNumbers(Properties sqlProperties){
		this.sqlProperties = sqlProperties;
	}

	public boolean isExists(String serialNumber) throws ClassNotFoundException, SQLException, IOException{

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
		logger.trace(sql);

		int executeUpdate;
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, serialNumber.getSerialNumber());
			statement.setLong(2, serialNumber.getId());
			executeUpdate = statement.executeUpdate();
		}
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

}
