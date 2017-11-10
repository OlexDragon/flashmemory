package irt.flash.data.connection.dao;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

public class DatabaseSerialNumberProfileTest {

	private DatabaseSerialNumberProfile databaseSerialNumberProfile;

	@Before
	public void setUp() throws Exception {
		Properties p = new Properties();
		try( InputStream resourceAsStream = Database.class.getResourceAsStream("sql.properties");){
			p.load(resourceAsStream);
			databaseSerialNumberProfile = new DatabaseSerialNumberProfile(p);
		}
	}

	@Test
	public void testGetNextMacAddress() throws ClassNotFoundException, SQLException, IOException, InterruptedException {
		String nextMacAddress = databaseSerialNumberProfile.getNextMacAddress();
		assertNotNull(nextMacAddress);
	}
}
