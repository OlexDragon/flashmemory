package irt.flash.data.connection.dao;

import irt.flash.data.DeviceType;
import static org.junit.Assert.*;
import irt.flash.data.connection.MicrocontrollerSTM32.Address;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

import org.junit.Before;
import org.junit.Test;

public class DatabaseDeviceTypesTest {

    private Logger logger = (Logger) LogManager.getLogger();

    private DatabaseDeviceTypes databaseDeviceTypes;

    @Before
    public void setUp() throws Exception {
        Properties p = new Properties();
		try (InputStream resourceAsStream = Database.class.getResourceAsStream("sql.properties");) {
			p.load(resourceAsStream);
			databaseDeviceTypes = new DatabaseDeviceTypes(p);
		}
    }

    @Test
    public void testGet() throws ClassNotFoundException, SQLException, IOException, InterruptedException {
        final List<DeviceType> types = databaseDeviceTypes.getTypes(Address.BIAS.toString());
        logger.trace("{}", types);
        assertNotNull(types);
    }
}
