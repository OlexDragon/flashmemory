package irt.flash.data;

import java.util.Arrays;
import java.util.Optional;

import javax.xml.bind.DatatypeConverter;

public enum FlashAnswer {
	UNKNOWN	((byte) -1),
	NULL	((byte) 0),
	ACK		((byte) 0x79),
	NACK	((byte) 0x1F);

	private final byte answer;

	private FlashAnswer(byte answer) {
		this.answer = answer;
	}

	public byte getAnswer() {
		return answer;
	}

	public static Optional<FlashAnswer> valueOf(byte key){
		final Optional<FlashAnswer> findAny = Arrays
				.stream(values())
				.filter(a->a.answer==key)
				.findAny();
		return findAny;
	}

	@Override
	public String toString() {
		return name()+" (0x"+ DatatypeConverter.printHexBinary(new byte[] {answer}) + ")";
	}

	public boolean match(Byte answer) {
		return Optional.ofNullable(answer).filter(b->b==answer).isPresent();
	}
}
