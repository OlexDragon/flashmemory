package irt.flash.helpers;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import irt.flash.data.UnitAddress;

public class FlashWorkerTest {

	@Test
	public void test() throws IOException {
		byte[] pagesToErase = FlashWorker.getPagesToExtendedErase(UnitAddress.CONVERTER.getAddr(), 10 * FlashWorker.KB);
		System.out.println(Arrays.toString(pagesToErase));
		assertEquals(4, pagesToErase.length);
		assertEquals(0, pagesToErase[0]);
		assertEquals(0, pagesToErase[1]);
		assertEquals(0, pagesToErase[2]);
		assertEquals(10, pagesToErase[3]);

		pagesToErase = FlashWorker.getPagesToExtendedErase(UnitAddress.BIAS.getAddr(), 1 * FlashWorker.KB);
		System.out.println(Arrays.toString(pagesToErase));
		assertEquals(4, pagesToErase.length);
		assertEquals(0, pagesToErase[0]);
		assertEquals(0, pagesToErase[1]);
		assertEquals(0, pagesToErase[2]);
		assertEquals(11, pagesToErase[3]);

		pagesToErase = FlashWorker.getPagesToExtendedErase(UnitAddress.BIAS.getAddr(), 129 * FlashWorker.KB);
		System.out.println(Arrays.toString(pagesToErase));
		assertEquals(6, pagesToErase.length);
		assertEquals(0, pagesToErase[0]);
		assertEquals(1, pagesToErase[1]);
		assertEquals(0, pagesToErase[2]);
		assertEquals(11, pagesToErase[3]);
		assertEquals(0, pagesToErase[4]);
		assertEquals(12, pagesToErase[5]);
	}

