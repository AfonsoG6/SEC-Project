package pt.tecnico.sec.bftb.server;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.DriverManager;

public class SQLiteJDBC {
	Connection c = null;
	Statement s = null;
	public SQLiteJDBC() {
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:test.db");
		} catch (Exception e) {
			// TODO handle exception
		}
		System.out.println("Opened database successfully");
	}
}
