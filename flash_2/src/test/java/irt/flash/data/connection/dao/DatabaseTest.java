package irt.flash.data.connection.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import irt.flash.data.DeviceType;
import irt.flash.data.SerialNumber;
import irt.flash.data.connection.MicrocontrollerSTM32.Address;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.junit.Test;

public class DatabaseTest {

	private static final Logger logger = (Logger) LogManager.getLogger();

	@Test
	public void getPartNumbersTest() throws ClassNotFoundException, SQLException, IOException, InterruptedException{
		List<String> partNumbers = Database.getPartNumbers("'100W AntBUC Ku-Band Extended'");
		assertEquals(5, partNumbers.size());
		assertEquals("TPB-KXB0500-HMS12", partNumbers.get(0));
	}

	@Test
	public void getDescriptionsTest() throws ClassNotFoundException, SQLException, IOException, InterruptedException{
		List<String> partNumbers = Database.getDescriptions("TPB-CB00460-HMA0");
		assertEquals(1, partNumbers.size());
		assertEquals("'40W PicoBUC C-Band'", partNumbers.get(0));
	}

	@Test
	public void getDeviceTypesTest() throws ClassNotFoundException, SQLException, IOException, InterruptedException{
		List<DeviceType> deviceTypes = Database.getDeviceTypes(Address.BIAS.toString());
		assertEquals(2, deviceTypes.size());
	}

	@Test
	public void getSystemNameByPartNumberTest() throws ClassNotFoundException, SQLException, IOException, InterruptedException{
		List<String> systemName = Database.getSystemNameByPartNumber("TPB-KXB0490-HMS0");
		assertEquals(2, systemName.size());
		systemName = Database.getSystemNameByPartNumber("TPB-CB00430-HMA0");
		assertEquals(1, systemName.size());
		systemName = Database.getSystemNameByPartNumber("TPB-KXB0460-HMS9");
		assertNull(systemName);
	}

	@Test
	public void getSystemNameByDescriptionTest() throws ClassNotFoundException, SQLException, IOException, InterruptedException{
		List<String> systemName = Database.getSystemNameByDescription("'100W AntBUC Ku-Band Extended'");
		assertEquals(1, systemName.size());
		systemName = Database.getSystemNameByDescription("'40W L to C Band BUC'");
		assertNull(systemName);
		systemName = Database.getSystemNameByDescription("'20W PicoBUC C-Band'");
		assertEquals(1, systemName.size());
	}

	@Test
	public void getSerialNumber1() throws ClassNotFoundException, SQLException, IOException, InterruptedException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException{

		logger.entry();
		Method method = Database.class.getDeclaredMethod("getSerialNumber", Connection.class, String.class);
		method.setAccessible(true);

		try(Connection connection = MySQLConnector.getConnection()){
			SerialNumber output = (SerialNumber) method.invoke(Database.getInstance(), connection, "IRT-1350002");
			assertEquals(new SerialNumber().setId(66), output);
		}
		logger.exit();
	}

	@Test
	public void getSerialNumber2() throws ClassNotFoundException, SQLException, IOException, InterruptedException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException{

		logger.entry();
		Method method = Database.class.getDeclaredMethod("getSerialNumber", Connection.class, String.class, String.class, Timestamp.class);
		method.setAccessible(true);

		try(Connection connection = MySQLConnector.getConnection()){
			SerialNumber output = (SerialNumber) method.invoke(Database.getInstance(), connection, null, "IRT-1350002", null);
			assertEquals(new SerialNumber().setId(66), output);
		}
		logger.exit();
	}

	@Test
	public void getSerialNumber3() throws Exception {

		logger.entry();
		Method method = Database.class.getDeclaredMethod("getSerialNumber", Connection.class, String.class, String.class, Timestamp.class);
		method.setAccessible(true);

		try(Connection connection = MySQLConnector.getConnection()){
			SerialNumber output = (SerialNumber) method.invoke(Database.getInstance(), connection, null, "IRT-1350002", Timestamp.valueOf("2014-01-23 09:54:00"));
			assertEquals(new SerialNumber().setId(66), output);
		}
		logger.exit();
	}

	@Test
	public void getSerialNumber4() throws Exception {

		logger.entry();
		Method method = Database.class.getDeclaredMethod("getSerialNumber", Connection.class, String.class, String.class, Timestamp.class);
		method.setAccessible(true);

		try(Connection connection = MySQLConnector.getConnection()){
			SerialNumber output = (SerialNumber) method.invoke(Database.getInstance(), connection, "IRT-1350002", null, null);
			assertEquals(new SerialNumber().setId(66), output);
		}
		logger.exit();
	}

	@Test
	public void getSerialNumber5() throws Exception {

		logger.entry();
		Method method = Database.class.getDeclaredMethod("getSerialNumber", Connection.class, String.class, String.class, Timestamp.class);
		method.setAccessible(true);

		try(Connection connection = MySQLConnector.getConnection()){
			SerialNumber output = (SerialNumber) method.invoke(Database.getInstance(), connection, "IRT-1350002", null, null);
			assertEquals(new SerialNumber().setId(66), output);
		}
		logger.exit();
	}

	@Test
	public void getSerialNumber6() throws Exception {

//		logger.entry();
//		Method method = Database.class.getDeclaredMethod("getSerialNumber", Connection.class, String.class, String.class, Timestamp.class);
//		method.setAccessible(true);
//
//		try(Connection connection = MySQLConnector.getConnection()){
//			SerialNumber output = (SerialNumber) method.invoke(Database.getInstance(), connection, "IRT-1350002", "IRT-1350002C", null);
//			assertEquals(new SerialNumber().setId(66), output);
//			logger.exit(output);
//		}
	}

	@Test
	public void getSerialNumber7() throws Exception {

//		logger.entry();
//		Method method = Database.class.getDeclaredMethod("getSerialNumber", Connection.class, String.class, String.class, Timestamp.class);
//		method.setAccessible(true);
//
//		try(Connection connection = MySQLConnector.getConnection()){
//			SerialNumber output = (SerialNumber) method.invoke(Database.getInstance(), connection, "IRT-1350002", "IRT-1350002A", Timestamp.valueOf("2013-01-23 09:54:00"));
//			assertEquals(new SerialNumber().setId(7), output);
//		}
//		logger.exit();
	}
}
