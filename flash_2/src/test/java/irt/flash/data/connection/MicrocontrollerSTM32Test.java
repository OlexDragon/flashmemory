package irt.flash.data.connection;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
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
		BigDecimal actual = new BigDecimal(2149).divide(new BigDecimal(10000), 2, RoundingMode.HALF_EVEN);
		System.out.println(actual);
		assertEquals(new BigDecimal("0.21"), actual);
	}

	@Test
	public void test2() {
		stm32.notifyObservers();
	}
}
