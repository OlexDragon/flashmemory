package irt.flash.data;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ToHexTest {

	@Test
	public void testParseToByte() {
		assertEquals((byte)0, ToHex.parseToByte(null));
		assertEquals((byte)0, ToHex.parseToByte(""));
		assertEquals((byte)11, ToHex.parseToByte("b"));
		assertEquals((byte)27, ToHex.parseToByte("1b"));
	}

	@Test
	public void testByteToHex(){
		assertEquals("00", ToHex.byteToHex((byte) 0));
		assertEquals("05", ToHex.byteToHex((byte) 5));
		assertEquals("0A", ToHex.byteToHex((byte) 10));
		assertEquals("14", ToHex.byteToHex((byte) 20));
		assertEquals("FF", ToHex.byteToHex((byte) 255));
	}
}
