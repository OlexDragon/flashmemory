package irt.flash.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import irt.flash.data.connection.MicrocontrollerSTM32.ProfileProperties;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Test;

public class ProfileTest {

	private final String profileStr = 	"# IRT Technologies board environment config\n\r"+
										"# First two lines must start from this text - do not modify\n"+
										"# MD5 checksum will be placed here\n\r"+
										"\n\r"+
										"# Device information\n\r"+
										"device-type 100\n\r"+
										"device-revision 0\n\r"+
										"device-subtype 1\n\r"+
										"\n\r"+
										"system-name AntBUC\n\r"+
										"product-description '100W AntBUC Ku-Band Extended'\n\r"+
										"device-serial-number IRT-1347002\n\r"+
										"device-part-number TPB-KXB0490-HMS0\n\r"+
										"\n\r"+
										"# Thresholds of current protection\n\r"+
										"#device-threshold-current 0 XXX       	# Zero current\n\r"+
										"device-threshold-current 1 10000      	# SW Output over current\n\r"+
										"device-threshold-current 2 10500      	# SW Driver over current\n\r"+
										"device-threshold-current 3 15         	# HW output over current\n\r"+
										"device-threshold-current 4 20         	# HW driver over current\n\r"+
										"\n\r"+
										"# Thresholds of temperature protection\n\r"+
										"device-threshold-temperature 1 85     	# Mute\n\r"+
										"device-threshold-temperature 2 77     	# Unmute\n\r"+
										"\n\r"+
										"# Output power detector source\n\r"+
										"power-detector-source 1\n\r"+
										"\n\r"+
										"# Network information\n\r"+
										"mac-address 02:49:52:54:00:15\n\r"+
										"\n\r"+
										"#zero-attenuation-gain XXX            	# The gain for zero attenuation\n\r"+
										"\n\r"+
										"power-lut-size 19\n\r"+
										"power-lut-entry  772 32\n\r"+
										"power-lut-entry  842 33\n\r"+
										"power-lut-entry  923 34\n\r"+
										"power-lut-entry  1026 35\n\r"+
										"power-lut-entry  1134 36\n\r"+
										"power-lut-entry  1263 37\n\r"+
										"power-lut-entry  1404 38\n\r"+
										"power-lut-entry  1555 39\n\r"+
										"power-lut-entry  1760 40\n\r"+
										"power-lut-entry  1970 41\n\r"+
										"power-lut-entry  2176 42\n\r"+
										"power-lut-entry  2446 43\n\r"+
										"power-lut-entry  2754 44\n\r"+
										"power-lut-entry  3110 45\n\r"+
										"power-lut-entry  3521 46\n\r"+
										"power-lut-entry  3974 47\n\r"+
										"power-lut-entry  4509 48\n\r"+
										"power-lut-entry  5114 49\n\r"+
										"power-lut-entry  5719 50\n\r"+
										"\n\r"+
										"temperature-lut-size 4\n\r"+
										"temperature-lut-entry 45.0 2800\n\r"+
										"temperature-lut-entry -18 3350\n\r"+
										"temperature-lut-entry 20 3070\n\r"+
										"temperature-lut-entry -18.0 3350\n\r"+
										"temperature-lut-entry 45 2800\n\r"+
										"temperature-lut-entry 74 2450";

	@Test
	public void testProfile1() throws IOException{
		Profile p = Profile.parseProfile(profileStr);
		assertEquals("device-type 100",  p.getProperty(ProfileProperties.DEVICE_TYPE.toString()), "100");
		assertEquals("device-revision 0",  p.getProperty(ProfileProperties.DEVICE_REVISION.toString()), "0");
		assertEquals("device-subtype 1",  p.getProperty(ProfileProperties.DEVICE_SUBTYPE.toString()), "1");
		assertEquals("product-description '100W AntBUC Ku-Band Extended'",  p.getProperty(ProfileProperties.PRODUCT_DESCRIPTION.toString()), "'100W AntBUC Ku-Band Extended'");
		assertEquals("device-serial-number IRT-1347002",  p.getProperty(ProfileProperties.SERIAL_NUMBER.toString()), "IRT-1347002");
		assertEquals("device-part-number TPB-KXB0490-HMS0",  p.getProperty(ProfileProperties.DEVICE_PART_NUMBER.toString()), "TPB-KXB0490-HMS0");
		assertEquals("power-detector-source 1",  p.getProperty(ProfileProperties.POWER_DETECTOR_SOURCE.toString()), "1");
		assertEquals("mac-address 02:49:52:54:00:15",  p.getProperty(ProfileProperties.MAC_ADDRESS.toString()), "02:49:52:54:00:15");
		assertNull("abracadabra", p.getTable("abracadabra"));
	}

