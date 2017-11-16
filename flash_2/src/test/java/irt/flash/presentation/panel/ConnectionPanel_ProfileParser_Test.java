
package irt.flash.presentation.panel;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import irt.flash.presentation.panel.ConnectionPanel.TestResult;

public class ConnectionPanel_ProfileParser_Test {
	private final Logger logger = LogManager.getLogger();

	@Test
	public void testUploadProfileFromFile() {

		//No error
		final ConnectionPanel.ProfileParser profileParser = new ConnectionPanel.ProfileParser();
		try (Scanner scanner = new Scanner(profile)) {
			while (scanner.hasNextLine()){
				final String trim = scanner.nextLine().trim();
				profileParser.append(trim);
			}
		}
		assertEquals(100, (int)profileParser.getDeviceType());
		assertFalse(profileParser.hasError());

		//wrong size
		try (Scanner scanner = new Scanner(wrongSize)) {
			while (scanner.hasNextLine()){
				final String trim = scanner.nextLine().trim();
				profileParser.append(trim);
			}
		}
		assertTrue(profileParser.hasError());
		Map<String, TestResult> reports = profileParser.getReports();
		logger.error(reports);
		assertEquals(1, reports.size());
		assertEquals(TestResult.WRONG_TABLE_SIZE, reports.get("temperature"));

		//Missing size
		try (Scanner scanner = new Scanner(missingSize)) {
			while (scanner.hasNextLine()){
				final String trim = scanner.nextLine().trim();
				profileParser.append(trim);
			}
		}
		assertTrue(profileParser.hasError());
		reports = profileParser.getReports();
		logger.error(reports);
		assertEquals(2, reports.size());
		assertEquals(TestResult.WRONG_SIZE_VALUE, reports.get("temperature2"));

		//Missing value
		try (Scanner scanner = new Scanner(missingValue)) {
			while (scanner.hasNextLine()){
				final String trim = scanner.nextLine().trim();
				profileParser.append(trim);
			}
		}
		assertTrue(profileParser.hasError());
		reports = profileParser.getReports();
		logger.error(reports);
		assertEquals(3, reports.size());
		assertEquals(TestResult.WRONG_STRUCTURE, reports.get("temperature3"));//line with missing value is not considered

		//Wrong sequence
		try (Scanner scanner = new Scanner(wrongSequence)) {
			while (scanner.hasNextLine()){
				final String trim = scanner.nextLine().trim();
				profileParser.append(trim);
			}
		}
		assertTrue(profileParser.hasError());
		reports = profileParser.getReports();
		logger.error(reports);
		assertEquals(4, reports.size());
		assertEquals(TestResult.WRONG_SEQUENCE, reports.get("temperature4"));//line with missing value is not considered
	}

	@Test
	public void testSequrnce() {

		//No error
		final ConnectionPanel.ProfileParser profileParser = new ConnectionPanel.ProfileParser();
		try (Scanner scanner = new Scanner(profile)) {
			while (scanner.hasNextLine()){
				final String trim = scanner.nextLine().trim();
				profileParser.append(trim);
			}
		}

		//Wrong sequence
		try (Scanner scanner = new Scanner(wrongSequence)) {
			while (scanner.hasNextLine()){
				final String trim = scanner.nextLine().trim();
				profileParser.append(trim);
			}
		}
		assertTrue(profileParser.hasError());
		Map<String, TestResult> reports = profileParser.getReports();
		logger.error(reports);
		assertEquals(1, reports.size());
		assertEquals(TestResult.WRONG_SEQUENCE, reports.get("temperature4"));//line with missing value is not considered
	}

