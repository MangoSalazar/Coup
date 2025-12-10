
package Datos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexionDB {
private static final String URL = "jdbc:sqlite:db/usuarios.db";
    public static Connection conectar() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL);
            crearTablaSiNoExiste(conn);
        } catch (SQLException e) {
            System.out.println("Error de conexión: " + e.getMessage());
        }
        return conn;
    }

    private static void crearTablaSiNoExiste(Connection conn) throws SQLException {
        // La tabla contiene SOLO: nombre (PK), password y partidas_ganadas
        String sql = "CREATE TABLE IF NOT EXISTS clientes ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, " 
                + "nombre_cliente TEXT NOT NULL UNIQUE, "
                + "contra TEXT NOT NULL, "
                + "ranking INTEGER DEFAULT 0"
                + ");";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
}
