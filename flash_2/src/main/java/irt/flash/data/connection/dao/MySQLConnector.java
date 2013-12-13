package irt.flash.data.connection.dao;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class MySQLConnector{

	private static Properties sqlProperties;
	private static Class<?> classForName;

	public static Connection getConnection() throws ClassNotFoundException, IOException, SQLException{
		if(classForName==null){
			sqlProperties = new Properties();
			sqlProperties.load(MySQLConnector.class.getResourceAsStream("MySQLConnector.properties"));
			classForName = Class.forName("com.mysql.jdbc.Driver");
		}
		return DriverManager.getConnection(sqlProperties.getProperty("url"), sqlProperties.getProperty("user"), sqlProperties.getProperty("password"));
	}
}