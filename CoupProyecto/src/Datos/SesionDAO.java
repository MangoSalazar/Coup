package Datos;

import Dominio.Sesion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SesionDAO {

    private static final String SQL_SELECT = "SELECT nombre, contra FROM cliente";
    private static final String SQL_INSERT = "INSERT INTO cliente(nombre, contra) VALUES (?, ?)";
    private static final String SQL_UPDATE = "UPDATE cliente SET contra = ? WHERE nombre = ?";
    private static final String SQL_DELETE = "DELETE FROM cliente WHERE nombre = ?";
    List<Sesion> sesiones;

    public boolean registrarUsuario(Sesion x) {
        if (existeUsuario(x.getNombre()) == null) {
            insertar(x);
            return true;
        }
        return false;

    }

    public boolean iniciarSesion(Sesion x) {
        Sesion buscar = existeUsuario(x.getNombre());
        if (buscar != null && x.getContra().equals(buscar.getContra())) {
            return true;
        }
        return false;
    }
  
    public Sesion existeUsuario(String nombre) {
        sesiones = listar();
        for (Sesion sesion : sesiones) {
            if (nombre.equals(sesion.getNombre())) {
                return sesion;
            }
        }
        return null;
    }

    // Obtener todos los usuarios
    public List<Sesion> listar() {
        sesiones = new ArrayList<>();
        try (Connection conn = Conexion.getConnection(); PreparedStatement stmt = conn.prepareStatement(SQL_SELECT); ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String nombre = rs.getString("nombre");
                String contra = rs.getString("contra");
                sesiones.add(new Sesion(nombre, contra));
            }

        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }
        return sesiones;
    }

    // Insertar nuevo usuario
    public int insertar(Sesion sesion) {
        int registros = 0;
        try (Connection conn = Conexion.getConnection(); PreparedStatement stmt = conn.prepareStatement(SQL_INSERT)) {

            stmt.setString(1, sesion.getNombre());
            stmt.setString(2, sesion.getContra());
            registros = stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }
        return registros;
    }

    // Actualizar contraseña
    public int actualizar(Sesion sesion) {
        int registros = 0;
        try (Connection conn = Conexion.getConnection(); PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {

            stmt.setString(1, sesion.getContra());
            stmt.setString(2, sesion.getNombre());
            registros = stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }
        return registros;
    }

    // Eliminar usuario por username
    public int eliminar(Sesion sesion) {
        int registros = 0;
        try (Connection conn = Conexion.getConnection(); PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {

            stmt.setString(1, sesion.getNombre());
            registros = stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }
        return registros;
    }

}
