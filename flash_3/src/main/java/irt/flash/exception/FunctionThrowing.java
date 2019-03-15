package irt.flash.exception;

@FunctionalInterface
public interface FunctionThrowing<T, R, E extends Exception> {

	R apply(T t) throws E;

}
