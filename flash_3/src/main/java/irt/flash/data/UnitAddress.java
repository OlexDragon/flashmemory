package irt.flash.data;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public enum UnitAddress {
	PROGRAM		("PROGRAM"			, 0x08000000),
	CONVERTER	("CONVERTER"		, 0x080C0000),
	BIAS		("BIAS BOARD"		, 0x080E0000),
	HP_BIAS		("HP BIAS"			, 0x081E0000),
	SHELF		("Shelf Controller"	, 0x080E0000),
	ORPC		("ORPC"				, 0x081C0000);

	private String text;
	private int addr;

	private UnitAddress(String name, int addr) {
		this.text = name;
		this.addr = addr;
	}

	public int getAddr() {
		return addr;
	}

	public byte[] getAddrAsBytes() {
		return intToBytes(addr);
	}

	public static byte[] intToBytes(int addr) {
		final ByteBuffer bb = ByteBuffer.allocate(4).putInt(addr);
		final ByteBuffer result = ByteBuffer.allocate(4).order( ByteOrder.BIG_ENDIAN);
		for(byte b: bb.array())
			result.put(b);
		return result.array();
	}

	@Override
	public String toString() {
		return text;
	}
}
