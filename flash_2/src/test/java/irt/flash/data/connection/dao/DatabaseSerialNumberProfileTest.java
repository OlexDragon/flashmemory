package irt.flash.data.connection.dao;

import static org.junit.Assert.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

public class DatabaseSerialNumberProfileTest {

	private DatabaseSerialNumberProfile databaseSerialNumberProfile;

	@Before
	public void setUp() throws Exception {
		Properties p = new Properties();
		p.load(Database.class.getResourceAsStream("sql.properties"));
		databaseSerialNumberProfile = new DatabaseSerialNumberProfile(p);
	}

	@Test
	public void testGetNextMacAddress() throws ClassNotFoundException, SQLException, IOException {
		String nextMacAddress = databaseSerialNumberProfile.getNextMacAddress();
		assertNotNull(nextMacAddress);
	}
}
