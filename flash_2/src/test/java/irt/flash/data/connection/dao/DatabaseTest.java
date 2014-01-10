package irt.flash.data.connection.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import irt.flash.data.DeviceType;
import irt.flash.data.connection.MicrocontrollerSTM32.Address;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.junit.Test;

public class DatabaseTest {

	@Test
	public void getPartNumbersTest() throws ClassNotFoundException, SQLException, IOException, InterruptedException{
		List<String> partNumbers = Database.getPartNumbers("'40W PicoBUC Ku-Band Extended'");
		assertEquals(1, partNumbers.size());
		assertEquals("TPB-KXB0460-HMS1", partNumbers.get(0));
	}

	@Test
	public void getDescriptionsTest() throws ClassNotFoundException, SQLException, IOException, InterruptedException{
		List<String> partNumbers = Database.getDescriptions("TPB-KXB0460-HMS1");
		assertEquals(1, partNumbers.size());
		assertEquals("'40W PicoBUC Ku-Band Extended'", partNumbers.get(0));
	}

	@Test
	public void getDeviceTypesTest() throws ClassNotFoundException, SQLException, IOException, InterruptedException{
		List<DeviceType> deviceTypes = Database.getDeviceTypes(Address.BIAS.toString());
		assertEquals(2, deviceTypes.size());
	}

	@Test
	public void getSystemNameByPartNumberTest() throws ClassNotFoundException, SQLException, IOException, InterruptedException{
		List<String> systemName = Database.getSystemNameByPartNumber("TPB-KXB0490-HMS0");
		assertEquals(1, systemName.size());
		systemName = Database.getSystemNameByPartNumber("TPB-CB00430-HMA0");
		assertEquals(1, systemName.size());
		systemName = Database.getSystemNameByPartNumber("TPB-KXB0460-HMS1");
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
}
