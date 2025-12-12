package Servicio;

import Datos.SesionDAO;
import Dominio.Sesion;
import Servidor.ServidorMulti;
import Servidor.UnCliente;
import java.io.IOException;

public class ServicioSesion {

    private UnCliente cliente;
    private boolean sesionIniciada = false;
    private Sesion sesionsita; // Mantenemos el objeto sesión en memoria para el juego
    private SesionDAO sesionDAO;

    public ServicioSesion(UnCliente cliente) {
        this.cliente = cliente;
        this.sesionDAO = new SesionDAO(); // Esto inicializa la BD
    }

    public boolean isSesionIniciada() {
        return sesionIniciada;
    }

    // Pide datos y valida formato básico (sin espacios, longitud mínima)
    public String pedirCredenciales() throws IOException {
        cliente.salida().writeUTF("--- INICIO DE SESIÓN ---");
        cliente.salida().writeUTF("Ingresa tu usuario: ");
        String usuario = cliente.entrada().readUTF().trim();

        cliente.salida().writeUTF("Ingresa tu contraseña: ");
        String contra = cliente.entrada().readUTF().trim();

        // Validaciones de formato
        if (usuario.length() < 3) {
            cliente.salida().writeUTF("Error: El usuario debe tener al menos 3 caracteres.");
            return null;
        }
        // Solo letras, números y guiones bajos, PERO no puede empezar con número
        if (!usuario.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
            cliente.salida().writeUTF("Error: El usuario debe iniciar con una letra y solo puede contener letras, números o _.");
            return null;
        }

        return usuario + " " + contra;
    }

    // Verifica si el usuario ya está conectado en el servidor (en memoria)
    public boolean yaConectadoOnline(String nombre) {
        for (UnCliente c : ServidorMulti.clientes.values()) {
            // Ignoramos al cliente actual (que tiene ID numérico temporal)
            if (c.equals(cliente)) {
                continue;
            }

            // Verificamos si alguien más ya tiene ese ID (ignora mayúsculas)
            if (c.getId().equalsIgnoreCase(nombre)) {
                return true;
            }
        }
        return false;
    }

    public void cerrarSesion() throws IOException {
        sesionIniciada = false;
        sesionsita = null;
        cliente.salida().writeUTF("sesion: "+sesionIniciada);
        ServidorMulti.cambiarIdCliente(cliente.getId(), "invitado");
    }

    public boolean iniciarSesion() throws IOException {
        while (!sesionIniciada) {
            String credenciales = pedirCredenciales();

            if (credenciales == null) {
                continue; // Hubo error de formato, pedimos de nuevo
            }

            String[] partes = credenciales.split(" ");
            String usuario = partes[0];
            String contra = partes[1];

            // 1. Verificar si ya está jugando
            if (yaConectadoOnline(usuario)) {
                cliente.salida().writeUTF("Error: Ese usuario ya está conectado en el servidor.");
                continue;
            }

            // 2. Lógica de Base de Datos
            if (sesionDAO.existeUsuario(usuario)) {
                // El usuario EXISTE -> Intentar Login
                if (sesionDAO.validarUsuario(usuario, contra)) {
                    loguearExitoso(usuario, contra, "¡Bienvenido de vuelta, " + usuario + "!");
                    return true;
                }
                cliente.salida().writeUTF("Error: Contraseña incorrecta.");
                continue;
            }
            // El usuario NO EXISTE -> Registrar automáticamente
            if (sesionDAO.registrarUsuario(usuario, contra)) {
                loguearExitoso(usuario, contra, "Cuenta creada exitosamente. Bienvenido " + usuario + ".");
                return true;
            }
            cliente.salida().writeUTF("Error al crear la cuenta. Intenta otro nombre.");

        }
        return false;
    }

    private void loguearExitoso(String usuario, String contra, String mensaje) throws IOException {
        sesionsita = new Sesion(usuario, contra);
        ServidorMulti.cambiarIdCliente(cliente.getId(), usuario);
        cliente.salida().writeUTF(mensaje);
        sesionIniciada = true;
    }
}
