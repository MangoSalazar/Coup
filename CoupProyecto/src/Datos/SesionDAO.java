package Datos;

import Dominio.Sesion;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SesionDAO {

    private static final String URL = "jdbc:sqlite:coup_usuarios.db";

    public SesionDAO() {
        crearTablaSiNoExiste();
    }

    private Connection conectar() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL);
        } catch (SQLException e) {
            System.out.println("Error de conexión a BD: " + e.getMessage());
        }
        return conn;
    }

    private void crearTablaSiNoExiste() {
        String sql = "CREATE TABLE IF NOT EXISTS usuarios (\n"
                + " id integer PRIMARY KEY AUTOINCREMENT,\n"
                + " nombre text NOT NULL UNIQUE,\n"
                + " contra text NOT NULL\n"
                + ");";

        try (Connection conn = conectar();
             Statement stmt = conn.createStatement()) {
            if (conn != null) {
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            System.out.println("Error creando tabla: " + e.getMessage());
        }
    }

    // Verifica si las credenciales son correctas
    public boolean validarUsuario(String nombre, String contra) {
        String sql = "SELECT id FROM usuarios WHERE nombre = ? AND contra = ?";
        try (Connection conn = conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            pstmt.setString(2, contra);
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // Retorna true si encontró el usuario
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    // Verifica si el nombre de usuario ya existe (para no registrar duplicados)
    public boolean existeUsuario(String nombre) {
        String sql = "SELECT id FROM usuarios WHERE nombre = ?";
        try (Connection conn = conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    // Registra un nuevo usuario en la BD
    public boolean registrarUsuario(String nombre, String contra) {
        String sql = "INSERT INTO usuarios(nombre, contra) VALUES(?,?)";
        try (Connection conn = conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            pstmt.setString(2, contra);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error registrando: " + e.getMessage());
            return false;
        }
    }
}