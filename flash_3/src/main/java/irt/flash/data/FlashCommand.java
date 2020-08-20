package irt.flash.data;

public enum FlashCommand {
	EMPTY			(new byte[] { }),
	/** DEC:{127}; HEX:{0x7F} */
	CONNECT			(new byte[] { 0x7F }),
	/** DEC:{0, 255}; HEX: {0x00, 0xFF} */
	GET				(new byte[] { 0x00, (byte) 0xFF }),
	/** DEC: {2,253}; HEX: { 0x02, 0xFD} */
	GET_ID			(new byte[] { 0x02, (byte) 0xFD }),
	/** DEC: {17,238}; HEX: { 0x11, 0xEE} */
	READ_MEMORY		(new byte[] { 0x11, (byte) 0xEE }),
	/** DEC: {49,206}; HEX: { 0x31, 0xCE} */
	WRITE_MEMORY	(new byte[] { 0x31, (byte) 0xCE }),
	/** DEC: {67,188}; HEX: { 0x43, 0xBC} */
	ERASE			(new byte[] { 0x43, (byte) 0xBC }),
	/** DEC: {68,187}; { 0x44, 0xBB} */
	EXTENDED_ERASE	(new byte[] { 0x44, (byte) 0xBB });

	private byte[] command;

	private FlashCommand(byte[] command) {
		this.command = command;
	}

	public byte[] toBytes() {
		return command;
	}

}
