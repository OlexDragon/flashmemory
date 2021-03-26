package irt.flash.helpers;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class OptionalIfElse<T> {

	private Optional<T> optional;

	private OptionalIfElse(Optional<T> optional) {
		this.optional = optional;
	}

	public OptionalIfElse<T> ifPresentOrNot(Consumer<T> ifPresent, Runnable ifNotPresent) {

		if (optional.isPresent())
            return ifPresent(ifPresent);

		else
        	return ifNotPresent(ifNotPresent);
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

    public OptionalIfElse<T> ifThenContinue(Predicate<? super T> predicate, Consumer<T> consumer) {
            optional.filter(predicate).ifPresent(consumer);
        return this;
    }

    public OptionalIfElse<T> ifThenEnd(Predicate<? super T> predicate, Consumer<T> consumer) {
 
    	if(optional.isPresent()) {
    		final Optional<T> filter = optional.filter(predicate);
 
    		if(filter.isPresent())
    			optional = Optional.empty();

    		filter.ifPresent(consumer);
    	}
    	return this;
    }

    public OptionalIfElse<T> ifThenEnd(Predicate<? super T> predicate, Runnable target) {
 
    	if(optional.isPresent()) {

    		final Optional<T> filter = optional.filter(predicate);
 
    		if(filter.isPresent()) {

    			optional = Optional.empty();
    			ThreadWorker.runThread(target);
    		}
    	}
    	return this;
    }

	public static <T> OptionalIfElse<T> of(T value) {
		return of(Optional.ofNullable(value));
	}

	public static <T> OptionalIfElse<T> of(Optional<T> optional) {
		return  new OptionalIfElse<T>(optional);
	}
}
