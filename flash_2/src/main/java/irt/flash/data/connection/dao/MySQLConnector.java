package irt.flash.data.connection.dao;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class MySQLConnector{

	private static final Logger logger = (Logger) LogManager.getLogger();

	private static Properties sqlProperties;
	private static Class<?> classForName;
	private static boolean isIrtTechnologies;

	//Check if http://irttechnologies exists
	static{
		try {

			InetAddress.getByName("irttechnologies");
			isIrtTechnologies = true;

		} catch (UnknownHostException e) {
			logger.catching(e);
		}
	}

	public static Connection getConnection(){
		logger.trace("Connecting to the Databace");
		try {

			if(classForName==null){
				setClassForName();
			}
			return isIrtTechnologies ? DriverManager.getConnection(sqlProperties.getProperty("url"), sqlProperties.getProperty("user"), sqlProperties.getProperty("password")) : null;

		} catch (Exception e) {

			logger.catching(e);
			return null;
		}
	}

	private static void setClassForName() throws InterruptedException {
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				logger.entry();
				sqlProperties = new Properties();
				try(InputStream resourceAsStream = MySQLConnector.class.getResourceAsStream("MySQLConnector.properties");) {
					
					sqlProperties.load(resourceAsStream);
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