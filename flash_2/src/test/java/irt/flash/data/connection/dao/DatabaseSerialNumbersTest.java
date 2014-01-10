package irt.flash.data.connection.dao;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

public class DatabaseSerialNumbersTest {

	private DatabaseSerialNumbers databaseSerialNumbers;

	@Before
	public void init() throws IOException{
		Properties p = new Properties();
		p.load(Database.class.getResourceAsStream("sql.properties"));
		databaseSerialNumbers = new DatabaseSerialNumbers(p);
	}

	@Test
	public void test() {
		String actual = new DecimalFormat("000").format(1);
		assertEquals("001", actual);
	}

	@Test
	public void getNewSerialNumber1() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<Integer> l = new ArrayList<>();

		Method method = DatabaseSerialNumbers.class.getDeclaredMethod("getSerialNumber", String.class, List.class);
		method.setAccessible(true);

		String output = (String) method.invoke(databaseSerialNumbers, "1352", l);
		assertEquals("1352001", output);
	}

	@Test
	public void getNewSerialNumber2() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<Integer> l = new ArrayList<>();
		l.add(1);
		l.add(2);
		l.add(4);

		Method method = DatabaseSerialNumbers.class.getDeclaredMethod("getSerialNumber", String.class, List.class);
		method.setAccessible(true);

		String output = (String) method.invoke(databaseSerialNumbers, "1352", l);
		assertEquals("1352003", output);
	}

	@Test
	public void getSerialNumbersStartWith() throws ClassNotFoundException, SQLException, IOException, InterruptedException {
		assertEquals("IRT-1343002", databaseSerialNumbers.getNextSerialNumber("1343"));
		assertEquals("IRT-1351001", databaseSerialNumbers.getNextSerialNumber("1351"));
	}
}
