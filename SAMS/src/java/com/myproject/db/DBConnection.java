package com.myproject.db;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    // Method to get database connection
    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // Load MySQL driver
            Connection con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/samsdb", "root", "Swadhu177!");
            System.out.println("✅ Database connected successfully!");
            return con;
        } catch (Exception e) {=
            e.printStackTrace(); // Print exact error
            return null;
        }
    }

    // Optional: Test connection
    public static void main(String[] args) {
        try (Connection con = getConnection()) {
            if (con != null) System.out.println("DB Connection works!");
            else System.out.println("DB Connection failed!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

