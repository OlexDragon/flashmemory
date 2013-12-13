package irt.flash.data.connection.dao;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;

public class DatabaseTest {

	private Database database;

	@Before
	public void init() throws IOException{
		database = Database.getInstance();
	}

	@Test
	public void setSerialNumber() throws ClassNotFoundException, SQLException, IOException{
	}
}
