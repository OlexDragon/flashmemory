package irt.flash.helpers;

import java.util.Optional;
import java.util.function.Consumer;

public class OptionalIfElse<T> {

	private Optional<T> optional;

	private OptionalIfElse(Optional<T> optional) {
		this.optional = optional;
	}

	public static <T> OptionalIfElse<T> of(Optional<T> optional) {
		return  new OptionalIfElse<T>(optional);
	}

	public OptionalIfElse<T> ifPresent(Consumer<T> c) {
        optional.ifPresent(c);
        return this;
    }

    public OptionalIfElse<T> ifNotPresent(Runnable r) {
        if (!optional.isPresent())
            r.run();
        return this;
    }
}