	private String profile = "# IRT Technologies board environment config\n"
			+ "#First two lines must start from this text - do not modify\n"
			+ "# MD5 checksum will be placed here\n"
			+ "\n"
			+ "# Device information\n"
			+ "device-type 100\n"
			+ "device-revision 2                    	# 0-1: Rev01; 2: Rev02; 3: Rev02 with non programmable HSS\n"
			+ "device-subtype 1                    	# 0: Generic; 1: Extended; 2: Standard; 3: Palapa; 4: Insat; OR-ed with 0x10 when boards are chained;\n"
			+ "\n"
			+ "contact-information \"IRT Technologies Inc. 5580 Boulevard Thimens, Saint-Laurent, QC, Canada H4R 2K9; Phone: +1-514-907-1161; Fax: +1-514-907-1123; Email: info@irttechnologies.com\"\n"
			+ "system-name PicoBUC\n"
			+ "product-description '12W PicoBUC Ku-Band Extended'\n"
			+ "device-serial-number OP-1111111X\n"
			+ "device-part-number TPB-KXB0410-HMAX\n"
			+ "\n"
			+ "# Thresholds of current protection\n"
			+ "device-threshold-current 0 140    	# Zero current\n"
			+ "#device-threshold-current 1 XXXX   	# SW Output over current (mA)\n"
			+ "#device-threshold-current 2 XXXX   	# SW Driver over current (mA)\n"
			+ "device-threshold-current 3 15     	# HW output over current (X(s) must be one of 15,20,25,30,35,40,45,50 Amps)\n"
			+ "device-threshold-current 4 15     	# HW driver over current (X(s) must be one of 15,20,25,30,35,40,45,50 Amps)\n"
			+ "# Thresholds of temperature protection\n"
			+ "device-threshold-temperature 1 85.0   	# Mute\n"
			+ "device-threshold-temperature 2 77.0   	# Unmute\n"
			+ "# Thresholds of power\n"
			+ "device-threshold-power 1 -45.0  	# Min input power\n"
			+ "\n"
			+ "\n"
			+ "# RF overdrive\n"
			+ "#rf-overdrive-attenuation-step XX.X   	# Attenuation incremental step, 0.1 dB\n"
			+ "# Output power detector source\n"
			+ "power-detector-source 2           	# 0: On-board sensor; 1: HSI1 current; 2: FCM IP + gain; 3: FCM OP; 4: HSI2 current; 5: FCM IP - attenuation\n"
			+ "#out-power-zero-attenuation-gain XXX   	# The gain for zero attenuation, used for OP calc\n"
			+ "\n"
			+ "# Network information\n"
			+ "#mac-address 02:49:52:54:XX:XX         	# Taken from bank of MAC\n"
			+ "zero-attenuation-gain 730             	# The gain for zero attenuation\n"
			+ "\n"
			+ "# No under-current check option\n"
			+ "#hss-uc-disable  0 1                 	# HSS1\n"
			+ "#hss-uc-disable  1 1                 	# HSS2\n"
			+ "# HSS Fault Register bit mask. Check when is set (default are all set).\n"
			+ "# Where: [6]- OTF, [5]- OCHF, [4]- OCLF, [3]- OLF, [2]- UVF, [1]- OVF\n"
			+ "#hss-fr-bitmask 0 0xXX\n"
			+ "#hss-fr-bitmask 1 0xXX\n"
			+ "\n"
			+ "# Linearizer\n"
			+ "#external-dp-present 1\n"
			+ "#external-dp-present 2\n"
			+ "# EIRP\n"
			+ "output-eirp-visible 0\n"
			+ "\n"
			+ "# Under IP threshold OP calc params\n"
			+ "#power-detector-source-threshold X XX.X	# <source> <input power threshold>\n"
			+ "#                         Pout, dBm\n"
			+ "#power2-lut-size   X\n"
			+ "#power2-lut-entry  XX     XX.X\n"
			+ "# LUTs\n"
			+ "#                 Pfcm,dBm  Pout,dBm\n"
			+ "#power-lut-size   X\n"
			+ "#power-lut-entry  XX.X      XX.X\n"
			+ "\n"
			+ "#                      T, degC   DAC\n"
			+ "#temperature-lut-size  X\n"
			+ "#temperature-lut-entry XX.X      XXXX\n"
			+ "\n"
			+ "\n"
			+ "power-lut-size 6\n"
			+ "power-lut-entry  1120   33\n"
			+ "power-lut-entry  1382   36\n"
			+ "power-lut-entry  1944   39\n"
			+ "power-lut-entry  2776   42\n"
			+ "power-lut-entry  3899   45\n"
			+ "power-lut-entry  5011   47\n"
			+ "\n"
			+ "frequency-lut-size 3\n"
			+ "#                    F, Hz     %\n"
			+ "\n"
			+ "frequency-lut-entry 14000000000	1\n"
			+ "frequency-lut-entry 14250000000	1\n"
			+ "frequency-lut-entry 14500000000	1.107741420590583";

	private String wrongSize = "temperature-lut-size 6 \n"
								+ "#                T, degC   DAC\n"
								+ "temperature-lut-entry -40 3740\n"
								+ "temperature-lut-entry -6 3400\n"
								+ "temperature-lut-entry 25 3090\n"
								+ "temperature-lut-entry 34 3000\n"
								+ "temperature-lut-entry 80 2586";

	private String missingSize = "temperature2-lut-size d \n"
								+ "#                T, degC   DAC\n"
								+ "temperature2-lut-entry -40 3740\n"
								+ "temperature2-lut-entry -6 3400\n"
								+ "temperature2-lut-entry 25 3090\n"
								+ "temperature2-lut-entry 34 3000\n"
								+ "temperature2-lut-entry 80 2586";

	private String missingValue = "temperature3-lut-size 5 \n"
								+ "#                T, degC   DAC\n"
								+ "temperature3-lut-entry -40 3740\n"
								+ "temperature3-lut-entry -6 \n"			//line with missing value is not considered
								+ "temperature3-lut-entry 25 3090\n"
								+ "temperature3-lut-entry 34 3000\n"
								+ "temperature3-lut-entry 80 2586";


	private String wrongSequence = "temperature4-lut-size 5 \n"
								+ "#                T, degC   DAC\n"
								+ "temperature4-lut-entry -6 3400\n"
								+ "temperature4-lut-entry -40 3740\n"
								+ "temperature4-lut-entry 25 3090\n"
								+ "temperature4-lut-entry 34 3000\n"
								+ "temperature4-lut-entry 80 2586";
}
