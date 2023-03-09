package irt.flash.data;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public enum UnitAddress {
	PROGRAM		("PROGRAM"			, 0x08000000),
	CONVERTER	("CONVERTER"		, 0x080C0000),
	BIAS		("BIAS BOARD"		, 0x080E0000),
	HP_BIAS		("HP BIAS"			, 0x081E0000),
	SHELF		("SHELF CONTROLLER"	, 0x080E0000),
	REF_BOARD	("REFERENCE BOARD"	, 0x08060000),
	ORPC		("ORPC"				, 0x081C0000);

	private final String text;
	private final int addr;

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