	@Test
	public void testProfile2() throws IOException{
		Profile p = Profile.parseProfile(profileStr);

		Table table = p.getTable("device-threshold-current");
		assertNotNull("device-threshold-current", table);
		assertEquals("table.getName()",  table.getName(), "device-threshold-current");
		TreeMap<BigDecimal, BigDecimal> tableMap = table.getTableMap();
		Set<BigDecimal> keySet = tableMap.keySet();
		assertEquals(4, keySet.size());
	}

	@Test
	public void testProfile3() throws IOException{
		Profile p = Profile.parseProfile(profileStr);

		Table table = p.getTable("device-threshold-temperature");
		assertNotNull("device-threshold-temperature", table);
		assertEquals("table.getName()",  table.getName(), "device-threshold-temperature");
		TreeMap<BigDecimal, BigDecimal> tableMap = table.getTableMap();
		Set<BigDecimal> keySet = tableMap.keySet();
		assertEquals(2, keySet.size());
	}

	@Test
	public void testProfile4() throws IOException{
		Profile p = Profile.parseProfile(profileStr);

		Table table = p.getTable("power-lut-entry");
		assertNotNull("power-lut-entry", table);
		assertEquals("table.getName()",  table.getName(), "power-lut-entry");
		TreeMap<BigDecimal, BigDecimal> tableMap = table.getTableMap();
		Set<BigDecimal> keySet = tableMap.keySet();
		assertEquals(19, keySet.size());
	}

	@Test
	public void testProfile5() throws IOException{
		Profile p = Profile.parseProfile(profileStr);

		Table table = p.getTable("temperature-lut-entry");
		assertNotNull("temperature-lut-entry", table);
		assertEquals("table.getName()",  table.getName(), "temperature-lut-entry");
		TreeMap<BigDecimal, BigDecimal> tableMap = table.getTableMap();
		Set<BigDecimal> keySet = tableMap.keySet();
		assertEquals(4, keySet.size());
		String[] values = new String[]{"3350", "3070", "2800", "2450"};
		int index = 0;
		for(BigDecimal bd:keySet)
			assertEquals(new BigDecimal(values[index++]), tableMap.get(bd));
	}

	@Test
	public void testForNull() {
		String[] processLine = Profile.processLine(null);
		assertNull("Input null output:", processLine);
	}

	@Test
	public void test2() {
		String[] processLine = Profile.processLine("");
		assertNull("Input \"\" output:", processLine);
	}

	@Test
	public void test3() {
		String[] processLine = Profile.processLine("device-type");
		assertEquals("Input 'device-type' output length:", 1, processLine.length);
	}

	@Test
	public void test4() {
		String[] processLine = Profile.processLine("device-type 100");
		assertEquals("Input 'device-type 100' output length:", 2, processLine.length);
	}

	@Test
	public void test5() {
		String[] processLine = Profile.processLine("out-power-lut-entry 100 2");
		assertEquals("Input 'out-power-lut-entry 100 2' output length:", 3, processLine.length);
	}

	@Test
	public void test6() {
		String[] processLine = Profile.processLine("out-power-lut-entry 100 2 #Comments");
		assertEquals("Input 'out-power-lut-entry 100 2 #Comments' output length:", 3, processLine.length);
	}

	@Test
	public void testDescription() {
		String[] processLine = Profile.processLine("product-description 40W Ku-bend");
		assertEquals("Input 'product-description 40W Ku-bend' output length:", 2, processLine.length);
	}

	@Test
	public void test8() {
		String[] processLine = Profile.processLine("#product-description 40W Ku-bend");
		assertNull("Input '#product-description 40W Ku-bend' output should be NULL: ", processLine);
	}

	@Test
	public void test9() {
		String[] processLine = Profile.processLine("\n \t\r#");
		assertNull("Input '\\n \\t\\r#' output should be NULL: ", processLine);
	}

	@Test
	public void test10() {
		String[] ss = new String[]{"product-description", "40W Ku-bend"};
		String[] processLine = Profile.processLine("\n\t\tproduct-description \t\t\r40W\t\t \tKu-bend \t");
		System.out.println("Arrays to String"+Arrays.toString(ss));
		System.out.println("Arrays to String"+Arrays.toString(processLine));
		assertTrue("Input '\\n\\t\\tproduct-description \\t\\t\\r40W\\t\\t \\t40WKu-bend' output length:", Arrays.equals(ss, processLine));
	}

	@Test
	public void test11() {
		Profile parseProfile = Profile.parseProfile(profileStr);
		Profile p = Profile.parseProfile(profileStr.substring(0, profileStr.length()-30));
		assertTrue( p.equals(parseProfile));
		p.setCompareByName(false);
		assertFalse( p.equals(parseProfile));
	}
}
