package irt.flash.data.connection.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class MySQLConnector{

	private static final Logger logger = (Logger) LogManager.getLogger();

	private static Properties sqlProperties;
	private static Class<?> classForName;

	public static Connection getConnection() throws SQLException, InterruptedException{
		logger.trace("Connecting to the Databace");
		if(classForName==null){
			setClassForName();
		}
		return DriverManager.getConnection(sqlProperties.getProperty("url"), sqlProperties.getProperty("user"), sqlProperties.getProperty("password"));
	}

	private static void setClassForName() throws InterruptedException {
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				logger.entry();
				sqlProperties = new Properties();
				try {
					sqlProperties.load(MySQLConnector.class.getResourceAsStream("MySQLConnector.properties"));
					classForName = Class.forName("com.mysql.jdbc.Driver");
				} catch (Exception e) {
					logger.catching(e);
				}
				logger.exit();
			}
		});
		int priority = t.getPriority();
		if(priority>Thread.MIN_PRIORITY)
			t.setPriority(priority-1);
		t.start();
		t.join();
	}
}