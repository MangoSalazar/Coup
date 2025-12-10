
package Datos;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexionDB {
private static final String URL = "jdbc:sqlite:bd/usuarios.db";
    public static Connection conectar() {
        Connection conn = null;
        try {
            File directorio = new File("db"); // Apunta a la carpeta 'db'
            if (!directorio.exists()) {
                if (directorio.mkdirs()) { // Crea la carpeta si no existe
                    System.out.println("Carpeta 'db' creada exitosamente.");
                }
            }
            conn = DriverManager.getConnection(URL);
            crearTablaSiNoExiste(conn);
            System.out.println("Base de datos creada");
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
