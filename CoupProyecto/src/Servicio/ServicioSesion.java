package Servicio;

import Datos.SesionDAO;
import Dominio.Sesion;
import Servidor.ServidorMulti;
import Servidor.UnCliente;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ServicioSesion {

    private List<Sesion> sesionesActivas = new ArrayList();
    private UnCliente cliente;
    private boolean sesionIniciada = false;
    private Sesion sesionsita;

    public ServicioSesion(UnCliente cliente) {
        this.cliente = cliente;
    }

    public boolean isSesionIniciada() {
        return sesionIniciada;
    }

    public String pedirCredenciales() throws IOException {
        cliente.salida().writeUTF("Ingresa tu usuario: ");
        String usuario = cliente.entrada().readUTF();
        cliente.salida().writeUTF("Ingresa tu contra");
        String contra = cliente.entrada().readUTF();
        String cadenita = usuario + " " + contra;
        if (evaluarCreedenciales(cadenita)) {
            return cadenita;
        }
        return null;
    }

    public boolean evaluarCreedenciales(String credenciales) {
        if (credenciales.split(" ").length == 2) {
            String nombre = credenciales.split(" ")[0];
            String contra = credenciales.split(" ")[1];
            return nombre.length() >= 3 && contra.length() >= 5 && nombre.matches("^[a-zA-Z0-9_]+$") && contra.matches("^[a-zA-Z0-9_]+$");
        }
        return false;
    }

    public boolean yaLogueado(Sesion sesionsita) throws IOException {
        for(UnCliente clientesito : ServidorMulti.clientes.values()){
            if (clientesito.getId().equals(sesionsita.getNombre())) {
                return true;
            }
        }
        return false;
    }

    public void iniciarSesion() throws IOException {
        while (!sesionIniciada) {
            String credenciales = pedirCredenciales();
            if (credenciales == null) {
                cliente.salida().writeUTF("datos invalidos");
                continue;
            }
            
            sesionsita = new Sesion(credenciales.split(" ")[0], credenciales.split(" ")[1]);
            if (yaLogueado(sesionsita)) {
                cliente.salida().writeUTF("usuario ya logueado");
                return;
            }
            if (new SesionDAO().iniciarSesion(sesionsita)) {
                sesionesActivas.add(sesionsita);
                cliente.salida().writeUTF("inicio de sesion exitoso");
                ServidorMulti.cambiarIdCliente(cliente.getId(), credenciales.split(" ")[0]);
                sesionIniciada = true;
                return;
            }cliente.salida().writeUTF("usuario y/o contra incorrectas");
            sesionsita=null;
        }
    }

    public void cerrarSesion() {
        ServidorMulti.eliminarIdCliente(cliente.getId());
        sesionesActivas.remove(sesionsita);
        sesionIniciada = false;
    }

}
