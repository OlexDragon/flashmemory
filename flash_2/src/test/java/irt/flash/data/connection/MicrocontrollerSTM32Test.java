package irt.flash.data.connection;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.junit.Before;
import org.junit.Test;

public class MicrocontrollerSTM32Test {

	private MicrocontrollerSTM32 stm32;

	@Before
	public void init(){
		stm32 = MicrocontrollerSTM32.getInstance();
	}

	@Test
	public void test() {
		BigDecimal divide = new BigDecimal(2149).divide(new BigDecimal(10000), 2, RoundingMode.HALF_EVEN);
		System.out.println(divide);
	}

	@Test
	public void test2() {
		stm32.notifyObservers();
	}
}
