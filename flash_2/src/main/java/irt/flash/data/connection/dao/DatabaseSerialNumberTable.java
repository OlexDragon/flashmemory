package irt.flash.data.connection.dao;

import irt.flash.data.SerialNumberTableRow;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class DatabaseSerialNumberTable {

	private static final Logger logger = (Logger) LogManager.getLogger();

	private Properties sqlProperties;

	public DatabaseSerialNumberTable(Properties sqlProperties) {
		logger.info("* Start *");
		this.sqlProperties = sqlProperties;
	}

	public long set(Connection connection, long serialNumberId, long profileVariableId, BigDecimal key, BigDecimal value) throws SQLException {

		long rowId = getRow(connection, serialNumberId, profileVariableId, key, value);
		if(rowId<=0)
			rowId = addRow(connection, serialNumberId, profileVariableId, key, value);
		return rowId;
		
	}

	private long getRow(Connection connection, long serialNumberId, long profileVariableId, BigDecimal key, BigDecimal value) throws SQLException {

		String sql = sqlProperties.getProperty("active_serial_number_table_row");
		logger.trace(sql);

		long rowId = 0;
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, serialNumberId);
			statement.setLong(2, profileVariableId);
			statement.setBigDecimal(3, key);
			statement.setBigDecimal(4, value);
			try(ResultSet resultSet = statement.executeQuery()){
				if(resultSet.next())
					rowId = resultSet.getLong("id");
			}
		}
		return rowId;
	}

	private long addRow(Connection connection, long serialNumberId, long profileVariableId, BigDecimal key, BigDecimal value) throws SQLException {
		logger.entry(serialNumberId, profileVariableId, key, value);

		String sql = sqlProperties.getProperty("insert_serial_number_table_row");
		logger.trace(sql);

		long rowId = 0;
		try (PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
			statement.setLong(1, serialNumberId);
			statement.setLong(2, profileVariableId);
			statement.setBigDecimal(3, key);
			statement.setBigDecimal(4, value);
			statement.setBoolean(5, true);
			if (statement.executeUpdate() > 0) {
				ResultSet generatedKeys = statement.getGeneratedKeys();
				if (generatedKeys.next())
					rowId = generatedKeys.getLong(1);
			}
		}
		return logger.exit(rowId);
	}

	public void setActive(Connection connection, long serialNumberId, long profileVariableId, TreeMap<BigDecimal, BigDecimal> tableMap) throws SQLException {
		logger.entry(profileVariableId, tableMap);
		List<SerialNumberTableRow> trs = getActiveRows(connection, serialNumberId, profileVariableId);
		if(trs!=null){
			for(SerialNumberTableRow tr:trs){
				BigDecimal value = tableMap.get(tr.getKey());
				if(value==null || tr.getValue().compareTo(value)!=0)
					setActive(connection, tr.getId(), false);
			}
		}
		logger.exit();
	}

	private void setActive(Connection connection, long rowId, boolean status) throws SQLException {

		String sql = sqlProperties.getProperty("update_active_serial_number_table_row");
		logger.trace(sql);

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setBoolean(1, status);
			statement.setLong(2, rowId);
			statement.executeUpdate();
		}
	}

	private List<SerialNumberTableRow> getActiveRows(Connection connection, long serialNumberId, long profileVariableId) throws SQLException {

		String sql = sqlProperties.getProperty("active_serial_number_table");
		logger.trace(sql);

		List<SerialNumberTableRow> rows = null;
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, serialNumberId);
			statement.setLong(2, profileVariableId);
			try(ResultSet resultSet = statement.executeQuery()){
				if(resultSet.next())
					rows = new ArrayList<>();
					do{
						rows.add(new SerialNumberTableRow()	.setId(resultSet.getLong("id"))
															.setSerialNumberId(resultSet.getLong("id_serial_numbers"))
															.setTableNameId(resultSet.getLong("id_profile_variables"))
															.setKey(resultSet.getBigDecimal("key"))
															.setValue(resultSet.getBigDecimal("value"))
															.setStatus(resultSet.getBoolean("status"))
															.setDate(resultSet.getTimestamp("date"))
															.setStatusChangeDate(resultSet.getTimestamp("status_change_date")));
					}while(resultSet.next());
			}
		}
		return rows;
	}

}
