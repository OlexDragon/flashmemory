package irt.flash.data.connection.dao;

import static org.junit.Assert.*;
import irt.flash.data.connection.MicrocontrollerSTM32.Address;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

public class DatabaseDeviceTypesTest {

	private DatabaseDeviceTypes databaseDeviceTypes;

	@Before
	public void setUp() throws Exception {
		Properties p = new Properties();
		p.load(Database.class.getResourceAsStream("sql.properties"));
		databaseDeviceTypes = new DatabaseDeviceTypes(p);
	}

	@Test
	public void testGet() throws ClassNotFoundException, SQLException, IOException, InterruptedException {
		assertNotNull(databaseDeviceTypes.getTypes(Address.BIAS.toString()));
	}

}
