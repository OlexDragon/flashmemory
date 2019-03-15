package irt.flash.exception;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ExceptionWrapper {

	public static <T, R, E extends Exception> Function<T, R> catchFunctionException(FunctionThrowing<T, R, E> fe) {

		return arg -> {
 
			try {

            	return fe.apply(arg);

            } catch (Exception e) {

            	throw new WrapperException(e);
 
            }
        };
	}

	public static <T, E extends Exception> Consumer<T> catchConsumerException(ConsumerThrowing<T, E> ce) {

		return arg -> {
 
			try {

            	ce.accept(arg);

            } catch (Exception e) {

            	throw new WrapperException(e);
 
            }
        };
	}

	public static <T, E extends Exception> Supplier<T> catchSupplierException(SupplierThrowing<T, E> se) {

		return () -> {

            	try {
					return se.get();

            	} catch (Exception e) {
					throw new WrapperException(e);
				}
        };
	}

	public static <E extends Exception> Runnable catchRunnableException(RannableThrowing<E> re) {

		return () -> {

            	try {

            		re.run();

            	} catch (Exception e) {
					throw new WrapperException(e);
				}
        };
	}
}
