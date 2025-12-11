package Datos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexion {

    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/CoupDB?useSSL=false&useTimezone=true&serverTimezone=UTC&allowPublicKeyRetrieval=true";    
    private static final String JDBC_USERNAME = "root";
    private static final String JDBC_PASSWORD = "admin";
    private static Connection conexion;
    
    public static Connection getConnection() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            conexion = DriverManager.getConnection(JDBC_URL, JDBC_USERNAME, JDBC_PASSWORD);
        }
        return conexion;
    }
    public static void closeConnection() throws SQLException{
        if (conexion != null && !conexion.isClosed()) {
            conexion.close();
        }
    }
    
}

