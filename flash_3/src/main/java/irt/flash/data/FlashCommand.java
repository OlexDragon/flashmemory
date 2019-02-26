package irt.flash.data;

public enum FlashCommand {
	EMPTY			(new byte[] { }),
	/** 0x7F */
	CONNECT			(new byte[] { 0x7F }),
	/** byte[] { 0x00, 0xFF} */
	GET				(new byte[] { 0x00, (byte) 0xFF }),
	/** byte[] { 0x02, 0xFD} */
	GET_ID			(new byte[] { 0x02, (byte) 0xFD }),
	/** byte[] { 0x11, 0xEE} */
	READ_MEMORY		(new byte[] { 0x11, (byte) 0xEE }),
	/** byte[] { 0x31, 0xCE} */
	WRITE_MEMORY	(new byte[] { 0x31, (byte) 0xCE }),
	/** byte[] { 0x43, 0xBC} */
	ERASE			(new byte[] { 0x43, (byte) 0xBC }),
	/** byte[] { 0x44, 0xBB} */
	EXTENDED_ERASE	(new byte[] { 0x44, (byte) 0xBB });

	private byte[] command;

	private FlashCommand(byte[] command) {
		this.command = command;
	}

	public byte[] toBytes() {
		return command;
	}

}
