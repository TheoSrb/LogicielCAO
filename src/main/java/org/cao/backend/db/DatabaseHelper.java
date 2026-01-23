package org.cao.backend.db;

import org.cao.backend.BackendLogic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseHelper {

    private static final String URL = BackendLogic.readProperty("db.url")
            + ";sendStringParametersAsUnicode=false"
            + ";rewriteBatchedStatements=true";
    private static final String USER = BackendLogic.readProperty("db.user");
    private static final String PASSWORD = BackendLogic.readProperty("db.password");

    static void main() {

    }

    private static void updateDatabase() {
        startConnectionWithDatabase();

        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD); Statement statement = con.createStatement()) {



        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void startConnectionWithDatabase() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

}
