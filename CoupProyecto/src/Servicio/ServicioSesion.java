package Servicio;

import Dominio.Sesion;
import Servidor.ServidorMulti;
import Servidor.UnCliente;
import Datos.SesionDAO; // IMPORTANTE: Importar tu DAO
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ServicioSesion {

    private List<Sesion> sesionesActivas = new ArrayList();
    private UnCliente cliente;
    private boolean sesionIniciada = false;
    private Sesion sesionsita;
    private SesionDAO sesionDAO = new SesionDAO();      // Instanciamos el DAO para conectar a la BD
    

    public ServicioSesion(UnCliente cliente) {
        this.cliente = cliente;
    }

    public boolean isSesionIniciada() {
        return sesionIniciada;
    }

    public String pedirCredenciales() throws IOException {
        cliente.salida().writeUTF("Ingresa tu usuario: ");
        String usuario = cliente.entrada().readUTF();
        cliente.salida().writeUTF("Ingresa tu contra: "); // Pequeña corrección visual
        String contra = cliente.entrada().readUTF();
        
        // Unimos con espacio para mantener tu estructura actual
        String cadenita = usuario + " " + contra;
        
        // Primero validamos formato (longitud, caracteres raros)
        if (evaluarCreedenciales(cadenita)) {
            return cadenita;
        }
        return null;
    }

    public boolean evaluarCreedenciales(String credenciales) {
        if (credenciales.split(" ").length == 2) {
            String nombre = credenciales.split(" ")[0];
            String contra = credenciales.split(" ")[1];
            // Validaciones básicas de formato
            return nombre.length() >= 3 && contra.length() >= 3 && nombre.matches("^[a-zA-Z0-9_]+$") && contra.matches("^[a-zA-Z0-9_]+$");
        }
        return false;
    }

    public boolean yaLogueado(String nombreUsuario) throws IOException {
        // Recorremos los clientes conectados al servidor
        for(UnCliente clientesito : ServidorMulti.clientes.values()){
            // Comparamos el ID del cliente con el nombre de usuario
            if (clientesito.getId().equals(nombreUsuario)) {
                return true;
            }
        }
        return false;
    }

    public void iniciarSesion() throws IOException {
        while (!sesionIniciada) {
            String credenciales = pedirCredenciales();
            
            // 1. Si el formato es inválido (muy corto o caracteres raros)
            if (credenciales == null) {
                cliente.salida().writeUTF("Formato invalido (min 3 letras, sin espacios)");
                continue;
            }

            String nombre = credenciales.split(" ")[0];
            String contra = credenciales.split(" ")[1];

            // 2. Verificamos si ya hay alguien conectado con ese nombre en el Servidor
            if (yaLogueado(nombre)) {
                cliente.salida().writeUTF("El usuario ya esta conectado en otra terminal.");
                continue;
            }

            // 3. INTEGRACIÓN CON BASE DE DATOS
            // Intentamos hacer Login
            Sesion sesion = sesionDAO.validarLogin(nombre, contra);

            if (sesion != null) {
                // --- CASO A: LOGIN EXITOSO ---
                confirmarInicioSesion(nombre, contra, "Inicio de sesion exitoso. Ranking: " + sesion.getRanking());
            
            } else {
                // --- CASO B: NO EXISTE O CONTRASEÑA MAL ---
                // Intentamos registrar al usuario si no existe
                Sesion nuevaSesion = new Sesion(nombre, contra);
                if (sesionDAO.registrarCliente(nuevaSesion)) {
                    // Registro exitoso, pasamos a iniciar sesión
                    confirmarInicioSesion(nombre, contra, "Usuario nuevo registrado exitosamente.");
                } else {
                    // Si falla el registro y falló el login, es que el usuario existe pero la contraseña está mal
                    cliente.salida().writeUTF("Error: Contraseña incorrecta o usuario no disponible.");
                }
            }
        }
    }

    // Método auxiliar para no repetir código al loguear
    private void confirmarInicioSesion(String nombre, String contra, String mensaje) throws IOException {
        sesionsita = new Sesion(nombre, contra);
        sesionesActivas.add(sesionsita);
        
        cliente.salida().writeUTF(mensaje);
        
        // Actualizamos el ID en el servidor para que aparezca con su nombre
        ServidorMulti.cambiarIdCliente(cliente.getId(), nombre);
        sesionIniciada = true;
    }

    public void cerrarSesion() {
        if (sesionIniciada) {
            ServidorMulti.eliminarIdCliente(cliente.getId());
            sesionesActivas.remove(sesionsita);
            sesionIniciada = false;
        }
    }
}