	@Test
	public void getCheckSumTest() throws IOException {
		byte[] bytes = 	new byte[] {(byte)0xFF ,(byte)0x23 ,(byte)0x20 ,(byte)0x49 ,(byte)0x52 ,(byte)0x54 ,(byte)0x20 ,(byte)0x54 ,(byte)0x65 ,(byte)0x63 ,(byte)0x68 ,(byte)0x6E ,(byte)0x6F ,(byte)0x6C ,(byte)0x6F ,(byte)0x67 ,(byte)0x69 ,(byte)0x65 ,(byte)0x73 ,(byte)0x20 ,(byte)0x62 ,(byte)0x6F ,(byte)0x61 ,(byte)0x72 ,(byte)0x64 ,(byte)0x20 ,(byte)0x65 ,(byte)0x6E ,(byte)0x76 ,(byte)0x69 ,(byte)0x72 ,(byte)0x6F ,(byte)0x6E ,(byte)0x6D ,(byte)0x65 ,(byte)0x6E ,(byte)0x74 ,(byte)0x20 ,(byte)0x63 ,(byte)0x6F ,(byte)0x6E ,(byte)0x66 ,(byte)0x69 ,(byte)0x67 ,(byte)0x0A ,(byte)0x23 ,(byte)0x20 ,(byte)0x46 ,(byte)0x69 ,(byte)0x72 ,(byte)0x73 ,(byte)0x74 ,(byte)0x20 ,(byte)0x74 ,(byte)0x77 ,(byte)0x6F ,(byte)0x20 ,(byte)0x6C ,(byte)0x69 ,(byte)0x6E ,(byte)0x65 ,(byte)0x73 ,(byte)0x20 ,(byte)0x6D ,(byte)0x75 ,(byte)0x73 ,(byte)0x74 ,(byte)0x20 ,(byte)0x73 ,(byte)0x74 ,(byte)0x61 ,(byte)0x72 ,(byte)0x74 ,(byte)0x20 ,(byte)0x66 ,(byte)0x72 ,(byte)0x6F ,(byte)0x6D ,(byte)0x20 ,(byte)0x74 ,(byte)0x68 ,(byte)0x69 ,(byte)0x73 ,(byte)0x20 ,(byte)0x74 ,(byte)0x65 ,(byte)0x78 ,(byte)0x74 ,(byte)0x20 ,(byte)0x2D ,(byte)0x20 ,(byte)0x64 ,(byte)0x6F ,(byte)0x20 ,(byte)0x6E ,(byte)0x6F ,(byte)0x74 ,(byte)0x20 ,(byte)0x6D ,(byte)0x6F ,(byte)0x64 ,(byte)0x69 ,(byte)0x66 ,(byte)0x79 ,(byte)0x0A ,(byte)0x23 ,(byte)0x20 ,(byte)0x4D ,(byte)0x44 ,(byte)0x35 ,(byte)0x20 ,(byte)0x63 ,(byte)0x68 ,(byte)0x65 ,(byte)0x63 ,(byte)0x6B ,(byte)0x73 ,(byte)0x75 ,(byte)0x6D ,(byte)0x20 ,(byte)0x77 ,(byte)0x69 ,(byte)0x6C ,(byte)0x6C ,(byte)0x20 ,(byte)0x62 ,(byte)0x65 ,(byte)0x20 ,(byte)0x70 ,(byte)0x6C ,(byte)0x61 ,(byte)0x63 ,(byte)0x65 ,(byte)0x64 ,(byte)0x20 ,(byte)0x68 ,(byte)0x65 ,(byte)0x72 ,(byte)0x65 ,(byte)0x0A ,(byte)0x0A ,(byte)0x23 ,(byte)0x20 ,(byte)0x44 ,(byte)0x65 ,(byte)0x76 ,(byte)0x69 ,(byte)0x63 ,(byte)0x65 ,(byte)0x20 ,(byte)0x69 ,(byte)0x6E ,(byte)0x66 ,(byte)0x6F ,(byte)0x72 ,(byte)0x6D ,(byte)0x61 ,(byte)0x74 ,(byte)0x69 ,(byte)0x6F ,(byte)0x6E ,(byte)0x0A ,(byte)0x64 ,(byte)0x65 ,(byte)0x76 ,(byte)0x69 ,(byte)0x63 ,(byte)0x65 ,(byte)0x2D ,(byte)0x74 ,(byte)0x79 ,(byte)0x70 ,(byte)0x65 ,(byte)0x20 ,(byte)0x31 ,(byte)0x30 ,(byte)0x30 ,(byte)0x0A ,(byte)0x64 ,(byte)0x65 ,(byte)0x76 ,(byte)0x69 ,(byte)0x63 ,(byte)0x65 ,(byte)0x2D ,(byte)0x72 ,(byte)0x65 ,(byte)0x76 ,(byte)0x69 ,(byte)0x73 ,(byte)0x69 ,(byte)0x6F ,(byte)0x6E ,(byte)0x20 ,(byte)0x32 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x09 ,(byte)0x23 ,(byte)0x20 ,(byte)0x30 ,(byte)0x2D ,(byte)0x31 ,(byte)0x3A ,(byte)0x20 ,(byte)0x52 ,(byte)0x65 ,(byte)0x76 ,(byte)0x30 ,(byte)0x31 ,(byte)0x3B ,(byte)0x20 ,(byte)0x32 ,(byte)0x3A ,(byte)0x20 ,(byte)0x52 ,(byte)0x65 ,(byte)0x76 ,(byte)0x30 ,(byte)0x32 ,(byte)0x3B ,(byte)0x20 ,(byte)0x33 ,(byte)0x3A ,(byte)0x20 ,(byte)0x52 ,(byte)0x65 ,(byte)0x76 ,(byte)0x30 ,(byte)0x32 ,(byte)0x20 ,(byte)0x77 ,(byte)0x69 ,(byte)0x74 ,(byte)0x68 ,(byte)0x20 ,(byte)0x6E ,(byte)0x6F ,(byte)0x6E};

		assertEquals((byte) 0xAE, FlashWorker.getCheckSum(bytes));

		bytes = 		new byte[] {(byte)0xFF ,(byte)0x23 ,(byte)0x20 ,(byte)0x49 ,(byte)0x52 ,(byte)0x54 ,(byte)0x20 ,(byte)0x54 ,(byte)0x65 ,(byte)0x63 ,(byte)0x68 ,(byte)0x6E ,(byte)0x6F ,(byte)0x6C ,(byte)0x6F ,(byte)0x67 ,(byte)0x69 ,(byte)0x65 ,(byte)0x73 ,(byte)0x20 ,(byte)0x62 ,(byte)0x6F ,(byte)0x61 ,(byte)0x72 ,(byte)0x64 ,(byte)0x20 ,(byte)0x65 ,(byte)0x6E ,(byte)0x76 ,(byte)0x69 ,(byte)0x72 ,(byte)0x6F ,(byte)0x6E ,(byte)0x6D ,(byte)0x65 ,(byte)0x6E ,(byte)0x74 ,(byte)0x20 ,(byte)0x63 ,(byte)0x6F ,(byte)0x6E ,(byte)0x66 ,(byte)0x69 ,(byte)0x67 ,(byte)0x0D ,(byte)0x0A ,(byte)0x23 ,(byte)0x20 ,(byte)0x46 ,(byte)0x69 ,(byte)0x72 ,(byte)0x73 ,(byte)0x74 ,(byte)0x20 ,(byte)0x74 ,(byte)0x77 ,(byte)0x6F ,(byte)0x20 ,(byte)0x6C ,(byte)0x69 ,(byte)0x6E ,(byte)0x65 ,(byte)0x73 ,(byte)0x20 ,(byte)0x6D ,(byte)0x75 ,(byte)0x73 ,(byte)0x74 ,(byte)0x20 ,(byte)0x73 ,(byte)0x74 ,(byte)0x61 ,(byte)0x72 ,(byte)0x74 ,(byte)0x20 ,(byte)0x66 ,(byte)0x72 ,(byte)0x6F ,(byte)0x6D ,(byte)0x20 ,(byte)0x74 ,(byte)0x68 ,(byte)0x69 ,(byte)0x73 ,(byte)0x20 ,(byte)0x74 ,(byte)0x65 ,(byte)0x78 ,(byte)0x74 ,(byte)0x20 ,(byte)0x2D ,(byte)0x20 ,(byte)0x64 ,(byte)0x6F ,(byte)0x20 ,(byte)0x6E ,(byte)0x6F ,(byte)0x74 ,(byte)0x20 ,(byte)0x6D ,(byte)0x6F ,(byte)0x64 ,(byte)0x69 ,(byte)0x66 ,(byte)0x79 ,(byte)0x0D ,(byte)0x0A ,(byte)0x23 ,(byte)0x20 ,(byte)0x4D ,(byte)0x44 ,(byte)0x35 ,(byte)0x20 ,(byte)0x63 ,(byte)0x68 ,(byte)0x65 ,(byte)0x63 ,(byte)0x6B ,(byte)0x73 ,(byte)0x75 ,(byte)0x6D ,(byte)0x20 ,(byte)0x77 ,(byte)0x69 ,(byte)0x6C ,(byte)0x6C ,(byte)0x20 ,(byte)0x62 ,(byte)0x65 ,(byte)0x20 ,(byte)0x70 ,(byte)0x6C ,(byte)0x61 ,(byte)0x63 ,(byte)0x65 ,(byte)0x64 ,(byte)0x20 ,(byte)0x68 ,(byte)0x65 ,(byte)0x72 ,(byte)0x65 ,(byte)0x0D ,(byte)0x0A ,(byte)0x0D ,(byte)0x0A ,(byte)0x23 ,(byte)0x20 ,(byte)0x44 ,(byte)0x65 ,(byte)0x76 ,(byte)0x69 ,(byte)0x63 ,(byte)0x65 ,(byte)0x20 ,(byte)0x69 ,(byte)0x6E ,(byte)0x66 ,(byte)0x6F ,(byte)0x72 ,(byte)0x6D ,(byte)0x61 ,(byte)0x74 ,(byte)0x69 ,(byte)0x6F ,(byte)0x6E ,(byte)0x0D ,(byte)0x0A ,(byte)0x64 ,(byte)0x65 ,(byte)0x76 ,(byte)0x69 ,(byte)0x63 ,(byte)0x65 ,(byte)0x2D ,(byte)0x74 ,(byte)0x79 ,(byte)0x70 ,(byte)0x65 ,(byte)0x20 ,(byte)0x31 ,(byte)0x30 ,(byte)0x30 ,(byte)0x0D ,(byte)0x0A ,(byte)0x64 ,(byte)0x65 ,(byte)0x76 ,(byte)0x69 ,(byte)0x63 ,(byte)0x65 ,(byte)0x2D ,(byte)0x72 ,(byte)0x65 ,(byte)0x76 ,(byte)0x69 ,(byte)0x73 ,(byte)0x69 ,(byte)0x6F ,(byte)0x6E ,(byte)0x20 ,(byte)0x32 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x20 ,(byte)0x09 ,(byte)0x23 ,(byte)0x20 ,(byte)0x30 ,(byte)0x2D ,(byte)0x31 ,(byte)0x3A ,(byte)0x20 ,(byte)0x52 ,(byte)0x65 ,(byte)0x76 ,(byte)0x30 ,(byte)0x31 ,(byte)0x3B ,(byte)0x20 ,(byte)0x32 ,(byte)0x3A ,(byte)0x20 ,(byte)0x52 ,(byte)0x65 ,(byte)0x76 ,(byte)0x30 ,(byte)0x32 ,(byte)0x3B ,(byte)0x20 ,(byte)0x33 ,(byte)0x3A ,(byte)0x20 ,(byte)0x52 ,(byte)0x65 ,(byte)0x76 ,(byte)0x30 ,(byte)0x32 ,(byte)0x20 ,(byte)0x77 ,(byte)0x69};

		assertEquals((byte) 0xFD, FlashWorker.getCheckSum(bytes));
	}
}