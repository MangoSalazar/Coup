package Datos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Conexion {

    private static final String DB_NAME = "CoupDB";

    // Conexión sin base seleccionada
    private static final String JDBC_URL_BASE =
            "jdbc:mysql://localhost:3306/?useSSL=false&useTimezone=true&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    // Conexión ya con la base seleccionada
    private static final String JDBC_URL_DB =
            "jdbc:mysql://localhost:3306/" + DB_NAME + "?useSSL=false&useTimezone=true&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    private static final String JDBC_USERNAME = "root";
    private static final String JDBC_PASSWORD = "admin";

    private static Connection conexion;

    public static Connection getConnection() throws SQLException {
        if (conexion == null || conexion.isClosed()) {

            // 1. Crear BD si no existe
            crearBaseDeDatos();

            // 2. Crear tablas si no existen
            crearTablas();

            // 3. Conectarse ya a la BD lista
            conexion = DriverManager.getConnection(JDBC_URL_DB, JDBC_USERNAME, JDBC_PASSWORD);
        }
        return conexion;
    }

    private static void crearBaseDeDatos() throws SQLException {
        try (Connection conn = DriverManager.getConnection(JDBC_URL_BASE, JDBC_USERNAME, JDBC_PASSWORD);
             Statement st = conn.createStatement()) {

            st.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
        }
    }

    private static void crearTablas() throws SQLException {
        try (Connection conn = DriverManager.getConnection(JDBC_URL_DB, JDBC_USERNAME, JDBC_PASSWORD);
             Statement st = conn.createStatement()) {

            String tablaCliente = """
                CREATE TABLE IF NOT EXISTS cliente (
                    nombre VARCHAR(45)PRIMARY KEY,
                    contra VARCHAR(45) NOT NULL
                )
            """;

            st.executeUpdate(tablaCliente);
        }
    }

    public static void closeConnection() throws SQLException {
        if (conexion != null && !conexion.isClosed()) {
            conexion.close();
        }
    }
}