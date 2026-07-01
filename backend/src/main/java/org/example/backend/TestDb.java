package org.example.backend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestDb {
    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/postgres", "postgres", "123456");
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE orca_db;");
            System.out.println("Database created successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
