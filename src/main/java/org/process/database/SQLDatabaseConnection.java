package org.process.database;

import java.sql.*;

public class SQLDatabaseConnection {

    public static void createConnection(){

        String connectionUrl = "jdbc:sqlserver://localhost:1433;" +
                "databaseName=AdventureWorks2022;" +
                "user=sa;" +
                "password=cc0395...;" +
                "loginTimeout=30;";

        try (Connection connection = DriverManager.getConnection(connectionUrl);
             Statement statement = connection.createStatement()) {

            String selectSql = "SELECT TOP 10 Title, FirstName, LastName FROM SalesLT.Customer";
            ResultSet resultSet = statement.executeQuery(selectSql);

            while (resultSet.next()) {
                System.out.println(resultSet.getString(2) + " " + resultSet.getString(3));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
}
