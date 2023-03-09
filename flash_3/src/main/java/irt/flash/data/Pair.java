package irt.flash.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor @Getter @ToString
public class Pair<T1, T2> {

	private final T1 first;
	private final T2 second;
}
