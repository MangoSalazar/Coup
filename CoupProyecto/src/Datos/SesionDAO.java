package Datos;

import Dominio.Sesion;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SesionDAO {

    /**
     * Registra un nuevo cliente en la tabla 'clientes'.
     */
    public boolean registrarCliente(Sesion sesion) {
        // SQL ajustado a tus columnas
        String sql = "INSERT INTO clientes(nombre_cliente, contra, ranking) VALUES(?,?,?)";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, sesion.getNombre());
            // Encriptamos la "contra" antes de guardar
            pstmt.setString(2,sesion.getContra()); 
            pstmt.setInt(3, 0); // Ranking inicial 0

            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            if(e.getMessage().contains("UNIQUE")) {
                System.out.println("Error: El nombre de cliente ya existe.");
            } else {
                System.out.println("Error al registrar: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Valida login buscando por 'nombre_cliente' y comparando 'contra'.
     */
    public Sesion validarLogin(Sesion sesion) {
        String sql = "SELECT * FROM clientes WHERE nombre_cliente = ?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, sesion.getNombre());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Recuperamos los datos usando los nombres EXACTOS de tu tabla
                String contraGuardada = rs.getString("contra");
                if (contraGuardada.equals(sesion.getContra())) {
                    return new Sesion(
                        rs.getInt("id"),
                        rs.getString("nombre_cliente"),
                        rs.getString("contra"),
                        rs.getInt("ranking")
                    );
                }
            }
        } catch (SQLException e) {
            System.out.println("Error en login: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Actualiza el ranking (partidas ganadas) usando el ID.
     */
    public void sumarPuntoRanking(Sesion sesion) {
        String sql = "UPDATE clientes SET ranking = ranking + 1 WHERE nombre_cliente = ?";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sesion.getNombre());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error al actualizar ranking: " + e.getMessage());
        }
    }
}